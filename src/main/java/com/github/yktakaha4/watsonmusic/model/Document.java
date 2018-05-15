package com.github.yktakaha4.watsonmusic.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Document {
  private Integer documentId;
  private Integer musicId;
  private String content;
  private String contentHash;
  private String discoveryDocId;
  private LocalDateTime publishedAt;
  private DocumentOperationStatus operationStatus;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public void setOperationStatus(DocumentOperationStatus operationStatus) {
    if (this.operationStatus == null) {
      this.operationStatus = operationStatus;
    } else {
      this.operationStatus = this.operationStatus.next(operationStatus);
    }
  }
}
