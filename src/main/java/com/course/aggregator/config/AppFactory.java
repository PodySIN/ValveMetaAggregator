package com.course.aggregator.config;

import com.course.aggregator.cli.CliRunner;
import com.course.aggregator.cli.CommandLineParser;
import com.course.aggregator.http.ApiHttpClient;
import com.course.aggregator.service.AggregationService;
import com.course.aggregator.source.DeadlockSource;
import com.course.aggregator.source.OpenDotaSource;
import com.course.aggregator.source.SourceRegistry;
import com.course.aggregator.source.SteamStoreSource;
import com.course.aggregator.storage.StorageFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

public final class AppFactory {

  private AppFactory() {
  }

  public static CliRunner createCliRunner() {
    ObjectMapper objectMapper = createObjectMapper();
    ApiHttpClient httpClient = new ApiHttpClient(Duration.ofSeconds(20));

    SourceRegistry registry = new SourceRegistry();
    registry.register(new OpenDotaSource(httpClient, objectMapper));
    registry.register(new DeadlockSource(httpClient, objectMapper));
    registry.register(new SteamStoreSource(httpClient, objectMapper));

    AggregationService aggregationService = new AggregationService(registry);
    StorageFactory storageFactory = new StorageFactory(objectMapper);
    CommandLineParser parser = new CommandLineParser();

    return new CliRunner(parser, aggregationService, storageFactory, registry, objectMapper);
  }

  public static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }
}
