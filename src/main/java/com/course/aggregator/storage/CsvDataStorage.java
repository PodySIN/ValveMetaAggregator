package com.course.aggregator.storage;

import com.course.aggregator.exception.StorageException;
import com.course.aggregator.model.AggregatedItem;
import com.course.aggregator.model.EntityType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CsvDataStorage implements DataStorage {

  private static final Logger log = LoggerFactory.getLogger(CsvDataStorage.class);

  private static final String[] HEADERS = {
      "source", "entityType", "externalId", "name", "fetchedAt", "metrics", "attributes"
  };

  private final ObjectMapper objectMapper;
  private final CSVFormat format;

  public CsvDataStorage(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.format = CSVFormat.DEFAULT.builder()
        .setHeader(HEADERS)
        .setSkipHeaderRecord(true)
        .setRecordSeparator('\n')
        .build();
  }

  @Override
  public void write(Path path, List<AggregatedItem> items, WriteMode mode) throws StorageException {
    try {
      boolean append = mode == WriteMode.APPEND && Files.exists(path);
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }

      StandardOpenOption[] options = append
          ? new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.APPEND }
          : new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };

      try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options);
          CSVPrinter printer = new CSVPrinter(writer,
              append
                  ? CSVFormat.DEFAULT.builder().setRecordSeparator('\n').build()
                  : CSVFormat.DEFAULT.builder().setHeader(HEADERS).setRecordSeparator('\n').build())) {

        for (AggregatedItem item : items) {
          printer.printRecord(
              item.getSource(),
              item.getEntityType().name(),
              item.getExternalId(),
              item.getName(),
              item.getFetchedAt().toString(),
              toJson(item.getMetrics()),
              toJson(item.getAttributes()));
        }
      }
      log.info("{} {} item(s) to CSV file {}", append ? "Appended" : "Wrote", items.size(), path);
    } catch (IOException e) {
      throw new StorageException("Failed to write CSV to " + path + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<AggregatedItem> readAll(Path path) throws StorageException {
    if (!Files.exists(path)) {
      throw new StorageException("File does not exist: " + path);
    }
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        CSVParser parser = format.parse(reader)) {

      List<AggregatedItem> result = new ArrayList<>();
      for (CSVRecord csv : parser) {
        result.add(fromCsv(csv));
      }
      log.debug("Read {} item(s) from CSV {}", result.size(), path);
      return result;
    } catch (IOException e) {
      throw new StorageException("Failed to read CSV from " + path + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<AggregatedItem> readBySource(Path path, String sourceName) throws StorageException {
    return readAll(path).stream()
        .filter(item -> sourceName.equals(item.getSource()))
        .collect(Collectors.toList());
  }

  @Override
  public String format() {
    return "csv";
  }

  private AggregatedItem fromCsv(CSVRecord csv) throws StorageException {
    try {
      Map<String, Object> metrics = fromJsonMap(csv.get("metrics"));
      Map<String, Object> attributes = fromJsonMap(csv.get("attributes"));
      return AggregatedItem.builder()
          .source(csv.get("source"))
          .entityType(EntityType.valueOf(csv.get("entityType")))
          .externalId(csv.get("externalId"))
          .name(csv.get("name"))
          .fetchedAt(Instant.parse(csv.get("fetchedAt")))
          .metrics(metrics)
          .attributes(attributes)
          .build();
    } catch (RuntimeException e) {
      throw new StorageException("Failed to map CSV row to AggregatedItem: " + e.getMessage(), e);
    }
  }

  private String toJson(Map<String, Object> map) throws StorageException {
    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new StorageException("Failed to serialize map to JSON for CSV column", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> fromJsonMap(String json) throws StorageException {
    try {
      if (json == null || json.isBlank()) {
        return Map.of();
      }
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      throw new StorageException("Failed to parse JSON map from CSV column", e);
    }
  }
}
