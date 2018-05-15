package com.github.yktakaha4.watsonmusic.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Track;

@Mapper
public interface TrackMapper {
  List<Track> selectAll();

  List<Track> selectByPlayedAtIsNull();

  List<Track> selectByPlayedAtIsNotNull();

  List<Track> selectByPlayedAtIsNotNullAndPlayedAt(LocalDateTime playedAt);

  List<Track> selectByRequestTag(String requestTag);

  Track selectByTrackTag(String trackTag);

  void insert(Track track);

  void updatePlayedAtByTag(@Param("trackTag") String trackTag, @Param("playedAt") LocalDateTime playedAt);

  void deleteByTrackTag(String trackTag);

}
