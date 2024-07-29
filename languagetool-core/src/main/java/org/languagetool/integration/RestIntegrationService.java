package org.languagetool.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public class RestIntegrationService<REQUEST, RESPONSE> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public RESPONSE get(String url, REQUEST body, Class<RESPONSE> responseClass) {

    return executeRequest(url, "GET", body, responseClass);
  }

  public RESPONSE post(String url, REQUEST body, Class<RESPONSE> responseClass) {

    return executeRequest(url, "POST", body, responseClass);
  }

  private RESPONSE executeRequest(String url, String method, REQUEST body, Class<RESPONSE> responseClass) {

    final Optional<String> requestBody = Optional.ofNullable(body)
      .map(this::serializeObject);

    HttpURLConnection connection = null;

    try {

      connection = prepareConnection(url, method);
      if (requestBody.isPresent()) writeRequestBody(connection, requestBody.get());
      String responseBody = readResponseBody(connection);
      return deserializeObject(responseBody, responseClass);

    } catch (Exception e) {

      e.printStackTrace();
      return null;

    } finally {

      if (connection != null) {

        connection.disconnect();
      }
    }
  }

  private HttpURLConnection prepareConnection(String url, String method) throws IOException {

    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod(method);
    connection.setRequestProperty("Content-Type", "application/json; utf-8");
    connection.setRequestProperty("Accept", "application/json");
    connection.setDoOutput(true);

    return connection;
  }

  private void writeRequestBody(HttpURLConnection connection, String bodyContend) {

    try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
      wr.write(bodyContend.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String readResponseBody(HttpURLConnection connection) throws IOException {

    StringBuilder response = new StringBuilder();
    try (BufferedReader br = new BufferedReader(
      new InputStreamReader(connection.getInputStream(), "utf-8"))) {
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(responseLine.trim());
      }
    }

    return response.toString();
  }

  @SneakyThrows
  private String serializeObject(REQUEST request) {

    return objectMapper.writeValueAsString(request);
  }

  @SneakyThrows
  private RESPONSE deserializeObject(String response, Class<RESPONSE> responseClass) {

    return objectMapper.readValue(response, responseClass);
  }
}
