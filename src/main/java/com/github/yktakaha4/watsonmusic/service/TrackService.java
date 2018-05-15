package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.mapper.TrackMapper;
import com.github.yktakaha4.watsonmusic.model.Track;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;

@Service
public class TrackService {
  private final TrackMapper trackMapper;
  private final IdentifierUtils identifierUtils;

  @Autowired
  public TrackService(TrackMapper trackMapper, IdentifierUtils identifierUtils) {
    this.trackMapper = trackMapper;
    this.identifierUtils = identifierUtils;
  }

  @Transactional(readOnly = true)
  public List<Track> getPlayedTracks() {
    return trackMapper.selectByPlayedAtIsNotNull();
  }

  @Transactional(readOnly = true)
  public List<Track> getPlayableTracks() {
    return trackMapper.selectByPlayedAtIsNull();
  }

  @Transactional(readOnly = true)
  public List<Track> getPlayableTracksByRequestTag(String requestTag) {
    return trackMapper.selectByPlayedAtIsNull().stream().filter(track -> {
      return StringUtils.equalsIgnoreCase(track.getRequestTag(), requestTag);
    }).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<Track> getPlayedTracksFrom(LocalDateTime localDateTime) {
    return trackMapper.selectByPlayedAtIsNotNullAndPlayedAt(localDateTime);
  }

  @Transactional(readOnly = true)
  public Optional<Track> getTrackByTrackTag(String trackTag) {
    return Optional.ofNullable(trackMapper.selectByTrackTag(trackTag));
  }

  @Transactional
  public void addTrack(Track track) {
    trackMapper.insert(track);
  }

  @Transactional
  public void markTrackIsPlayed(String trackTag) {
    trackMapper.updatePlayedAtByTag(trackTag, LocalDateTime.now());
  }

  public Track createTrack(Integer musicId, String discoveryDocId, String requestTag) {
    Track track = new Track();

    track.setTrackTag(identifierUtils.newTag());
    track.setDiscoveryDocId(discoveryDocId);
    track.setMusicId(musicId);
    track.setCreatedAt(LocalDateTime.now());
    track.setRequestTag(requestTag);

    return track;
  }

  public Track createTrack(Integer musicId, String discoveryDocId) {
    return createTrack(musicId, discoveryDocId, null);
  }

  @Transactional
  public void removeTrack(String trackTag) {
    trackMapper.deleteByTrackTag(trackTag);
  }


}
