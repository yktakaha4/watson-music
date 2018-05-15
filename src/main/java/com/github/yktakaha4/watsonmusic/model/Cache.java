package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Cache {
  private Integer cacheId;
  private Source source;
  private String cacheKey;
  private String response;
  private LocalDateTime requestAt;
}
