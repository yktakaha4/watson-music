package com.github.yktakaha4.watsonmusic.model;

import lombok.Data;

@Data
public class TextLink {
  private Integer textId;
  private Integer seq;
  private String title;
  private LinkType linkType;
}
