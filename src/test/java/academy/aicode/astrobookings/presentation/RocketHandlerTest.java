package academy.aicode.astrobookings.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;

import academy.aicode.astrobookings.business.RocketService;
import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Range;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;
import academy.aicode.astrobookings.presentation.dto.RocketResponse;

class RocketHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private RocketHandler handler;

  @BeforeEach
  void setUp() {
    clearRocketRepository();
    handler = new RocketHandler();
  }

  @Test
  void post_whenPayloadIsValid_returns201_withGeneratedId() throws IOException {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("  Falcon  ");
    req.setCapacity(10);
    req.setRange(Range.LEO);
    req.setSpeed(7.8);

    String json = objectMapper.writeValueAsString(req);
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/rockets"), "/rockets", json);

    handler.handle(exchange);

    assertEquals(201, exchange.getStatusCode());
    RocketResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), RocketResponse.class);
    assertNotNull(response.getId());
    assertEquals("Falcon", response.getName());
    assertEquals(10, response.getCapacity());
    assertEquals(Range.LEO, response.getRange());
    assertEquals(7.8, response.getSpeed());
  }

  @Test
  void post_whenJsonIsInvalid_returns400_invalidJson() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/rockets"), "/rockets", "not-json");

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_JSON", response.getCode());
  }

  @Test
  void post_whenRangeIsInvalid_returns400_invalidJson() throws IOException {
    String json = "{\"name\":\"Falcon\",\"capacity\":10,\"range\":\"BAD\"}";
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/rockets"), "/rockets", json);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_JSON", response.getCode());
  }

  @Test
  void get_whenListingWithNameFilter_returnsFilteredArray() throws IOException {
    RocketService seed = new RocketService();
    CreateRocketRequest r1 = new CreateRocketRequest();
    r1.setName("Falcon");
    r1.setCapacity(10);
    seed.create(r1);

    CreateRocketRequest r2 = new CreateRocketRequest();
    r2.setName("Starship");
    r2.setCapacity(10);
    seed.create(r2);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/rockets?name=con"), "/rockets", null);
    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    RocketResponse[] responses = objectMapper.readValue(exchange.getResponseBodyAsString(), RocketResponse[].class);
    assertEquals(1, responses.length);
    assertEquals("Falcon", responses[0].getName());
  }

  @Test
  void get_whenExistingId_returns200_withRocket() throws IOException {
    RocketService seed = new RocketService();
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("Falcon");
    req.setCapacity(10);
    Rocket saved = seed.create(req);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/rockets/" + saved.getId()), "/rockets",
        null);
    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    RocketResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), RocketResponse.class);
    assertEquals(saved.getId(), response.getId());
  }

  @Test
  void get_whenIdDoesNotExist_returns404_notFound() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/rockets/missing-id"), "/rockets", null);
    handler.handle(exchange);

    assertEquals(404, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("NOT_FOUND", response.getCode());
  }

  @Test
  void unsupportedMethod_returns405() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("DELETE", URI.create("/rockets"), "/rockets", null);
    handler.handle(exchange);

    assertEquals(405, exchange.getStatusCode());
    assertTrue(exchange.getResponseBodyAsString().contains("Method not allowed"));
  }

  private static void clearRocketRepository() {
    try {
      Field field = RocketRepository.class.getDeclaredField("rockets");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Rocket> rockets = (Map<String, Rocket>) field.get(null);
      rockets.clear();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final class FakeHttpExchange extends HttpExchange {

    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final URI requestUri;
    private final String method;
    private final HttpContext httpContext;
    private final InputStream requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private int statusCode;

    private FakeHttpExchange(String method, URI requestUri, String contextPath, String body) {
      this.method = method;
      this.requestUri = requestUri;
      this.httpContext = new FakeHttpContext(contextPath);
      this.requestBody = body == null ? InputStream.nullInputStream()
          : new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    static FakeHttpExchange create(String method, URI requestUri, String contextPath, String body) {
      return new FakeHttpExchange(method, requestUri, contextPath, body);
    }

    int getStatusCode() {
      return statusCode;
    }

    String getResponseBodyAsString() {
      return responseBody.toString(StandardCharsets.UTF_8);
    }

    @Override
    public Headers getRequestHeaders() {
      return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
      return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
      return requestUri;
    }

    @Override
    public String getRequestMethod() {
      return method;
    }

    @Override
    public HttpContext getHttpContext() {
      return httpContext;
    }

    @Override
    public void close() {
    }

    @Override
    public InputStream getRequestBody() {
      return requestBody;
    }

    @Override
    public OutputStream getResponseBody() {
      return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) {
      this.statusCode = rCode;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public int getResponseCode() {
      return statusCode;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return new InetSocketAddress(0);
    }

    @Override
    public String getProtocol() {
      return "HTTP/1.1";
    }

    @Override
    public Object getAttribute(String name) {
      return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
    }

    @Override
    public void setStreams(InputStream i, OutputStream o) {
    }

    @Override
    public HttpPrincipal getPrincipal() {
      return null;
    }
  }

  private static final class FakeHttpContext extends HttpContext {

    private final String path;

    private FakeHttpContext(String path) {
      this.path = path;
    }

    @Override
    public HttpHandler getHandler() {
      return null;
    }

    @Override
    public void setHandler(HttpHandler handler) {
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public HttpServer getServer() {
      return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return new HashMap<>();
    }

    @Override
    public List<com.sun.net.httpserver.Filter> getFilters() {
      return Collections.emptyList();
    }

    @Override
    public com.sun.net.httpserver.Authenticator getAuthenticator() {
      return null;
    }

    @Override
    public com.sun.net.httpserver.Authenticator setAuthenticator(com.sun.net.httpserver.Authenticator auth) {
      return null;
    }
  }
}
