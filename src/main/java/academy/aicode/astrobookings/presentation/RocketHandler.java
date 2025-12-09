package academy.aicode.astrobookings.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import academy.aicode.astrobookings.business.RocketService;
import academy.aicode.astrobookings.persistence.models.Rocket;

/**
 * Handler HTTP para el recurso `/rockets`.
 * Acepta únicamente peticiones POST que contienen el JSON de un {@link Rocket}.
 */
public class RocketHandler extends BaseHandler {

  private final RocketService rocketService = new RocketService();

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();

    if ("POST".equals(method)) {
      handlePost(exchange);
    } else if ("GET".equals(method)) {
      handleGet(exchange);
    } else if ("PUT".equals(method)) {
      handlePut(exchange);
    } else {
      this.handleMethodNotAllowed(exchange);
    }
  }

  /**
   * Maneja peticiones GET sobre `/rockets`.
   * - `GET /rockets` -> devuelve todos los cohetes
   * - `GET /rockets/{id}` -> devuelve el cohete por id o 404
   */
  private void handleGet(HttpExchange exchange) throws IOException {
    String response = "";
    int statusCode = 200;

    try {
      URI uri = exchange.getRequestURI();
      String path = uri.getPath();
      String context = exchange.getHttpContext().getPath(); // "/rockets"
      String relative = "";
      if (path.length() > context.length()) {
        relative = path.substring(context.length()); // could be "" or "/{id}"
      }

      if (relative == null || relative.isEmpty() || "/".equals(relative)) {
        // return all
        List<Rocket> all = rocketService.findAll();
        response = this.objectMapper.writeValueAsString(all);
        statusCode = 200;
      } else {
        // remove leading '/'
        String id = relative.startsWith("/") ? relative.substring(1) : relative;
        Rocket r = rocketService.findById(id);
        if (r == null) {
          statusCode = 404;
          response = "{\"error\": \"Rocket not found\"}";
        } else {
          response = this.objectMapper.writeValueAsString(r);
          statusCode = 200;
        }
      }
    } catch (Exception e) {
      statusCode = 400;
      response = "{\"error\": \"Invalid request\"}";
    }

    sendResponse(exchange, statusCode, response);
  }

  /**
   * Maneja la creación de un Rocket leyendo el body JSON, validando y
   * guardando en el repositorio en memoria.
   */
  private void handlePost(HttpExchange exchange) throws IOException {
    String response = "";
    int statusCode = 200;

    try {
      // Parse JSON body
      InputStream is = exchange.getRequestBody();
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Rocket rocket = this.objectMapper.readValue(body, Rocket.class);
      try {
        Rocket saved = rocketService.create(rocket);
        statusCode = 201;
        response = this.objectMapper.writeValueAsString(saved);
      } catch (IllegalArgumentException iae) {
        statusCode = 400;
        response = "{\"error\": \"" + iae.getMessage() + "\"}";
      }
    } catch (Exception e) {
      statusCode = 400;
      response = "{\"error\": \"Invalid JSON or request\"}";
    }

    sendResponse(exchange, statusCode, response);
  }

  /**
   * Maneja la actualización de un Rocket: `PUT /rockets/{id}`.
   * El body contiene un JSON con los campos a actualizar (name, capacity, speed).
   */
  private void handlePut(HttpExchange exchange) throws IOException {
    String response = "";
    int statusCode = 200;

    try {
      URI uri = exchange.getRequestURI();
      String path = uri.getPath();
      String context = exchange.getHttpContext().getPath(); // "/rockets"
      String relative = "";
      if (path.length() > context.length()) {
        relative = path.substring(context.length()); // should be like "/{id}"
      }

      if (relative == null || relative.isEmpty() || "/".equals(relative)) {
        statusCode = 400;
        response = "{\"error\": \"Rocket id must be provided in path\"}";
      } else {
        String id = relative.startsWith("/") ? relative.substring(1) : relative;

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Rocket updates = this.objectMapper.readValue(body, Rocket.class);
        try {
          Rocket updated = rocketService.update(id, updates);
          if (updated == null) {
            statusCode = 404;
            response = "{\"error\": \"Rocket not found\"}";
          } else {
            statusCode = 200;
            response = this.objectMapper.writeValueAsString(updated);
          }
        } catch (IllegalArgumentException iae) {
          statusCode = 400;
          response = "{\"error\": \"" + iae.getMessage() + "\"}";
        }
      }
    } catch (Exception e) {
      statusCode = 400;
      response = "{\"error\": \"Invalid JSON or request\"}";
    }

    sendResponse(exchange, statusCode, response);
  }

  /**
   * Validaciones simples de negocio y entrada.
   * 
   * @return mensaje de error si no pasa validación, o null si es válido.
   */
  // Validation moved to business layer (RocketService)

}
