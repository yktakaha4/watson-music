package com.github.yktakaha4.watsonmusic.api.endpoint;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.jaudiotagger.tag.FieldKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.WebProperties;
import com.github.yktakaha4.watsonmusic.api.dj.DiscJockey;
import com.github.yktakaha4.watsonmusic.api.dj.JockingTrack;
import com.github.yktakaha4.watsonmusic.api.entity.Track;
import com.github.yktakaha4.watsonmusic.api.entity.TrackTime;
import com.github.yktakaha4.watsonmusic.model.Music;
import com.github.yktakaha4.watsonmusic.service.MusicService;
import com.github.yktakaha4.watsonmusic.service.TrackService;

@RestController
@RequestMapping("/api/tracks")
public class Tracks {
  private static final String UNKNOWN = "???";

  private final WebProperties webProperties;
  private final TrackService trackService;
  private final MusicService musicService;
  private final DiscJockey discJockey;

  private final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();

  @Autowired
  public Tracks(WebProperties webProperties, MusicService musicService, TrackService trackService,
      DiscJockey discJockey) {
    this.webProperties = webProperties;
    this.musicService = musicService;
    this.trackService = trackService;
    this.discJockey = discJockey;
  }

  @RequestMapping(path = "/now-playing", method = RequestMethod.GET)
  public Track getNowPlaying() {
    JockingTrack jockingTrack = discJockey.getJockingTrack()
        .orElseThrow(() -> new ApplicationException("no_jocking_tracks"));
    Music music = jockingTrack.getMusic();

    Track track = createTrack(jockingTrack.getTrack(), music);
    TrackTime trackTime = getTrackTime(jockingTrack);
    track.setTrackTime(trackTime);

    return track;
  }

  @RequestMapping(path = "/now-playing/time", method = RequestMethod.GET)
  public TrackTime getNowPlayingTime() {
    JockingTrack jockingTrack = discJockey.getJockingTrack()
        .orElseThrow(() -> new ApplicationException("no_jocking_tracks"));
    return getTrackTime(jockingTrack);
  }

  @RequestMapping(path = "/histories", method = RequestMethod.GET)
  public List<Track> getHistories() {
    return trackService.getPlayedTracksFrom(LocalDateTime.now().minusHours(webProperties.getPlayedAtHour())).stream()
        .map((source) -> {
          return Pair.of(source, musicService.getByMusicIdIgnoreArtworks(source.getMusicId()));
        }).filter(pair -> pair.getRight().isPresent()).map(pair -> {
          return createTrack(pair.getLeft(), pair.getRight().get());
        }).collect(Collectors.toList());
  }

  private Track createTrack(com.github.yktakaha4.watsonmusic.model.Track sourceTrack, Music music) {
    Track track = new Track();
    track.setTrackTag(sourceTrack.getTrackTag());

    if (music.getArtworks() != null) {
      music.getArtworks().stream().findFirst().ifPresent(artwork -> {
        try {
          String extension = mimeTypes.forName(artwork.getMimetype()).getExtension().replaceAll("^\\.", "");
          track.setArtwork(String.format("./api/artworks/%s/%s", music.getMusicId(), extension));
        } catch (MimeTypeException e) {
        }
      });
    }
    if (Optional.ofNullable(sourceTrack.getPlayedAt()).isPresent()) {
      track.setPlayedAt(ZonedDateTime.ofLocal(sourceTrack.getPlayedAt(), ZoneId.of("UTC"), null).toEpochSecond());
    }
    track.setSongTitle(music.getMusicTagValue(FieldKey.TITLE).orElse(UNKNOWN));
    track.setAlbumTitle(music.getMusicTagValue(FieldKey.ALBUM).orElse(UNKNOWN));
    track.setArtistName(
        music.getMusicTagValue(FieldKey.ARTIST)
            .orElse(music.getMusicTagValue(FieldKey.ALBUM_ARTIST).orElse(UNKNOWN)));
    track.setYear(music.getMusicTagValue(FieldKey.YEAR).orElse(UNKNOWN));

    return track;
  }

  private TrackTime getTrackTime(JockingTrack jockingTrack) {
    Music music = jockingTrack.getMusic();
    TrackTime trackTime = new TrackTime();
    trackTime.setTrackTag(jockingTrack.getTrack().getTrackTag());
    trackTime.setTrackLength(music.getTrackLength());
    trackTime.setElapsed(Long.valueOf(Duration.between(
        jockingTrack.getPlayingAt(),
        LocalDateTime.now()).getSeconds()).intValue());

    return trackTime;
  }

}
