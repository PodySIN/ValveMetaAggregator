package com.course.aggregator.service;

import com.course.aggregator.exception.ApiRequestException;
import com.course.aggregator.exception.DataMappingException;
import com.course.aggregator.exception.StorageException;
import com.course.aggregator.model.AggregatedItem;
import com.course.aggregator.source.DataSource;
import com.course.aggregator.source.SourceRegistry;
import com.course.aggregator.storage.DataStorage;
import com.course.aggregator.storage.WriteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AggregationService {

  private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

  private final SourceRegistry sourceRegistry;

  public AggregationService(SourceRegistry sourceRegistry) {
    this.sourceRegistry = sourceRegistry;
  }

  public List<AggregatedItem> pollAndStore(
      List<String> sourceNames,
      Map<String, Map<String, String>> perSourceParams,
      DataStorage storage,
      Path outputPath,
      WriteMode writeMode) throws ApiRequestException, DataMappingException, StorageException {

    List<AggregatedItem> all = new ArrayList<>();

    for (String name : sourceNames) {
      DataSource source = sourceRegistry.getRequired(name);
      Map<String, String> params = perSourceParams.getOrDefault(name, Map.of());
      log.info("Polling source '{}' ({}) with params {}", name, source.getDescription(), params);

      List<AggregatedItem> items = source.fetch(params);
      log.info("Source '{}' returned {} item(s)", name, items.size());
      all.addAll(items);
    }

    storage.write(outputPath, all, writeMode);
    log.info("Stored {} total item(s) into {} (mode={}, format={})",
        all.size(), outputPath, writeMode, storage.format());
    return all;
  }

  public List<AggregatedItem> loadAll(DataStorage storage, Path path) throws StorageException {
    return storage.readAll(path);
  }

  public List<AggregatedItem> loadBySource(DataStorage storage, Path path, String sourceName)
      throws StorageException {
    return storage.readBySource(path, sourceName);
  }

  public static Map<String, Map<String, String>> defaultParams() {
    Map<String, Map<String, String>> defaults = new LinkedHashMap<>();
    defaults.put("opendota", Map.of("hero_id", "1"));
    defaults.put("deadlock", Map.of("language", "english"));
    defaults.put("steam", Map.of("appids", "570", "cc", "us", "l", "english"));
    return defaults;
  }
}
