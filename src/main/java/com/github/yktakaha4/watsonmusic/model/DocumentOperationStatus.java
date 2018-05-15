package com.github.yktakaha4.watsonmusic.model;

public enum DocumentOperationStatus {
  NO_OP, SHOULD_ADD, SHOULD_UPDATE, SHOULD_DELETE;

  public DocumentOperationStatus next(DocumentOperationStatus nextOperationStatus) {
    switch (nextOperationStatus) {
    case SHOULD_ADD:
    case SHOULD_UPDATE:
    case SHOULD_DELETE:
      if (!NO_OP.equals(this)) {
        throw new IllegalStateException();
      }
      break;
    case NO_OP:
      if (NO_OP.equals(this)) {
        throw new IllegalStateException();
      }
      break;
    }
    return nextOperationStatus;
  }

  public boolean shouldAdd() {
    return SHOULD_ADD.equals(this);
  }

  public boolean shouldUpdate() {
    return SHOULD_UPDATE.equals(this);
  }

  public boolean shouldDelete() {
    return SHOULD_DELETE.equals(this);
  }

  public boolean isNoOp() {
    return NO_OP.equals(this);
  }

}
