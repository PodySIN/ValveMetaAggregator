package com.course.aggregator.cli;

import com.course.aggregator.exception.CliException;
import com.course.aggregator.storage.WriteMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandLineParser {

  public ParsedCommand parse(String[] args) throws CliException {
    if (args == null || args.length == 0) {
      return new ParsedCommand(CliCommand.HELP, null, null);
    }

    String commandToken = args[0].toLowerCase(Locale.ROOT);
    CliCommand command = switch (commandToken) {
      case "poll" -> CliCommand.POLL;
      case "show" -> CliCommand.SHOW;
      case "help", "-h", "--help" -> CliCommand.HELP;
      default -> throw new CliException(
          "Unknown command: '" + args[0] + "'. Use: poll | show | help");
    };

    Map<String, String> flags = parseFlags(Arrays.copyOfRange(args, 1, args.length));

    return switch (command) {
      case POLL -> new ParsedCommand(command, parsePoll(flags), null);
      case SHOW -> new ParsedCommand(command, null, parseShow(flags));
      case HELP -> new ParsedCommand(command, null, null);
    };
  }

  private PollOptions parsePoll(Map<String, String> flags) throws CliException {
    String sourcesRaw = required(flags, "sources");
    List<String> sources = new ArrayList<>();
    for (String part : sourcesRaw.split(",")) {
      String trimmed = part.trim().toLowerCase(Locale.ROOT);
      if (!trimmed.isEmpty()) {
        sources.add(trimmed);
      }
    }
    if (sources.isEmpty()) {
      throw new CliException("--sources must list at least one source (comma-separated)");
    }

    String format = flags.getOrDefault("format", "json").toLowerCase(Locale.ROOT);
    if (!format.equals("json") && !format.equals("csv")) {
      throw new CliException("--format must be 'json' or 'csv'");
    }

    String modeRaw = flags.getOrDefault("mode", "create").toLowerCase(Locale.ROOT);
    WriteMode mode = switch (modeRaw) {
      case "create", "new" -> WriteMode.CREATE;
      case "append" -> WriteMode.APPEND;
      default -> throw new CliException("--mode must be 'create' or 'append'");
    };

    String output = flags.getOrDefault("output", "data." + format);
    Path outputPath = Path.of(output);

    Map<String, Map<String, String>> sourceParams = new LinkedHashMap<>();
    for (Map.Entry<String, String> e : flags.entrySet()) {
      String key = e.getKey();
      int dot = key.indexOf('.');
      if (dot > 0) {
        String source = key.substring(0, dot).toLowerCase(Locale.ROOT);
        String param = key.substring(dot + 1);
        sourceParams.computeIfAbsent(source, k -> new HashMap<>()).put(param, e.getValue());
      }
    }

    return new PollOptions(sources, format, mode, outputPath, sourceParams);
  }

  private ShowOptions parseShow(Map<String, String> flags) throws CliException {
    String input = required(flags, "file");
    String format = flags.getOrDefault("format", guessFormat(input)).toLowerCase(Locale.ROOT);
    if (!format.equals("json") && !format.equals("csv")) {
      throw new CliException("--format must be 'json' or 'csv'");
    }
    String source = flags.get("source");
    return new ShowOptions(Path.of(input), format, source);
  }

  private static String guessFormat(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".csv")) {
      return "csv";
    }
    return "json";
  }

  private static Map<String, String> parseFlags(String[] tokens) throws CliException {
    Map<String, String> flags = new LinkedHashMap<>();
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if (!token.startsWith("--")) {
        throw new CliException("Unexpected argument: " + token + " (expected --flag=value or --flag value)");
      }
      String body = token.substring(2);
      String key;
      String value;
      int eq = body.indexOf('=');
      if (eq >= 0) {
        key = body.substring(0, eq);
        value = body.substring(eq + 1);
      } else {
        key = body;
        if (i + 1 >= tokens.length || tokens[i + 1].startsWith("--")) {
          throw new CliException("Missing value for --" + key);
        }
        value = tokens[++i];
      }
      flags.put(key.toLowerCase(Locale.ROOT), value);
    }
    return flags;
  }

  private static String required(Map<String, String> flags, String key) throws CliException {
    String value = flags.get(key);
    if (value == null || value.isBlank()) {
      throw new CliException("Missing required option --" + key);
    }
    return value;
  }

  public static final class ParsedCommand {
    private final CliCommand command;
    private final PollOptions pollOptions;
    private final ShowOptions showOptions;

    public ParsedCommand(CliCommand command, PollOptions pollOptions, ShowOptions showOptions) {
      this.command = command;
      this.pollOptions = pollOptions;
      this.showOptions = showOptions;
    }

    public CliCommand getCommand() {
      return command;
    }

    public PollOptions getPollOptions() {
      return pollOptions;
    }

    public ShowOptions getShowOptions() {
      return showOptions;
    }
  }
}
