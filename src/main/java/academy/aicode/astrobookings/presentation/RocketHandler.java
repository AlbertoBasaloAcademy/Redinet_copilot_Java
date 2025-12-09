package academy.aicode.astrobookings.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Rocket;

/**
 * Handler HTTP para el recurso `/rockets`.
 * Acepta únicamente peticiones POST que contienen el JSON de un {@link Rocket}.
 */
public class RocketHandler extends BaseHandler {

  private final RocketRepository rocketRepository = new RocketRepository();

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();

    if ("POST".equals(method)) {
      handlePost(exchange);
    } else if ("GET".equals(method)) {
      handleGet(exchange);
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
        List<Rocket> all = rocketRepository.findAll();
        response = this.objectMapper.writeValueAsString(all);
        statusCode = 200;
      } else {
        // remove leading '/'
        String id = relative.startsWith("/") ? relative.substring(1) : relative;
        Rocket r = rocketRepository.findById(id);
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

      // Business validations mixed with input validation
      String error = validateRocket(rocket);
      if (error != null) {
        statusCode = 400;
        response = "{\"error\": \"" + error + "\"}";
      } else {
        Rocket saved = rocketRepository.save(rocket);
        statusCode = 201;
        response = this.objectMapper.writeValueAsString(saved);
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
  private String validateRocket(Rocket rocket) {
    if (rocket.getName() == null || rocket.getName().trim().isEmpty()) {
      return "Rocket name must be provided";
    }
    if (rocket.getCapacity() <= 0 || rocket.getCapacity() > 10) {
      return "Rocket capacity must be between 1 and 10";
    }
    // Speed is optional, no validation
    return null;
  }

}
