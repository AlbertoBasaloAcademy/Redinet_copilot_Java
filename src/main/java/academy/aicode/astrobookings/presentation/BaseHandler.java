package academy.aicode.astrobookings.presentation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Clase base para handlers HTTP del servidor embebido.
 * Proporciona un `ObjectMapper` configurado y utilidades para enviar respuestas
 * y parsear queries.
 */
public abstract class BaseHandler implements HttpHandler {

  protected final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /**
   * Envía una respuesta JSON con el código HTTP indicado.
   */
  protected void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, response.getBytes().length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(response.getBytes());
    }
  }

  /**
   * Respuesta estándar para métodos no permitidos (405).
   */
  protected void handleMethodNotAllowed(HttpExchange exchange) throws IOException {
    sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
  }

  /**
   * Parsea la parte de query de una URL en un Map simple.
   * Formato esperado: key1=val1&key2=val2
   */
  protected Map<String, String> parseQuery(String query) {
    Map<String, String> params = new HashMap<>();
    if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          params.put(keyValue[0], keyValue[1]);
        }
      }
    }
    return params;
  }
}