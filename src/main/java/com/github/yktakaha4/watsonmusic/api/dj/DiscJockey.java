package com.github.yktakaha4.watsonmusic.api.dj;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.api.ClientActionType;
import com.github.yktakaha4.watsonmusic.api.util.Socket;
import com.github.yktakaha4.watsonmusic.model.Document;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.model.Request;
import com.github.yktakaha4.watsonmusic.model.Track;
import com.github.yktakaha4.watsonmusic.service.DiscoverySearchService;
import com.github.yktakaha4.watsonmusic.service.DocumentService;
import com.github.yktakaha4.watsonmusic.service.MusicService;
import com.github.yktakaha4.watsonmusic.service.RequestService;
import com.github.yktakaha4.watsonmusic.service.TrackService;
import com.github.yktakaha4.watsonmusic.util.PathUtils;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

@RestController
public class DiscJockey {
  private final WebProperties webProperties;
  private final MusicService musicService;
  private final DocumentService documentService;
  private final RequestService requestService;
  private final TrackService trackService;
  private final DiscoverySearchService discoverySearchService;
  private final PathUtils pathUtils;
  private final Socket socket;

  private final BasicPlayer basicPlayer = new BasicPlayer();
  private final Listener listener = new Listener();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Optional<JockingTrack> jockingTrack = Optional.empty();

  @Autowired
  public DiscJockey(WebProperties webProperties, MusicService musicService, DocumentService documentService,
      RequestService requestService, TrackService trackService,
      DiscoverySearchService discoverySearchService, PathUtils pathUtils, Socket socket) {
    this.webProperties = webProperties;
    this.musicService = musicService;
    this.documentService = documentService;
    this.requestService = requestService;
    this.trackService = trackService;
    this.discoverySearchService = discoverySearchService;
    this.pathUtils = pathUtils;
    this.socket = socket;

    basicPlayer.addBasicPlayerListener(listener);
  }

  @Transactional
  @EventListener(ApplicationReadyEvent.class)
  private void jock() {
    try {
      logger.info("----- jock() -----");
      if (trackService.getPlayableTracks().isEmpty()) {
        chooseTracks();
        jock();
      } else {
        playTrack();
      }
    } catch (Throwable e) {
      logger.error("jockey is dead...", e);
      markPlayedJocking();
    }
  }

  private void chooseTracks() {
    logger.info("----- chooseTracks() -----");
    List<String> ignoreTracks = trackService
        .getPlayedTracksFrom(LocalDateTime.now().minusHours(webProperties.getPlayedAtHour())).stream()
        .sorted((l, r) -> {
          return r.getCreatedAt().compareTo(l.getCreatedAt());
        })
        .limit(webProperties.getIgnoreDocumentsCount())
        .map(track -> track.getDiscoveryDocId())
        .collect(Collectors.toList());

    Optional<Request> maybeRequest = requestService.getPlayableRequest();
    if (maybeRequest.isPresent()) {
      // リクエストがある場合、Discoveryを検索しトラックを生成
      Request request = maybeRequest.get();
      List<String> similarTracks = ignoreTracks.stream().limit(webProperties.getSimilarDocumentsCount())
          .collect(Collectors.toList());
      logger.info("search discovery: " + request.getText());

      List<Track> tracks = discoverySearchService.search(request.getText(), similarTracks, ignoreTracks).stream()
          .peek((result) -> {
            logger.info(
                String.format("choosed from discovery: musicId=%s, score=%s", result.getMusicId(), result.getScore()));
          }).map((result) -> {
            return trackService.createTrack(result.getMusicId(), result.getDocumentId(), request.getRequestTag());
          }).collect(Collectors.toList());

      requestService.markRequestIsPlaying(request.getRequestTag());

      socket.sendToClients(Arrays.asList(ClientActionType.GET_REQUESTS));
      if (tracks.isEmpty()) {
        logger.info("discovery result unfound: " + request.getText());
        requestService.markRequestIsPlayed(request.getRequestTag());
      } else {
        tracks.forEach(trackService::addTrack);
      }
    } else {
      // リクエストがない場合、ランダムにトラックを生成
      List<Document> documents = documentService.getPublishedDocuments();
      Collections.shuffle(documents);

      Optional<Document> maybeDocument = documents.stream()
          .filter(document -> !ignoreTracks.contains(document.getDiscoveryDocId())).findFirst();
      if (maybeDocument.isPresent()) {
        Document document = maybeDocument.get();
        logger.info("choosed random: musicId=" + document.getMusicId());
        trackService.addTrack(trackService.createTrack(document.getMusicId(), document.getDiscoveryDocId()));
      } else {
        logger.error("failed choose any tracks...");
        throw new ApplicationException("document_unfound");
      }
    }
  }

  private void playTrack() {
    logger.info("----- playTrack() -----");
    Track track = trackService.getPlayableTracks().get(0);
    Music music = musicService.getByMusicId(track.getMusicId()).get();

    File musicFile = pathUtils.resolveFromBasePath(Paths.get(music.getPath())).toFile();
    if (musicFile.isFile()) {
      try {
        logger.info("open music: " + musicFile.getPath());
        basicPlayer.open(musicFile);
        logger.info("play music: " + musicFile.getPath());
        basicPlayer.play();
        jockingTrack = Optional.of(new JockingTrack(track, music, LocalDateTime.now()));

        socket.sendToClients(Arrays.asList(ClientActionType.GET_NOW_PLAYING));
      } catch (BasicPlayerException e) {
        logger.error("failed play music file: ", e);
        cancelPlaying(track);
      }
    } else {
      logger.warn("music file unfound: " + musicFile.getPath());
      cancelPlaying(track);
      jock();
    }
  }

  private void markPlayedJocking() {
    jockingTrack.ifPresent((jocking) -> {
      Track track = jocking.getTrack();
      trackService.markTrackIsPlayed(track.getTrackTag());
      if (trackService.getPlayableTracksByRequestTag(track.getRequestTag()).isEmpty()) {
        requestService.markRequestIsPlayed(track.getRequestTag());
      }
    });
    jockingTrack = Optional.empty();
  }
  private void cancelPlaying(Track track) {
    trackService.removeTrack(track.getTrackTag());
    if (trackService.getPlayableTracksByRequestTag(track.getRequestTag()).isEmpty()) {
      requestService.markRequestIsPlayed(track.getRequestTag());
    }
  }

  private class Listener implements BasicPlayerListener {
    @Override
    public void opened(Object stream, @SuppressWarnings("rawtypes") Map properties) {
    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata,
        @SuppressWarnings("rawtypes") Map properties) {
    }

    @Override
    public void stateUpdated(BasicPlayerEvent event) {
      try {
        switch (event.getCode()) {
        case BasicPlayerEvent.STOPPED:
          logger.info("basicPlayer state: STOPPED");
          markPlayedJocking();
          jock();
          break;
        default:
          logger.debug(String.format("basicPlayer state: %s", event.getCode()));
          break;
        }
      } catch (Throwable e) {
        logger.error("jockey is dead...", e);
      }
    }

    @Override
    public void setController(BasicController controller) {
    }

  }

  public Optional<JockingTrack> getJockingTrack() {
    return jockingTrack;
  }

}
