package com.github.yktakaha4.watsonmusic.model;

import lombok.Data;

@Data
public class DiscoverySearchResult {
  private String documentId;
  private Double score;
  private Integer musicId;
}
