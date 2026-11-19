package com.course.aggregator.http;

import com.course.aggregator.exception.ApiRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public class ApiHttpClient {

  private static final Logger log = LoggerFactory.getLogger(ApiHttpClient.class);

  private final HttpClient httpClient;
  private final Duration timeout;

  public ApiHttpClient(Duration timeout) {
    this.timeout = Objects.requireNonNull(timeout, "timeout");
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public String getAsString(String sourceName, URI uri) throws ApiRequestException {
    log.debug("HTTP GET [{}] {}", sourceName, uri);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(uri)
        .timeout(timeout)
        .header("Accept", "application/json")
        .header("User-Agent", "ValveMetaAggregator/1.0 (course-project)")
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        throw new ApiRequestException(sourceName, status,
            "Unexpected HTTP status " + status + " for " + uri);
      }
      String body = response.body();
      log.debug("HTTP GET [{}] {} -> {} bytes", sourceName, uri, body != null ? body.length() : 0);
      return body;
    } catch (ApiRequestException e) {
      throw e;
    } catch (IOException e) {
      throw new ApiRequestException(sourceName, "I/O error while calling " + uri + ": " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiRequestException(sourceName, "Request interrupted for " + uri, e);
    }
  }
}
