package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TextTag implements HasTextTag {
  private Integer textId;
  private String tag;
  private Integer relevance;
  private LocalDateTime createdAt;

  @Override
  public String getTextTag() {
    return getTag();
  }

}
