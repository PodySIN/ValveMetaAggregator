package com.course.aggregator.cli;

import java.nio.file.Path;

public final class ShowOptions {

  private final Path inputPath;
  private final String format;
  private final String sourceFilter;

  public ShowOptions(Path inputPath, String format, String sourceFilter) {
    this.inputPath = inputPath;
    this.format = format;
    this.sourceFilter = sourceFilter;
  }

  public Path getInputPath() {
    return inputPath;
  }

  public String getFormat() {
    return format;
  }

  public String getSourceFilter() {
    return sourceFilter;
  }
}
