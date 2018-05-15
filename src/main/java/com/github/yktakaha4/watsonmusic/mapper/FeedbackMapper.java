package com.github.yktakaha4.watsonmusic.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.github.yktakaha4.watsonmusic.model.Feedback;

@Mapper
public interface FeedbackMapper {
  List<Feedback> selectAll();

  List<Feedback> selectByUserTag(String userTag);

  void insert(Feedback feedback);

  void updateFeedbackedAtByTag(@Param("feedbackTag") String feedbackTag, @Param("feedbackedAt") LocalDateTime feedbackedAt);
}
