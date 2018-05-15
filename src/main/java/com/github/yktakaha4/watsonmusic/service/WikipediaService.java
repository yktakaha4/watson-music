package com.github.yktakaha4.watsonmusic.service;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.mapper.CacheMapper;
import com.github.yktakaha4.watsonmusic.model.Cache;
import com.github.yktakaha4.watsonmusic.model.LinkType;
import com.github.yktakaha4.watsonmusic.model.Source;
import com.github.yktakaha4.watsonmusic.model.WikiPage;
import com.github.yktakaha4.watsonmusic.util.RequestInterval;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Service
public class WikipediaService {
  private final WebProperties webProperties;
  private final CacheMapper cacheMapper;
  private final RequestInterval requestInterval;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public WikipediaService(WebProperties webProperties, CacheMapper cacheMapper, RequestInterval requestInterval) {
    this.webProperties = webProperties;
    this.cacheMapper = cacheMapper;
    this.requestInterval = requestInterval;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Optional<WikiPage> getWikiPage(String title) {
    Optional<Cache> maybeCache = Optional.ofNullable(cacheMapper.selectBySourceAndRequestKey(Source.WIKIPEDIA, title));
    Cache cache;
    if (maybeCache.isPresent()) {
      cache = maybeCache.get();

    } else {
      cacheMapper.insert(createCache(title));
      logger.info("cached wikipedia page: " + title);
      cache = cacheMapper.selectBySourceAndRequestKey(Source.WIKIPEDIA, title);

    }

    return createWikipage(cache);
  }

  private Cache createCache(String title) {
    Builder builder = ClientBuilder.newClient()
        .target(webProperties.getWikipediaTarget())
        .path("/w/api.php")
        .queryParam("action", "parse")
        .queryParam("page", "{title}").resolveTemplate("title", title)
        .queryParam("redirects", "redirects")
        .queryParam("prop", "text")
        .queryParam("format", "json")
        .property(ClientProperties.FOLLOW_REDIRECTS, true)
        .property(ClientProperties.CONNECT_TIMEOUT, webProperties.getConnectionTimeout())
        .request();

    RetryPolicy retryPolicy = new RetryPolicy()
        .retryIf((Response response) -> response.getStatus() >= 500)
        .retryOn((ProcessingException ex) -> ex.getCause() instanceof SocketTimeoutException)
        .withDelay(webProperties.getRequestInterval(), TimeUnit.SECONDS)
        .withMaxRetries(webProperties.getMaxRetryCount());

    Response response = Failsafe.with(retryPolicy).get(() -> {
      requestInterval.takeInterval(Source.WIKIPEDIA);
      return builder.get();
    });

    Cache cache = new Cache();
    cache.setSource(Source.WIKIPEDIA);
    cache.setCacheKey(title);
    cache.setRequestAt(LocalDateTime.now());
    cache.setResponse(response.readEntity(String.class));

    return cache;
  }

  private Optional<WikiPage> createWikipage(Cache cache) {
    String title = cache.getCacheKey();
    String htmlString;
    try {
      JSONObject jsonObject = new JSONObject(cache.getResponse());
      if (!jsonObject.has("parse")) {
        logger.info("page unfound: " + title);
        return Optional.empty();
      }
      htmlString = jsonObject
          .getJSONObject("parse")
          .getJSONObject("text")
          .getString("*");
    } catch (JSONException e) {
      logger.warn("page parse error: " + title);
      return Optional.empty();
    }
    try {
      WikiPage wikiPage = new WikiPage();
      wikiPage.setTitle(title);

      Element root = Jsoup.parse(htmlString).getElementsByClass("mw-parser-output").first();
      // コメントと不要項目を削除
      removeComments(root.childNodes());
      root.getElementsByClass("toc").remove();
      root.getElementsByClass("asbox").remove();
      root.getElementsByClass("noprint").remove();
      root.getElementsByClass("reference").remove();
      root.getElementsByClass("mw-editsection").remove();
      wikiPage.setContent(root.text());
      wikiPage.setRaw(root.html());

      List<Pair<String, LinkType>> linkTitles = new ArrayList<>();
      root.getElementsByTag("a").stream().filter((element) -> {
        // Wikipedia内の記事へのリンクのみ抽出
        return element.attr("href").matches("^/wiki/[%\\w]+$");
      }).sorted((l, r) -> {
        return l.text().compareTo(r.text());
      }).forEach((element) -> {
        // リンクを含むタグの区別
        String linkTitleValue = element.attr("title");
        if (!linkTitles.stream().filter(lt -> lt.getKey().equals(linkTitleValue)).findFirst().isPresent()) {
          LinkType linkType;
          if (hasClassInAnyParent(element, "infobox")) {
            linkType = LinkType.INFOBOX;
          } else if (hasClassInAnyParent(element, "navbox")) {
            linkType = LinkType.NAVBOX;
          } else {
            linkType = LinkType.NORMAL;
          }
          linkTitles.add(Pair.of(linkTitleValue, linkType));
        }
      });
      wikiPage.setLinkTitles(linkTitles);

      return Optional.of(wikiPage);
    } catch (Error e) {
      logger.warn("html parse error: " + title);
      return Optional.empty();
    }
  }

  private void removeComments(List<Node> nodes) {
    nodes.forEach((node) -> {
      if (node.nodeName().equalsIgnoreCase("#comments")) {
        node.remove();
      } else if (node.childNodeSize() > 0) {
        removeComments(node.childNodes());
      }
    });
  }

  private boolean hasClassInAnyParent(Element element, String className) {
    if (element.hasClass(className)) {
      return true;
    } else {
      if (element.hasParent()) {
        return hasClassInAnyParent(element.parent(), className);
      } else {
        return false;
      }
    }
  }
}
