package com.github.yktakaha4.watsonmusic.api.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class PostedText {
  @NotNull
  @Size(min = 1, max = 300)
  private String text;
}
