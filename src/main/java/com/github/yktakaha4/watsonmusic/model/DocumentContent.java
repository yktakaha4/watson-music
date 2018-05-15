package com.github.yktakaha4.watsonmusic.model;

import java.util.Map;

import lombok.Data;

@Data
public class DocumentContent {
  private String text;
  private Integer musicId;
  private String path;
  private String encoding;
  private Integer trackLength;
  private Map<String, String> musicTags;
}
