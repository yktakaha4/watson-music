package com.github.yktakaha4.watsonmusic;

import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {
  public static final String ERRORTYPE_ERROR = "error";

  private final HttpStatus status;
  private final String errorType;

  public ApplicationException(Throwable e) {
    super(e);
    status = HttpStatus.INTERNAL_SERVER_ERROR;
    errorType = ERRORTYPE_ERROR;
  }

  public ApplicationException(String errorType) {
    super(HttpStatus.BAD_REQUEST.getReasonPhrase());
    this.status = HttpStatus.BAD_REQUEST;
    this.errorType = errorType;
  }

  public ApplicationException(HttpStatus status) {
    super(status.getReasonPhrase());
    this.status = status;
    this.errorType = ERRORTYPE_ERROR;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getErrorType() {
    return errorType;
  }

  public boolean isError() {
    return ERRORTYPE_ERROR.equals(errorType);
  }

}
