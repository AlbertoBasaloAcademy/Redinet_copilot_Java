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

import academy.aicode.astrobookings.business.FlightService;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;
import academy.aicode.astrobookings.presentation.dto.FlightResponse;

/**
 * HTTP handler for the `/flights` resource.
 */
public class FlightHandler extends BaseHandler {

  private static final Logger LOGGER = Logger.getLogger(FlightHandler.class.getName());

  private final FlightService flightService = new FlightService();

  /**
   * Handles requests for `/flights` and `/flights/{id}`.
   */
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

  private void handlePost(HttpExchange exchange) throws IOException {
    String response;
    int statusCode;

    try {
      String relative = getRelativePath(exchange);
      if (relative != null && !relative.isEmpty() && !"/".equals(relative)) {
        String trimmed = relative.startsWith("/") ? relative.substring(1) : relative;
        String[] parts = trimmed.split("/");
        if (parts.length == 2 && "cancel".equals(parts[1])) {
          String id = parts[0] == null ? null : parts[0].trim();
          if (id == null || id.isEmpty()) {
            ErrorResponse er = new ErrorResponse("Invalid id", "INVALID_ID",
                Map.of("field", "id", "message", "id must be provided"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 400;
            sendResponse(exchange, statusCode, response);
            return;
          }

          Flight cancelled = flightService.cancelById(id);
          if (cancelled == null) {
            ErrorResponse er = new ErrorResponse("Flight not found", "NOT_FOUND",
                Map.of("field", "id", "message", "no flight with given id"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 404;
            sendResponse(exchange, statusCode, response);
            return;
          }

          response = this.objectMapper.writeValueAsString(toResponse(cancelled));
          statusCode = 200;
          sendResponse(exchange, statusCode, response);
          return;
        }

        ErrorResponse er = new ErrorResponse("Invalid path", "INVALID_PATH",
            Map.of("field", "path", "message", "POST supports /flights or /flights/{id}/cancel"));
        response = this.objectMapper.writeValueAsString(er);
        statusCode = 400;
        sendResponse(exchange, statusCode, response);
        return;
      }

      InputStream is = exchange.getRequestBody();
      String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      if (body == null || body.trim().isEmpty()) {
        throw new IllegalArgumentException("Request body must be provided");
      }

      CreateFlightRequest req = this.objectMapper.readValue(body, CreateFlightRequest.class);
      Flight created = flightService.create(req);

      response = this.objectMapper.writeValueAsString(toResponse(created));
      statusCode = 201;
    } catch (IllegalArgumentException iae) {
      LOGGER.log(Level.INFO, "Validation failed creating flight: {0}", iae.getMessage());
      ErrorResponse er = new ErrorResponse("Validation failed", "INVALID_INPUT", Map.of("message", iae.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (IllegalStateException ise) {
      LOGGER.log(Level.INFO, "Conflict in flight operation: {0}", ise.getMessage());
      ErrorResponse er = new ErrorResponse("Conflict", "CONFLICT", Map.of("message", ise.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 409;
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.INFO, "Invalid JSON in create flight request", jpe);
      ErrorResponse er = new ErrorResponse("Invalid JSON", "INVALID_JSON", Map.of("message", jpe.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create flight", e);
      ErrorResponse er = new ErrorResponse("Server error", "SERVER_ERROR", Map.of("message", e.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 500;
    }

    sendResponse(exchange, statusCode, response);
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    String response;
    int statusCode;

    try {
      URI uri = exchange.getRequestURI();
      String relative = getRelativePath(exchange);

      if (relative == null || relative.isEmpty() || "/".equals(relative)) {
        Map<String, String> params = parseQuery(uri.getQuery());
        FlightState stateFilter = null;

        String stateRaw = params.get("state");
        if (stateRaw != null && !stateRaw.trim().isEmpty()) {
          try {
            stateFilter = FlightState.valueOf(stateRaw.trim());
          } catch (IllegalArgumentException iae) {
            ErrorResponse er = new ErrorResponse("Invalid state", "INVALID_QUERY",
                Map.of("field", "state", "message", "Unsupported state value"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 400;
            sendResponse(exchange, statusCode, response);
            return;
          }
        }

        List<Flight> flights = flightService.findFutureFlights(stateFilter);
        List<FlightResponse> out = new ArrayList<>();
        for (Flight f : flights) {
          out.add(toResponse(f));
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
          Flight flight = flightService.findById(id.trim());
          if (flight == null) {
            ErrorResponse er = new ErrorResponse("Flight not found", "NOT_FOUND",
                Map.of("field", "id", "message", "no flight with given id"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 404;
          } else {
            response = this.objectMapper.writeValueAsString(toResponse(flight));
            statusCode = 200;
          }
        }
      }
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.WARNING, "JSON processing error in GET /flights", jpe);
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

  private String getRelativePath(HttpExchange exchange) {
    URI uri = exchange.getRequestURI();
    String path = uri.getPath();
    String context = exchange.getHttpContext().getPath();

    if (path.length() <= context.length()) {
      return "";
    }

    return path.substring(context.length());
  }

  private FlightResponse toResponse(Flight flight) {
    if (flight == null) {
      return null;
    }

    return new FlightResponse(flight.getId(), flight.getRocketId(), flight.getLaunchDateTime(), flight.getBasePrice(),
        flight.getMinimumPassengers(), flight.getState());
  }
}
