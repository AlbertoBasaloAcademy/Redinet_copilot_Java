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
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpServer;

import academy.aicode.astrobookings.business.BookingService;
import academy.aicode.astrobookings.business.FlightService;
import academy.aicode.astrobookings.business.RocketService;
import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.FlightRepository;
import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.BookingResponse;
import academy.aicode.astrobookings.presentation.dto.CreateBookingRequest;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;

class BookingHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private BookingHandler handler;

  @BeforeEach
  void setUp() {
    clearBookingRepository();
    clearFlightRepository();
    clearRocketRepository();
    handler = new BookingHandler();
  }

  @Test
  void post_whenPayloadIsValid_returns201_withGeneratedId_andComputedFields() throws IOException {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 2, 1000.0);

    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId(flight.getId());
    req.setPassengerName("Ada Lovelace");
    req.setPassengerDocument("P123456");

    String json = objectMapper.writeValueAsString(req);
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/bookings"), "/bookings", json);

    handler.handle(exchange);

    assertEquals(201, exchange.getStatusCode());
    BookingResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), BookingResponse.class);
    assertNotNull(response.getId());
    assertEquals(flight.getId(), response.getFlightId());
    assertEquals("Ada Lovelace", response.getPassengerName());
    assertEquals("P123456", response.getPassengerDocument());
    assertNotNull(response.getCreatedAt());
    assertEquals(10, response.getDiscountPercent());
    assertEquals(900.0, response.getFinalPrice());
  }

  @Test
  void post_whenPassengerNameIsMissing_returns400_invalidInput() throws IOException {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 2, 1000.0);

    String json = "{\"flightId\":\"" + flight.getId() + "\",\"passengerDocument\":\"P123\"}";
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/bookings"), "/bookings", json);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_INPUT", response.getCode());
  }

  @Test
  void post_whenFlightIsSoldOut_returns409_conflict() throws IOException {
    Rocket rocket = seedRocket(1);
    Flight flight = createFutureFlight(rocket.getId(), 1, 1000.0);

    BookingService seed = new BookingService();
    CreateBookingRequest first = new CreateBookingRequest();
    first.setFlightId(flight.getId());
    first.setPassengerName("Ada");
    first.setPassengerDocument("P1");
    seed.create(first);

    CreateBookingRequest second = new CreateBookingRequest();
    second.setFlightId(flight.getId());
    second.setPassengerName("Grace");
    second.setPassengerDocument("P2");

    String json = objectMapper.writeValueAsString(second);
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/bookings"), "/bookings", json);

    handler.handle(exchange);

    assertEquals(409, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("CONFLICT", response.getCode());
  }

  @Test
  void get_whenOmittingFlightIdQuery_returns400_invalidQuery() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/bookings"), "/bookings", null);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_QUERY", response.getCode());
  }

  @Test
  void get_whenListingByFlightId_returns200_withArray() throws IOException {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 1, 1000.0);

    BookingService seed = new BookingService();

    CreateBookingRequest b1 = new CreateBookingRequest();
    b1.setFlightId(flight.getId());
    b1.setPassengerName("Ada");
    b1.setPassengerDocument("P1");
    seed.create(b1);

    CreateBookingRequest b2 = new CreateBookingRequest();
    b2.setFlightId(flight.getId());
    b2.setPassengerName("Grace");
    b2.setPassengerDocument("P2");
    seed.create(b2);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/bookings?flightId=" + flight.getId()),
        "/bookings", null);

    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    BookingResponse[] responses = objectMapper.readValue(exchange.getResponseBodyAsString(), BookingResponse[].class);
    assertEquals(2, responses.length);
    assertEquals(flight.getId(), responses[0].getFlightId());
    assertEquals(flight.getId(), responses[1].getFlightId());
  }

  @Test
  void get_whenExistingId_returns200_withBooking() throws IOException {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 1, 1000.0);

    BookingService seed = new BookingService();
    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId(flight.getId());
    req.setPassengerName("Ada");
    req.setPassengerDocument("P1");
    Booking created = seed.create(req);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/bookings/" + created.getId()),
        "/bookings", null);

    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    BookingResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), BookingResponse.class);
    assertEquals(created.getId(), response.getId());
    assertEquals(flight.getId(), response.getFlightId());
  }

  @Test
  void get_whenIdDoesNotExist_returns404_notFound() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/bookings/missing-id"), "/bookings",
        null);

    handler.handle(exchange);

    assertEquals(404, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("NOT_FOUND", response.getCode());
  }

  @Test
  void unsupportedMethod_returns405() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("DELETE", URI.create("/bookings"), "/bookings", null);

    handler.handle(exchange);

    assertEquals(405, exchange.getStatusCode());
    assertTrue(exchange.getResponseBodyAsString().contains("Method not allowed"));
  }

  private static Rocket seedRocket(int capacity) {
    RocketService rocketService = new RocketService();
    CreateRocketRequest rocketRequest = new CreateRocketRequest();
    rocketRequest.setName("Falcon");
    rocketRequest.setCapacity(capacity);
    return rocketService.create(rocketRequest);
  }

  private static Flight createFutureFlight(String rocketId, int minimumPassengers, double basePrice) {
    FlightService flightService = new FlightService();
    CreateFlightRequest flightRequest = new CreateFlightRequest();
    flightRequest.setRocketId(rocketId);
    flightRequest.setLaunchDateTime(Instant.now().plusSeconds(3600));
    flightRequest.setBasePrice(basePrice);
    flightRequest.setMinimumPassengers(minimumPassengers);
    return flightService.create(flightRequest);
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

  private static void clearFlightRepository() {
    try {
      Field field = FlightRepository.class.getDeclaredField("flights");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Flight> flights = (Map<String, Flight>) field.get(null);
      flights.clear();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void clearBookingRepository() {
    try {
      Field field = BookingRepository.class.getDeclaredField("bookings");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Booking> bookings = (Map<String, Booking>) field.get(null);
      bookings.clear();
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
