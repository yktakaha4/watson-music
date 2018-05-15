package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class Text {
  private Integer textId;
  private Source source;
  private String title;
  private String content;
  private String raw;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<TextLink> textLinks;
}
