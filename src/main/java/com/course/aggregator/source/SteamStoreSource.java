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
import java.util.List;
import java.util.Map;

public class SteamStoreSource implements DataSource {

  public static final String NAME = "steam";

  private static final Logger log = LoggerFactory.getLogger(SteamStoreSource.class);
  private static final String BASE_URL = "https://store.steampowered.com/api/appdetails";

  private final ApiHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public SteamStoreSource(ApiHttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Steam Store game details";
  }

  @Override
  public List<AggregatedItem> fetch(Map<String, String> queryParams)
      throws ApiRequestException, DataMappingException {

    String appIds = queryParams.getOrDefault("appids", "570");
    String country = queryParams.getOrDefault("cc", "us");
    String language = queryParams.getOrDefault("l", "english");

    String query = "appids=" + urlEncode(appIds)
        + "&cc=" + urlEncode(country)
        + "&l=" + urlEncode(language);

    URI uri = URI.create(BASE_URL + "?" + query);
    log.info("Fetching Steam Store details for appids={}, cc={}, l={}", appIds, country, language);

    String body = httpClient.getAsString(NAME, uri);

    try {
      JsonNode root = objectMapper.readTree(body);
      List<AggregatedItem> items = new ArrayList<>();
      Instant now = Instant.now();

      root.fields().forEachRemaining(entry -> {
        String appId = entry.getKey();
        JsonNode wrapper = entry.getValue();
        if (wrapper == null || !wrapper.path("success").asBoolean(false)) {
          log.warn("Steam returned success=false for appid={}", appId);
          return;
        }
        JsonNode data = wrapper.get("data");
        if (data == null || !data.isObject()) {
          return;
        }

        String name = textOr(data.get("name"), "App #" + appId);

        AggregatedItem.Builder builder = AggregatedItem.builder()
            .source(NAME)
            .entityType(EntityType.GAME)
            .externalId(appId)
            .name(name)
            .fetchedAt(now)
            .attribute("type", textOr(data.get("type"), null))
            .attribute("is_free", boolOr(data.get("is_free")))
            .attribute("required_age", intOr(data.get("required_age")))
            .attribute("short_description", textOr(data.get("short_description"), null))
            .attribute("website", textOr(data.get("website"), null))
            .attribute("header_image", textOr(data.get("header_image"), null))
            .attribute("cc", country)
            .attribute("language", language);

        JsonNode price = data.get("price_overview");
        if (price != null && price.isObject()) {
          builder.metric("price_final", intOr(price.get("final")));
          builder.metric("price_initial", intOr(price.get("initial")));
          builder.metric("discount_percent", intOr(price.get("discount_percent")));
          builder.attribute("currency", textOr(price.get("currency"), null));
          builder.attribute("price_final_formatted", textOr(price.get("final_formatted"), null));
        }

        JsonNode genres = data.get("genres");
        if (genres != null && genres.isArray()) {
          List<String> genreNames = new ArrayList<>();
          genres.forEach(g -> {
            String gName = textOr(g.get("description"), null);
            if (gName != null) {
              genreNames.add(gName);
            }
          });
          builder.attribute("genres", genreNames);
        }

        JsonNode categories = data.get("categories");
        if (categories != null && categories.isArray()) {
          builder.attribute("categories_count", categories.size());
        }

        JsonNode metacritic = data.get("metacritic");
        if (metacritic != null && metacritic.isObject()) {
          builder.metric("metacritic_score", intOr(metacritic.get("score")));
        }

        JsonNode recommendations = data.get("recommendations");
        if (recommendations != null && recommendations.isObject()) {
          builder.metric("recommendations_total", intOr(recommendations.get("total")));
        }

        items.add(builder.build());
      });

      log.info("Mapped {} Steam game item(s)", items.size());
      return items;
    } catch (Exception e) {
      throw new DataMappingException(NAME, "Failed to parse Steam Store response: " + e.getMessage(), e);
    }
  }

  private static String textOr(JsonNode node, String fallback) {
    return node != null && !node.isNull() ? node.asText() : fallback;
  }

  private static Boolean boolOr(JsonNode node) {
    return node != null && node.isBoolean() ? node.asBoolean() : null;
  }

  private static Integer intOr(JsonNode node) {
    return node != null && node.isNumber() ? node.asInt() : null;
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
