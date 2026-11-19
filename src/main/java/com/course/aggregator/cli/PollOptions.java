package com.course.aggregator.cli;

import com.course.aggregator.storage.WriteMode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class PollOptions {

  private final List<String> sources;
  private final String format;
  private final WriteMode writeMode;
  private final Path outputPath;
  private final Map<String, Map<String, String>> sourceParams;

  public PollOptions(
      List<String> sources,
      String format,
      WriteMode writeMode,
      Path outputPath,
      Map<String, Map<String, String>> sourceParams) {
    this.sources = List.copyOf(sources);
    this.format = format;
    this.writeMode = writeMode;
    this.outputPath = outputPath;
    this.sourceParams = Map.copyOf(sourceParams);
  }

  public List<String> getSources() {
    return sources;
  }

  public String getFormat() {
    return format;
  }

  public WriteMode getWriteMode() {
    return writeMode;
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public Map<String, Map<String, String>> getSourceParams() {
    return sourceParams;
  }
}
