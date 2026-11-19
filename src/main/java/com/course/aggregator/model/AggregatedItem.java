package com.course.aggregator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AggregatedItem {

  private final String source;
  private final EntityType entityType;
  private final String externalId;
  private final String name;
  private final Instant fetchedAt;
  private final Map<String, Object> metrics;
  private final Map<String, Object> attributes;

  @JsonCreator
  public AggregatedItem(
      @JsonProperty("source") String source,
      @JsonProperty("entityType") EntityType entityType,
      @JsonProperty("externalId") String externalId,
      @JsonProperty("name") String name,
      @JsonProperty("fetchedAt") Instant fetchedAt,
      @JsonProperty("metrics") Map<String, Object> metrics,
      @JsonProperty("attributes") Map<String, Object> attributes) {
    this.source = Objects.requireNonNull(source, "source");
    this.entityType = entityType != null ? entityType : EntityType.UNKNOWN;
    this.externalId = Objects.requireNonNull(externalId, "externalId");
    this.name = Objects.requireNonNull(name, "name");
    this.fetchedAt = fetchedAt != null ? fetchedAt : Instant.now();
    this.metrics = metrics == null
        ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(metrics));
    this.attributes = attributes == null
        ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
  }

  private AggregatedItem(Builder builder) {
    this(
        builder.source,
        builder.entityType,
        builder.externalId,
        builder.name,
        builder.fetchedAt,
        builder.metrics,
        builder.attributes);
  }

  @JsonProperty("source")
  public String getSource() {
    return source;
  }

  @JsonProperty("entityType")
  public EntityType getEntityType() {
    return entityType;
  }

  @JsonProperty("externalId")
  public String getExternalId() {
    return externalId;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("fetchedAt")
  public Instant getFetchedAt() {
    return fetchedAt;
  }

  @JsonProperty("metrics")
  public Map<String, Object> getMetrics() {
    return metrics;
  }

  @JsonProperty("attributes")
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String source;
    private EntityType entityType = EntityType.UNKNOWN;
    private String externalId;
    private String name;
    private Instant fetchedAt = Instant.now();
    private final Map<String, Object> metrics = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder entityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder externalId(String externalId) {
      this.externalId = externalId;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder fetchedAt(Instant fetchedAt) {
      this.fetchedAt = fetchedAt;
      return this;
    }

    public Builder metric(String key, Object value) {
      if (key != null && value != null) {
        this.metrics.put(key, value);
      }
      return this;
    }

    public Builder metrics(Map<String, Object> metrics) {
      if (metrics != null) {
        this.metrics.putAll(metrics);
      }
      return this;
    }

    public Builder attribute(String key, Object value) {
      if (key != null && value != null) {
        this.attributes.put(key, value);
      }
      return this;
    }

    public Builder attributes(Map<String, Object> attributes) {
      if (attributes != null) {
        this.attributes.putAll(attributes);
      }
      return this;
    }

    public AggregatedItem build() {
      return new AggregatedItem(this);
    }
  }

  @Override
  public String toString() {
    return "AggregatedItem{source='" + source + "', entityType=" + entityType
        + ", externalId='" + externalId + "', name='" + name + "'}";
  }
}
