package com.github.yktakaha4.watsonmusic.api;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.api.entity.Error;

@ControllerAdvice
public class ApiExceptionHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<Error> handleApplicationException(HttpServletRequest req,
      ApplicationException exception) {

    if (exception.isError()) {
      logger.error("!!! handled error !!!", exception);
    }

    return entity(new Error(exception.getErrorType(), exception.getStatus()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Error> handleMethodArgumentNotValidException(HttpServletRequest req,
      MethodArgumentNotValidException exception) {
    return handleApplicationException(req,
        new ApplicationException("invalid_arguments"));
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Error> handleThrowable(HttpServletRequest req, Throwable throwable) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    logger.error("!!! handled throwable !!!", throwable);

    return entity(new Error(ApplicationException.ERRORTYPE_ERROR, status));
  }

  private ResponseEntity<Error> entity(Error error) {
    return new ResponseEntity<Error>(error, error.getStatus());
  }

}
