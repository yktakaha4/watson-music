package com.github.yktakaha4.watsonmusic.model;

import lombok.Data;

@Data
public class DocumentSource {
  private Integer musicId;
  private Integer textId;
  private Integer relevance;
}
