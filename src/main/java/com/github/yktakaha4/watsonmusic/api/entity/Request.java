package com.github.yktakaha4.watsonmusic.api.entity;

import lombok.Data;

@Data
public class Request {
  private String requestId;
  private String text;
  private String type;
  private Long postedAt;
  private Boolean playing;
  private Boolean yours;
  private Boolean deletable;

}
