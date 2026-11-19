package com.course.aggregator.storage;

import com.course.aggregator.exception.StorageException;
import com.course.aggregator.model.AggregatedItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonDataStorage implements DataStorage {

  private static final Logger log = LoggerFactory.getLogger(JsonDataStorage.class);
  private static final TypeReference<List<AggregatedItem>> LIST_TYPE = new TypeReference<>() {
  };

  private final ObjectMapper objectMapper;

  public JsonDataStorage(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper.copy()
        .enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Override
  public void write(Path path, List<AggregatedItem> items, WriteMode mode) throws StorageException {
    try {
      List<AggregatedItem> toWrite;
      if (mode == WriteMode.APPEND && Files.exists(path)) {
        toWrite = new ArrayList<>(readAll(path));
        toWrite.addAll(items);
        log.info("Appending {} item(s) to existing JSON file {} (total will be {})",
            items.size(), path, toWrite.size());
      } else {
        toWrite = items;
        log.info("Writing {} item(s) to JSON file {}", items.size(), path);
      }
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      objectMapper.writeValue(path.toFile(), toWrite);
    } catch (StorageException e) {
      throw e;
    } catch (IOException e) {
      throw new StorageException("Failed to write JSON to " + path + ": " + e.getMessage(), e);
    }
  }

  @Override
  public List<AggregatedItem> readAll(Path path) throws StorageException {
    if (!Files.exists(path)) {
      throw new StorageException("File does not exist: " + path);
    }
    try {
      List<AggregatedItem> items = objectMapper.readValue(path.toFile(), LIST_TYPE);
      log.debug("Read {} item(s) from {}", items.size(), path);
      return items;
    } catch (IOException e) {
      throw new StorageException("Failed to read JSON from " + path + ": " + e.getMessage(), e);
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
    return "json";
  }
}
