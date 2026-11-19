package com.course.aggregator;

import com.course.aggregator.cli.CliRunner;
import com.course.aggregator.config.AppFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private Application() {
  }

  public static void main(String[] args) {
    log.info("Valve Meta Aggregator starting");
    CliRunner runner = AppFactory.createCliRunner();
    int exitCode = runner.run(args);
    log.info("Finished with exit code {}", exitCode);
    System.exit(exitCode);
  }
}
