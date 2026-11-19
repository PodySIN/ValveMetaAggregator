package com.course.aggregator.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StorageFactory {

  private final ObjectMapper objectMapper;

  public StorageFactory(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public DataStorage create(String format) {
    if (format == null) {
      throw new IllegalArgumentException("format must not be null");
    }
    return switch (format.toLowerCase()) {
      case "json" -> new JsonDataStorage(objectMapper);
      case "csv" -> new CsvDataStorage(objectMapper);
      default -> throw new IllegalArgumentException(
          "Unsupported storage format: '" + format + "'. Supported: json, csv");
    };
  }
}
