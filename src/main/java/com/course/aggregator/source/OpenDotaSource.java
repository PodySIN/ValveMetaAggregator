package com.course.aggregator.source;

import com.course.aggregator.exception.ApiRequestException;
import com.course.aggregator.exception.DataMappingException;
import com.course.aggregator.http.ApiHttpClient;
import com.course.aggregator.model.AggregatedItem;
import com.course.aggregator.model.EntityType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OpenDotaSource implements DataSource {

  public static final String NAME = "opendota";

  private static final Logger log = LoggerFactory.getLogger(OpenDotaSource.class);
  private static final String BASE_URL = "https://api.opendota.com/api/benchmarks";

  private final ApiHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OpenDotaSource(ApiHttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "OpenDota hero benchmarks (Dota 2)";
  }

  @Override
  public List<AggregatedItem> fetch(Map<String, String> queryParams)
      throws ApiRequestException, DataMappingException {

    String heroId = queryParams.getOrDefault("hero_id", "1");
    URI uri = URI.create(BASE_URL + "?hero_id=" + urlEncode(heroId));

    log.info("Fetching OpenDota benchmarks for hero_id={}", heroId);
    String body = httpClient.getAsString(NAME, uri);

    try {
      JsonNode root = objectMapper.readTree(body);
      String resolvedHeroId = textOr(root.get("hero_id"), heroId);
      JsonNode result = root.get("result");
      if (result == null || !result.isObject()) {
        throw new DataMappingException(NAME, "Missing or invalid 'result' object in benchmarks response");
      }

      AggregatedItem.Builder builder = AggregatedItem.builder()
          .source(NAME)
          .entityType(EntityType.HERO)
          .externalId(resolvedHeroId)
          .name("Dota2 Hero #" + resolvedHeroId)
          .fetchedAt(Instant.now())
          .attribute("endpoint", "benchmarks")
          .attribute("hero_id", resolvedHeroId);

      Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String metricName = entry.getKey();
        JsonNode percentiles = entry.getValue();
        if (percentiles != null && percentiles.isArray()) {
          Double median = extractPercentile(percentiles, 0.5);
          if (median != null) {
            builder.metric(metricName + "_p50", median);
          }
          Double p90 = extractPercentile(percentiles, 0.9);
          if (p90 != null) {
            builder.metric(metricName + "_p90", p90);
          }
          builder.attribute(metricName + "_points", percentiles.size());
        }
      }

      List<AggregatedItem> items = new ArrayList<>(1);
      items.add(builder.build());
      log.info("Mapped {} item(s) from OpenDota for hero_id={}", items.size(), heroId);
      return items;
    } catch (DataMappingException e) {
      throw e;
    } catch (Exception e) {
      throw new DataMappingException(NAME, "Failed to parse OpenDota benchmarks response: " + e.getMessage(), e);
    }
  }

  private static Double extractPercentile(JsonNode array, double target) {
    for (JsonNode node : array) {
      JsonNode p = node.get("percentile");
      JsonNode v = node.get("value");
      if (p != null && v != null && p.isNumber() && v.isNumber()) {
        if (Math.abs(p.asDouble() - target) < 0.001) {
          return v.asDouble();
        }
      }
    }
    return null;
  }

  private static String textOr(JsonNode node, String fallback) {
    return node != null && !node.isNull() ? node.asText() : fallback;
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
