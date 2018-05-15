package com.github.yktakaha4.watsonmusic.model;

import java.util.List;

import lombok.Data;

@Data
public class TextSimilarity {
  private String word;
  private List<TextSimilarityDetail> details;
}
