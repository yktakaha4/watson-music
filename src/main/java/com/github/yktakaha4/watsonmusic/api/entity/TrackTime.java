package com.github.yktakaha4.watsonmusic.api.entity;

import lombok.Data;

@Data
public class TrackTime {
  private String trackTag;
  private Integer trackLength;
  private Integer elapsed;
}
