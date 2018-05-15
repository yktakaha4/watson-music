package com.github.yktakaha4.watsonmusic.model;

import java.util.List;

import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;

import lombok.Data;

@Data
public class VisualRecognitionResult {
  private String imageHash;
  private String imageTitle;
  private List<ClassResult> classResults;

}
