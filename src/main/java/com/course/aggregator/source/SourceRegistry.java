package com.course.aggregator.source;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SourceRegistry {

  private final Map<String, DataSource> sources = new LinkedHashMap<>();

  public void register(DataSource source) {
    if (source == null) {
      throw new IllegalArgumentException("source must not be null");
    }
    String name = source.getName();
    if (sources.containsKey(name)) {
      throw new IllegalArgumentException("Source already registered: " + name);
    }
    sources.put(name, source);
  }

  public Optional<DataSource> find(String name) {
    return Optional.ofNullable(sources.get(name));
  }

  public DataSource getRequired(String name) {
    return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown data source: '" + name
        + "'. Available: " + sources.keySet()));
  }

  public Set<String> names() {
    return Set.copyOf(sources.keySet());
  }

  public Collection<DataSource> all() {
    return sources.values();
  }
}
