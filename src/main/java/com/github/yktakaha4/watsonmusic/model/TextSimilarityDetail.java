package com.github.yktakaha4.watsonmusic.model;

import lombok.Data;

@Data
public class TextSimilarityDetail {
  private String word;
  private Double tfIdf;
  private Double bm25;
}
