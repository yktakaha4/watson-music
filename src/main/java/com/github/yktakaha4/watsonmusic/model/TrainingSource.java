package com.github.yktakaha4.watsonmusic.model;

import lombok.Data;

@Data
public class TrainingSource {
  private String query;
  private String documentId;
  private Integer musicId;
  private Integer relevance;

}
