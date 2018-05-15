package com.github.yktakaha4.watsonmusic.api.entity;

import javax.validation.constraints.NotNull;

import com.github.yktakaha4.watsonmusic.model.FeedbackType;

import lombok.Data;

@Data
public class Feedback {
  @NotNull
  private FeedbackType feedbackType;
}
