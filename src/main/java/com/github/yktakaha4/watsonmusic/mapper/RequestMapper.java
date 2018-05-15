package com.github.yktakaha4.watsonmusic.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Request;

@Mapper
public interface RequestMapper {
  List<Request> selectAll();

  List<Request> selectByPlayedAtIsNull();

  Request selectByTag(String requestTag);

  void insert(Request request);

  void updatePlayingAtByTag(@Param("requestTag") String requestTag, @Param("playingAt") LocalDateTime playingAt);

  void updatePlayedAtByTag(@Param("requestTag") String requestTag, @Param("playedAt") LocalDateTime playedAt);

  void deleteByTag(String requestTag);

}
