package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Artwork implements HasTextTag {
  private Integer artworkId;
  private String mimetype;
  private byte[] image;
  private String imageHash;
  private String textTag;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
