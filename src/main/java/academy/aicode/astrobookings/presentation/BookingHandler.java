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

import academy.aicode.astrobookings.business.BookingConflictException;
import academy.aicode.astrobookings.business.BookingService;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.presentation.dto.BookingResponse;
import academy.aicode.astrobookings.presentation.dto.CreateBookingRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;

/**
 * HTTP handler for the `/bookings` resource.
 */
public class BookingHandler extends BaseHandler {

  private static final Logger LOGGER = Logger.getLogger(BookingHandler.class.getName());

  private final BookingService bookingService = new BookingService();

  /**
   * Handles requests for `/bookings` and `/bookings/{id}`.
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();

    if ("POST".equals(method)) {
      handlePost(exchange);
    } else if ("GET".equals(method)) {
      handleGet(exchange);
    } else {
      handleMethodNotAllowedJson(exchange);
    }
  }

  private void handlePost(HttpExchange exchange) throws IOException {
    String response;
    int statusCode;

    try {
      String relative = getRelativePath(exchange);
      if (relative != null && !relative.isEmpty() && !"/".equals(relative)) {
        ErrorResponse er = new ErrorResponse("Invalid path", "INVALID_PATH",
            Map.of("field", "path", "message", "POST only supports /bookings"));
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

      CreateBookingRequest req = this.objectMapper.readValue(body, CreateBookingRequest.class);
      Booking created = bookingService.create(req);

      response = this.objectMapper.writeValueAsString(toResponse(created));
      statusCode = 201;
    } catch (BookingConflictException bce) {
      LOGGER.log(Level.INFO, "Conflict creating booking: {0}", bce.getMessage());
      ErrorResponse er = new ErrorResponse("Conflict", "CONFLICT", Map.of("message", bce.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 409;
    } catch (IllegalArgumentException iae) {
      LOGGER.log(Level.INFO, "Validation failed creating booking: {0}", iae.getMessage());
      ErrorResponse er = new ErrorResponse("Validation failed", "INVALID_INPUT", Map.of("message", iae.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.INFO, "Invalid JSON in create booking request", jpe);
      ErrorResponse er = new ErrorResponse("Invalid JSON", "INVALID_JSON", Map.of("message", jpe.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to create booking", e);
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
        String flightId = params.get("flightId");
        if (flightId == null || flightId.trim().isEmpty()) {
          ErrorResponse er = new ErrorResponse("Invalid query", "INVALID_QUERY",
              Map.of("field", "flightId", "message", "flightId must be provided"));
          response = this.objectMapper.writeValueAsString(er);
          statusCode = 400;
          sendResponse(exchange, statusCode, response);
          return;
        }

        List<Booking> bookings = bookingService.findByFlightId(flightId.trim());
        List<BookingResponse> out = new ArrayList<>();
        for (Booking b : bookings) {
          out.add(toResponse(b));
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
          Booking booking = bookingService.findById(id.trim());
          if (booking == null) {
            ErrorResponse er = new ErrorResponse("Booking not found", "NOT_FOUND",
                Map.of("field", "id", "message", "no booking with given id"));
            response = this.objectMapper.writeValueAsString(er);
            statusCode = 404;
          } else {
            response = this.objectMapper.writeValueAsString(toResponse(booking));
            statusCode = 200;
          }
        }
      }
    } catch (IllegalArgumentException iae) {
      LOGGER.log(Level.INFO, "Invalid GET /bookings request: {0}", iae.getMessage());
      ErrorResponse er = new ErrorResponse("Invalid request", "INVALID_REQUEST", Map.of("message", iae.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 400;
    } catch (JsonProcessingException jpe) {
      LOGGER.log(Level.WARNING, "JSON processing error in GET /bookings", jpe);
      ErrorResponse er = new ErrorResponse("Invalid response serialization", "SERVER_ERROR",
          Map.of("message", jpe.getMessage()));
      response = this.objectMapper.writeValueAsString(er);
      statusCode = 500;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Invalid GET /bookings request", e);
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

  private void handleMethodNotAllowedJson(HttpExchange exchange) throws IOException {
    ErrorResponse er = new ErrorResponse("Method not allowed", "METHOD_NOT_ALLOWED",
        Map.of("field", "method", "message", "Unsupported HTTP method"));
    String response = this.objectMapper.writeValueAsString(er);
    sendResponse(exchange, 405, response);
  }

  private BookingResponse toResponse(Booking booking) {
    if (booking == null) {
      return null;
    }

    return new BookingResponse(booking.getId(), booking.getFlightId(), booking.getPassengerName(),
        booking.getPassengerDocument(), booking.getFinalPrice(), booking.getDiscountPercent(), booking.getCreatedAt());
  }
}
