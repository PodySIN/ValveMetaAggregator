package com.course.aggregator.exception;

public class DataMappingException extends AggregatorException {

  private final String sourceName;

  public DataMappingException(String sourceName, String message) {
    super(message);
    this.sourceName = sourceName;
  }

  public DataMappingException(String sourceName, String message, Throwable cause) {
    super(message, cause);
    this.sourceName = sourceName;
  }

  public String getSourceName() {
    return sourceName;
  }
}
