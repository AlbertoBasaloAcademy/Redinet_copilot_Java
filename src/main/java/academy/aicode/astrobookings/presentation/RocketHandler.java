package academy.aicode.astrobookings.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;

import academy.aicode.astrobookings.business.RocketService;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;
import academy.aicode.astrobookings.presentation.dto.RocketResponse;

/**
 * Handler HTTP para el recurso `/rockets`.
 * Acepta únicamente peticiones POST que contienen el JSON de un {@link Rocket}.
 */
public class RocketHandler extends BaseHandler {

  private static final Logger LOGGER = Logger.getLogger(RocketHandler.class.getName());

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
    String response;
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
        // list with optional ?name= filter
        Map<String, String> params = parseQuery(uri.getQuery());
        String nameFilter = params.get("name");

        List<Rocket> all = rocketService.findAll();
        List<RocketResponse> out = new ArrayList<>();
        if (nameFilter == null || nameFilter.trim().isEmpty()) {
          for (Rocket r : all) {
            out.add(toResponse(r));
          }
        } else {
          String nf = nameFilter.trim().toLowerCase();
          for (Rocket r : all) {
            if (r.getName() != null && r.getName().toLowerCase().contains(nf)) {
              out.add(toResponse(r));
            }
          }
        }
        response = this.objectMapper.writeValueAsString(out);
        statusCode = 200;
      } else {
        String id = relative.startsWith("/") ? relative.substring(1) : relative;
        if (id == null || id.trim().isEmpty()) {
          ErrorResponse er = new ErrorResponse("Invalid id", "INVALID_ID",
              Map.of("field", "id", "message", "id must be provided"));
          response = this.objectMapper.writeValueAsString(er);
          statusCode = 400;
        } else {
          Rocket r = rocketService.findById(id);
          if (r == null) {
            ErrorResponse er = new ErrorResponse("Rocket not found", "NOT_FOUND",
                Map.of("field", "id", "message", "no rocket with given id"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 404;
          } else {
            response = this.objectMapper.writeValueAsString(toResponse(r));
            statusCode = 200;
          }
        }
      }
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.WARNING, "JSON processing error in GET", jpe);
      ErrorResponse er = new ErrorResponse("Invalid response serialization", "SERVER_ERROR",
          Map.of("message", jpe.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 500;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Invalid GET request", e);
      ErrorResponse er = new ErrorResponse("Invalid request", "INVALID_REQUEST", Map.of("message", e.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    }

    sendResponse(exchange, statusCode, response);
  }

  /**
   * Maneja la creación de un Rocket leyendo el body JSON, validando y
   * guardando en el repositorio en memoria.
   */
  private void handlePost(HttpExchange exchange) throws IOException {
    String response;
    int statusCode = 201;

    try {
      InputStream is = exchange.getRequestBody();
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      CreateRocketRequest req = this.objectMapper.readValue(body, CreateRocketRequest.class);

      Rocket saved = rocketService.create(req);
      response = this.objectMapper.writeValueAsString(toResponse(saved));
      statusCode = 201;
    } catch (IllegalArgumentException iae) {
      LOGGER.log(Level.INFO, "Validation failed creating rocket: {0}", iae.getMessage());
      ErrorResponse er = new ErrorResponse("Validation failed", "INVALID_INPUT", Map.of("message", iae.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.INFO, "Invalid JSON in create request", jpe);
      ErrorResponse er = new ErrorResponse("Invalid JSON", "INVALID_JSON", Map.of("message", jpe.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create rocket", e);
      ErrorResponse er = new ErrorResponse("Server error", "SERVER_ERROR", Map.of("message", e.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 500;
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

  private RocketResponse toResponse(Rocket r) {
    if (r == null)
      return null;
    return new RocketResponse(r.getId(), r.getName(), r.getCapacity(), r.getRange(), r.getSpeed());
  }

  /**
   * Validaciones simples de negocio y entrada.
   * 
   * @return mensaje de error si no pasa validación, o null si es válido.
   */
  // Validation moved to business layer (RocketService)

}
