package com.github.yktakaha4.watsonmusic.service;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.spell.StringDistance;
import org.glassfish.jersey.client.ClientProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.mapper.CacheMapper;
import com.github.yktakaha4.watsonmusic.model.Cache;
import com.github.yktakaha4.watsonmusic.model.Lyrics;
import com.github.yktakaha4.watsonmusic.model.LyricsPage;
import com.github.yktakaha4.watsonmusic.model.LyricsPageLink;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.util.NormalizedStringDistance;
import com.github.yktakaha4.watsonmusic.util.RequestInterval;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class LyricsService {
  private static final double STRING_DISTANCE_THRESHOLD = 0.75;
  private final WebProperties webProperties;
  private final CacheMapper cacheMapper;
  private final RequestInterval requestInterval;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public LyricsService(WebProperties webProperties, CacheMapper cacheMapper, RequestInterval requestInterval) {
    this.webProperties = webProperties;
    this.cacheMapper = cacheMapper;
    this.requestInterval = requestInterval;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<LyricsPage> getLyricsPage(String artistName) {
    Optional<Cache> maybeArtistSearchCache = Optional
        .ofNullable(cacheMapper.selectBySourceAndRequestKey(Source.LYRICS_ARTIST_SEARCH, artistName));

    Cache artistSearchCache;
    if (maybeArtistSearchCache.isPresent()) {
      artistSearchCache = maybeArtistSearchCache.get();
    } else {
      cacheMapper.insert(createLyricsArtistSearch(artistName));
      logger.info("cached lyrics artist search: " + artistName);
      artistSearchCache = cacheMapper.selectBySourceAndRequestKey(Source.LYRICS_ARTIST_SEARCH, artistName);
    }

    Optional<String> maybeArtistPagePath = getArtistLyricsPagePath(artistSearchCache);
    if (!maybeArtistPagePath.isPresent()) {
      return Optional.empty();
    }
    String artistPagePath = maybeArtistPagePath.get();

    Optional<Cache> maybeLyricsNamesByArtist = Optional
        .ofNullable(cacheMapper.selectBySourceAndRequestKey(Source.LYRICS_NAMES_BY_ARTIST, artistPagePath));

    Cache lyricsNamesByArtistCache;
    if (maybeLyricsNamesByArtist.isPresent()) {
      lyricsNamesByArtistCache = maybeLyricsNamesByArtist.get();
    } else {
      cacheMapper.insert(createLyricsNamesByArtistCache(artistPagePath));
      logger.info("cached lyrics names by artist: " + artistName);
      lyricsNamesByArtistCache = cacheMapper.selectBySourceAndRequestKey(Source.LYRICS_NAMES_BY_ARTIST, artistPagePath);
    }

    Optional<List<LyricsPageLink>> maybeLyricsPageLinks = createLyricsPageLinks(lyricsNamesByArtistCache);
    if (maybeLyricsPageLinks.isPresent()) {
      LyricsPage lyricsPage = new LyricsPage();
      lyricsPage.setArtistName(artistName);
      lyricsPage.setPath(maybeArtistPagePath.get());
      lyricsPage.setLyricsPageLinks(maybeLyricsPageLinks.get());
      return Optional.of(lyricsPage);
    } else {
      return Optional.empty();
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<Lyrics> getLyrics(String artist, String title) {
    Optional<LyricsPage> maybeLyricPage = getLyricsPage(artist);
    if (!maybeLyricPage.isPresent()) {
      return Optional.empty();
    }

    final String p1 = ""
        + "[ 　]*[(（].*"
        + "(シングル|アルバム|みっくす|ミックス|ばーじょん|バージョン|らいぶ|ライブ|single|album|mix|version|ver.|live|edit|arrange)"
        + ".*[)）].*$";
    final String p2 = "[ 　]+[〜~-].+[〜~-].*$";
    final String p3 = "[,.、。・•]{3}|…|‥";
    final String p4 = "[ー〜~]";
    NormalizedStringDistance normalizedStringDistance = new NormalizedStringDistance();
    normalizedStringDistance.setNormalizer((s) -> {
      return s.toLowerCase().replaceAll(p1, "").replaceAll(p2, "").replaceAll(p3, "…").replaceAll(p4, "-");
    });

    LyricsPage lyricsPage = maybeLyricPage.get();
    Optional<String> maybeLyricsPagePath = lyricsPage.getLyricsPageLinks().stream().map((lyricsPageLink) -> {
      Float distance = normalizedStringDistance.getDistance(title,
          lyricsPageLink.getSongTitle());
      return Pair.of(lyricsPageLink.getPath(), distance);
    }).filter((pair) -> {
      return pair.getRight() > STRING_DISTANCE_THRESHOLD;
    }).sorted((l, r) -> {
      return l.getRight().compareTo(r.getRight());
    }).map((pair) -> {
      return pair.getLeft();
    }).findFirst();

    if (!maybeLyricsPagePath.isPresent()) {
      return Optional.empty();
    }
    String lyricsPagePath = maybeLyricsPagePath.get();

    Optional<Cache> maybeCache = Optional
        .ofNullable(cacheMapper.selectBySourceAndRequestKey(Source.LYRICS, lyricsPagePath));

    Cache cache;
    if (maybeCache.isPresent()) {
      cache = maybeCache.get();
    } else {
      cacheMapper.insert(createLyricsCache(lyricsPagePath));
      logger.info("cached lyrics: " + lyricsPagePath);
      cache = cacheMapper.selectBySourceAndRequestKey(Source.LYRICS, lyricsPagePath);
    }
    return createLyrics(cache, title, artist);
  }

  private Cache createLyricsArtistSearch(String artistName) {
    Builder builder = ClientBuilder.newClient()
        .target(webProperties.getLyricsSearchTarget())
        .path("/index.php")
        .queryParam("ca", "2") // アーティスト名の中間一致指定
        .queryParam("ka", "{artistName}").resolveTemplate("artistName", artistName)
        .property(ClientProperties.FOLLOW_REDIRECTS, true)
        .property(ClientProperties.CONNECT_TIMEOUT, webProperties.getConnectionTimeout())
        .request();

    Response response = Failsafe.with(createRetryPolicy()).get(() -> {
      requestInterval.takeInterval(Source.LYRICS_ARTIST_SEARCH);
      return builder.get();
    });

    Cache cache = new Cache();
    cache.setSource(Source.LYRICS_ARTIST_SEARCH);
    cache.setCacheKey(artistName);
    cache.setRequestAt(LocalDateTime.now());
    cache.setResponse(response.readEntity(String.class));

    return cache;
  }

  private Optional<String> getArtistLyricsPagePath(Cache cache) {
    StringDistance stringDistance = new NormalizedStringDistance();
    String artistName = cache.getCacheKey();
    String htmlString = cache.getResponse();

    try {
      Document document = Jsoup.parse(htmlString);
      Optional<String> maybePagePath = document.getElementsByTag("a").stream().filter((element) -> {
        return element.hasClass("artist") && element.hasAttr("href");
      }).map((element) -> {
        return Pair.of(element.attr("href"), stringDistance.getDistance(element.text(), artistName));
      }).sorted((l, r) -> {
        return l.getRight().compareTo(r.getRight());
      }).map((pair) -> {
        return pair.getLeft();
      }).findFirst();

      if (maybePagePath.isPresent()) {
        URL url = new URL(maybePagePath.get());
        return Optional.of(url.getPath());
      } else {
        return Optional.empty();
      }
    } catch (MalformedURLException | Error e) {
      logger.warn("html parse error: " + artistName);
      return Optional.empty();
    }
  }

  private Cache createLyricsNamesByArtistCache(String lyricsNamesPagePath) {
    Builder builder = ClientBuilder.newClient()
        .target(webProperties.getLyricsTarget())
        .path(lyricsNamesPagePath)
        .property(ClientProperties.FOLLOW_REDIRECTS, true)
        .property(ClientProperties.CONNECT_TIMEOUT, webProperties.getConnectionTimeout())
        .request();

    Response response = Failsafe.with(createRetryPolicy()).get(() -> {
      requestInterval.takeInterval(Source.LYRICS_NAMES_BY_ARTIST);
      return builder.get();
    });

    Cache cache = new Cache();
    cache.setSource(Source.LYRICS_NAMES_BY_ARTIST);
    cache.setCacheKey(lyricsNamesPagePath);
    cache.setRequestAt(LocalDateTime.now());
    cache.setResponse(response.readEntity(String.class));

    return cache;
  }

  private Optional<List<LyricsPageLink>> createLyricsPageLinks(Cache cache) {
    String path = cache.getCacheKey();
    String htmlString = cache.getResponse();

    try {
      Document document = Jsoup.parse(htmlString);
      List<LyricsPageLink> lyricsPageLinks = document.getElementById("mnb").getElementsByClass("bdy").stream()
          .filter((element) -> {
            // #mnb 配下の .bdy で idがlyで始まる要素が歌詞
            return element.attr("id").startsWith("ly");
          }).map((element) -> {
            // .ttl 配下の a を取得
            Element aElement = element.getElementsByClass("ttl").first().getElementsByTag("a").first();

            LyricsPageLink lyricsPageLink = new LyricsPageLink();
            lyricsPageLink.setSongTitle(aElement.text());
            lyricsPageLink.setPath(aElement.attr("href"));
            return lyricsPageLink;
          }).collect(Collectors.toList());

      return Optional.of(lyricsPageLinks);
    } catch (Error e) {
      logger.warn("html parse error: " + path);
      return Optional.empty();
    }
  }

  private Cache createLyricsCache(String lyricsPagePath) {
    Builder builder = ClientBuilder.newClient()
        .target(webProperties.getLyricsTarget())
        .path(lyricsPagePath)
        .property(ClientProperties.FOLLOW_REDIRECTS, true)
        .property(ClientProperties.CONNECT_TIMEOUT, webProperties.getConnectionTimeout())
        .request();

    Response response = Failsafe.with(createRetryPolicy()).get(() -> {
      requestInterval.takeInterval(Source.LYRICS);
      return builder.get();
    });

    Cache cache = new Cache();
    cache.setSource(Source.LYRICS);
    cache.setCacheKey(lyricsPagePath);
    cache.setRequestAt(LocalDateTime.now());
    cache.setResponse(response.readEntity(String.class));

    return cache;
  }

  private Optional<Lyrics> createLyrics(Cache cache, String title, String artist) {
    String path = cache.getCacheKey();
    String htmlString = cache.getResponse();

    try {
      Document document = Jsoup.parse(htmlString);
      Element lbdyElement = document.getElementById("mnb").getElementsByClass("lbdy").first();
      Lyrics lyrics = new Lyrics();
      lyrics.setTitle(title);
      lyrics.setArtist(artist);
      lyrics.setLyric(lbdyElement.getElementById("Lyric").text());

      lbdyElement.getElementsByClass("sml").stream().forEach((element) -> {
        String type = element.text();
        String value = type.replaceAll("^[^：:]+[：:]", "");
        if (type.startsWith("歌")) {
          lyrics.setSonger(value);
        } else if (type.startsWith("作詞")) {
          lyrics.setLyricist(value);
        } else if (type.startsWith("作曲")) {
          lyrics.setSongWriter(value);
        } else {
          logger.info("unknown type: " + type);
        }
      });
      return Optional.of(lyrics);
    } catch (Error e) {
      logger.warn("html parse error: " + path);
      return Optional.empty();
    }
  }

  private RetryPolicy createRetryPolicy() {
    return new RetryPolicy()
        .retryIf((Response response) -> response.getStatus() >= 500)
        .retryOn((ProcessingException ex) -> ex.getCause() instanceof SocketTimeoutException)
        .withDelay(webProperties.getRequestInterval(), TimeUnit.SECONDS)
        .withMaxRetries(webProperties.getMaxRetryCount());
  }

}
