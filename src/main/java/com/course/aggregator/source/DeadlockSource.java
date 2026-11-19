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

public class DeadlockSource implements DataSource {

  public static final String NAME = "deadlock";

  private static final Logger log = LoggerFactory.getLogger(DeadlockSource.class);
  private static final String BASE_URL = "https://api.deadlock-api.com/v1/assets/heroes";

  private final ApiHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public DeadlockSource(ApiHttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "Deadlock heroes assets and meta";
  }

  @Override
  public List<AggregatedItem> fetch(Map<String, String> queryParams)
      throws ApiRequestException, DataMappingException {

    String language = queryParams.getOrDefault("language", "english");
    String filterHeroId = queryParams.get("hero_id");

    URI uri = URI.create(BASE_URL + "?language=" + urlEncode(language));
    log.info("Fetching Deadlock heroes (language={}, filterHeroId={})", language, filterHeroId);

    String body = httpClient.getAsString(NAME, uri);

    try {
      JsonNode root = objectMapper.readTree(body);
      if (!root.isArray()) {
        throw new DataMappingException(NAME, "Expected JSON array of heroes");
      }

      List<AggregatedItem> items = new ArrayList<>();
      Instant now = Instant.now();

      for (JsonNode hero : root) {
        String id = textOr(hero.get("id"), null);
        if (id == null) {
          continue;
        }
        if (filterHeroId != null && !filterHeroId.equals(id)) {
          continue;
        }

        String name = textOr(hero.get("name"), "Hero #" + id);
        JsonNode description = hero.get("description");

        AggregatedItem.Builder builder = AggregatedItem.builder()
            .source(NAME)
            .entityType(EntityType.HERO)
            .externalId(id)
            .name(name)
            .fetchedAt(now)
            .attribute("class_name", textOr(hero.get("class_name"), null))
            .attribute("hero_type", textOr(hero.get("hero_type"), null))
            .attribute("gender", textOr(hero.get("gender"), null))
            .attribute("language", language)
            .attribute("player_selectable", boolOr(hero.get("player_selectable")))
            .attribute("development_state", textOr(hero.get("development_state"), null));

        if (description != null && description.isObject()) {
          builder.attribute("role", textOr(description.get("role"), null));
          builder.attribute("playstyle", textOr(description.get("playstyle"), null));
          String lore = textOr(description.get("lore"), null);
          if (lore != null && lore.length() > 280) {
            lore = lore.substring(0, 277) + "...";
          }
          builder.attribute("lore", lore);
        }

        JsonNode tags = hero.get("tags");
        if (tags != null && tags.isArray()) {
          List<String> tagList = new ArrayList<>();
          tags.forEach(t -> tagList.add(t.asText()));
          builder.attribute("tags", tagList);
        }

        JsonNode popularItems = hero.get("popular_items");
        if (popularItems != null && popularItems.isObject()) {
          JsonNode early = popularItems.get("early_game");
          if (early != null && early.isArray()) {
            builder.metric("early_game_items_count", early.size());
            double maxPick = 0;
            for (JsonNode item : early) {
              JsonNode pct = item.get("pick_pct");
              if (pct != null && pct.isNumber()) {
                maxPick = Math.max(maxPick, pct.asDouble());
              }
            }
            builder.metric("early_game_max_pick_pct", maxPick);
          }
        }

        items.add(builder.build());
      }

      log.info("Mapped {} Deadlock hero item(s)", items.size());
      return items;
    } catch (DataMappingException e) {
      throw e;
    } catch (Exception e) {
      throw new DataMappingException(NAME, "Failed to parse Deadlock heroes response: " + e.getMessage(), e);
    }
  }

  private static String textOr(JsonNode node, String fallback) {
    return node != null && !node.isNull() ? node.asText() : fallback;
  }

  private static Boolean boolOr(JsonNode node) {
    return node != null && node.isBoolean() ? node.asBoolean() : null;
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
