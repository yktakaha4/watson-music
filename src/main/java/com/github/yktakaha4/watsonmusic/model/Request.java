package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Request {
  private String requestTag;
  private String text;
  private String userTag;
  private LocalDateTime createdAt;
  private LocalDateTime playingAt;
  private LocalDateTime playedAt;

}
