package com.course.aggregator.exception;

public class AggregatorException extends Exception {

  public AggregatorException(String message) {
    super(message);
  }

  public AggregatorException(String message, Throwable cause) {
    super(message, cause);
  }
}
