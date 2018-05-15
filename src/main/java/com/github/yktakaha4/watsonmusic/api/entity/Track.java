package com.github.yktakaha4.watsonmusic.api.entity;

import lombok.Data;

@Data
public class Track {
  private String trackTag;
  private String songTitle;
  private String albumTitle;
  private String artistName;
  private String year;
  private Long playedAt;

  private String artwork;

  private TrackTime trackTime;
}
