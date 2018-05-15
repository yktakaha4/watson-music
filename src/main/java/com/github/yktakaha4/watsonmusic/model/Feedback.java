package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Feedback {
  private String feedbackTag;
  private FeedbackType feedbackType;
  private String trackTag;
  private String userTag;
  private LocalDateTime createdAt;
  private LocalDateTime feedbackedAt;
}
