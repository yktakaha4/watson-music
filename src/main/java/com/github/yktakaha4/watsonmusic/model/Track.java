package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Track {
  private String trackTag;
  private String requestTag;
  private String discoveryDocId;
  private Integer musicId;
  private LocalDateTime createdAt;
  private LocalDateTime playedAt;
}