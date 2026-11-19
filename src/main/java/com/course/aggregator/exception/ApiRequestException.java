package com.course.aggregator.exception;

public class ApiRequestException extends AggregatorException {

  private final String sourceName;
  private final int statusCode;

  public ApiRequestException(String sourceName, String message) {
    super(message);
    this.sourceName = sourceName;
    this.statusCode = -1;
  }

  public ApiRequestException(String sourceName, int statusCode, String message) {
    super(message);
    this.sourceName = sourceName;
    this.statusCode = statusCode;
  }

  public ApiRequestException(String sourceName, String message, Throwable cause) {
    super(message, cause);
    this.sourceName = sourceName;
    this.statusCode = -1;
  }

  public String getSourceName() {
    return sourceName;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
