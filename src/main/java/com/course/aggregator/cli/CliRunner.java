package com.course.aggregator.cli;

import com.course.aggregator.exception.ApiRequestException;
import com.course.aggregator.exception.CliException;
import com.course.aggregator.exception.DataMappingException;
import com.course.aggregator.exception.StorageException;
import com.course.aggregator.model.AggregatedItem;
import com.course.aggregator.service.AggregationService;
import com.course.aggregator.source.SourceRegistry;
import com.course.aggregator.storage.DataStorage;
import com.course.aggregator.storage.StorageFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliRunner {

  private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

  private final CommandLineParser parser;
  private final AggregationService aggregationService;
  private final StorageFactory storageFactory;
  private final SourceRegistry sourceRegistry;
  private final ObjectMapper objectMapper;

  public CliRunner(
      CommandLineParser parser,
      AggregationService aggregationService,
      StorageFactory storageFactory,
      SourceRegistry sourceRegistry,
      ObjectMapper objectMapper) {
    this.parser = parser;
    this.aggregationService = aggregationService;
    this.storageFactory = storageFactory;
    this.sourceRegistry = sourceRegistry;
    this.objectMapper = objectMapper;
  }

  public int run(String[] args) {
    try {
      CommandLineParser.ParsedCommand parsed = parser.parse(args);
      return switch (parsed.getCommand()) {
        case HELP -> {
          printHelp();
          yield 0;
        }
        case POLL -> {
          executePoll(parsed.getPollOptions());
          yield 0;
        }
        case SHOW -> {
          executeShow(parsed.getShowOptions());
          yield 0;
        }
      };
    } catch (CliException e) {
      log.error("CLI error: {}", e.getMessage());
      printHelp();
      return 2;
    } catch (ApiRequestException e) {
      log.error("API error from '{}': {} (status={})",
          e.getSourceName(), e.getMessage(), e.getStatusCode());
      return 3;
    } catch (DataMappingException e) {
      log.error("Mapping error from '{}': {}", e.getSourceName(), e.getMessage());
      return 4;
    } catch (StorageException e) {
      log.error("Storage error: {}", e.getMessage());
      return 5;
    } catch (IllegalArgumentException e) {
      log.error("Invalid argument: {}", e.getMessage());
      return 2;
    }
  }

  private void executePoll(PollOptions options)
      throws ApiRequestException, DataMappingException, StorageException {

    Map<String, Map<String, String>> params = new HashMap<>(AggregationService.defaultParams());
    options.getSourceParams().forEach((source, overrides) -> {
      Map<String, String> merged = new HashMap<>(params.getOrDefault(source, Map.of()));
      merged.putAll(overrides);
      params.put(source, merged);
    });

    DataStorage storage = storageFactory.create(options.getFormat());

    log.info("Starting poll: sources={}, format={}, mode={}, output={}",
        options.getSources(), options.getFormat(), options.getWriteMode(), options.getOutputPath());

    List<AggregatedItem> items = aggregationService.pollAndStore(
        options.getSources(),
        params,
        storage,
        options.getOutputPath(),
        options.getWriteMode());

    log.info("Poll finished successfully. Items written: {}", items.size());
  }

  private void executeShow(ShowOptions options) throws StorageException {
    DataStorage storage = storageFactory.create(options.getFormat());
    List<AggregatedItem> items;

    if (options.getSourceFilter() != null && !options.getSourceFilter().isBlank()) {
      items = aggregationService.loadBySource(
          storage, options.getInputPath(), options.getSourceFilter());
      log.info("Showing items from file {} filtered by source='{}' (count={})",
          options.getInputPath(), options.getSourceFilter(), items.size());
    } else {
      items = aggregationService.loadAll(storage, options.getInputPath());
      log.info("Showing all items from file {} (count={})", options.getInputPath(), items.size());
    }

    try {
      String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
      log.info("File content:\n{}", json);
    } catch (Exception e) {
      for (AggregatedItem item : items) {
        log.info("{}", item);
      }
    }
  }

  private void printHelp() {
    log.info(
        """

            Valve Meta Aggregator — Stage 1 (console)

            Usage:
              poll  --sources <list> --format <json|csv> --mode <create|append> [--output <path>]
                    [--opendota.hero_id=<id>] [--deadlock.language=<lang>] [--steam.appids=<ids>]
              show  --file <path> [--format <json|csv>] [--source <name>]
              help

            Mandatory for poll:
              --sources   comma-separated: opendota,deadlock,steam
              --format    json or csv
              --mode      create (overwrite) or append

            Examples:
              poll --sources opendota,deadlock,steam --format json --mode create --output data.json
              poll --sources opendota,steam --format csv --mode append --output data.csv --opendota.hero_id=53 --steam.appids=730
              show --file data.json
              show --file data.json --source opendota

            Available sources: {}
            """,
        sourceRegistry.names());
  }
}
