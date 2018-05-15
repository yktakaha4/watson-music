package com.github.yktakaha4.watsonmusic.model;

import org.jaudiotagger.tag.FieldKey;

import lombok.Data;

@Data
public class MusicTag implements HasTextTag {
  private Integer musicId;
  private Integer seq;
  private FieldKey name;
  private String value;
  private String textTag;
}
