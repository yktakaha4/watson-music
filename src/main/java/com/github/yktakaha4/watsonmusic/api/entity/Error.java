package com.github.yktakaha4.watsonmusic.api.entity;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Error {
  private String error;
  private HttpStatus status;
}
