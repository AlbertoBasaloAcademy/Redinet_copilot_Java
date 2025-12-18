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

import academy.aicode.astrobookings.business.FlightService;
import academy.aicode.astrobookings.business.RocketService;
import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.FlightRepository;
import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;
import academy.aicode.astrobookings.presentation.dto.ErrorResponse;
import academy.aicode.astrobookings.presentation.dto.FlightResponse;

class FlightHandlerTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private FlightHandler handler;

  @BeforeEach
  void setUp() {
    clearFlightRepository();
    clearBookingRepository();
    clearRocketRepository();
    handler = new FlightHandler();
  }

  @Test
  void post_whenPayloadIsValid_returns201_withGeneratedId_andScheduledState() throws IOException {
    Rocket rocket = seedRocket(10);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId("  " + rocket.getId() + "  ");
    req.setLaunchDateTime(Instant.parse("2099-01-15T10:00:00Z"));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(3);

    String json = objectMapper.writeValueAsString(req);
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights"), "/flights", json);

    handler.handle(exchange);

    assertEquals(201, exchange.getStatusCode());
    FlightResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse.class);
    assertNotNull(response.getId());
    assertEquals(rocket.getId(), response.getRocketId());
    assertEquals(Instant.parse("2099-01-15T10:00:00Z"), response.getLaunchDateTime());
    assertEquals(1000.0, response.getBasePrice());
    assertEquals(3, response.getMinimumPassengers());
    assertEquals(FlightState.SCHEDULED, response.getState());
  }

  @Test
  void post_whenRocketIdDoesNotExist_returns400_invalidInput() throws IOException {
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId("missing");
    req.setLaunchDateTime(Instant.parse("2099-01-15T10:00:00Z"));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);

    String json = objectMapper.writeValueAsString(req);
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights"), "/flights", json);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_INPUT", response.getCode());
  }

  @Test
  void post_whenJsonIsInvalid_returns400_invalidJson() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights"), "/flights", "not-json");

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_JSON", response.getCode());
  }

  @Test
  void get_whenListingFutureFlights_returnsOnlyFutureFlights() throws IOException {
    Rocket rocket = seedRocket(10);

    FlightService seed = new FlightService();
    CreateFlightRequest future = new CreateFlightRequest();
    future.setRocketId(rocket.getId());
    future.setLaunchDateTime(Instant.now().plusSeconds(3600));
    future.setBasePrice(1000.0);
    future.setMinimumPassengers(1);
    Flight savedFuture = seed.create(future);

    Flight pastFlight = new Flight();
    pastFlight.setRocketId(rocket.getId());
    pastFlight.setLaunchDateTime(Instant.now().minusSeconds(3600));
    pastFlight.setBasePrice(1000.0);
    pastFlight.setMinimumPassengers(1);
    pastFlight.setState(FlightState.SCHEDULED);
    new FlightRepository().save(pastFlight);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights"), "/flights", null);

    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    FlightResponse[] responses = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse[].class);
    assertEquals(1, responses.length);
    assertEquals(savedFuture.getId(), responses[0].getId());
  }

  @Test
  void get_whenFilteringByState_returnsFilteredArray() throws IOException {
    Rocket rocket = seedRocket(2);

    FlightService seed = new FlightService();
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);
    Flight flight = seed.create(req);

    saveBookingForFlight(flight.getId());

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights?state=CONFIRMED"), "/flights",
        null);
    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    FlightResponse[] responses = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse[].class);
    assertEquals(1, responses.length);
    assertEquals(FlightState.CONFIRMED, responses[0].getState());
  }

  @Test
  void get_whenStateQueryIsInvalid_returns400_invalidQuery() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights?state=BAD"), "/flights", null);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_QUERY", response.getCode());
  }

  @Test
  void get_whenExistingId_returns200_withFlight() throws IOException {
    Rocket rocket = seedRocket(10);

    FlightService seed = new FlightService();
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);
    Flight saved = seed.create(req);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights/" + saved.getId()), "/flights",
        null);

    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    FlightResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse.class);
    assertEquals(saved.getId(), response.getId());
  }

  @Test
  void get_whenIdDoesNotExist_returns404_notFound() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights/missing-id"), "/flights", null);

    handler.handle(exchange);

    assertEquals(404, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("NOT_FOUND", response.getCode());
  }

  @Test
  void unsupportedMethod_returns405() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("DELETE", URI.create("/flights"), "/flights", null);

    handler.handle(exchange);

    assertEquals(405, exchange.getStatusCode());
    assertTrue(exchange.getResponseBodyAsString().contains("Method not allowed"));
  }

  @Test
  void postCancel_whenFlightExistsAndNotDone_returns200_withCancelledState() throws IOException {
    Rocket rocket = seedRocket(10);

    FlightService seed = new FlightService();
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.parse("2099-01-15T10:00:00Z"));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);
    Flight saved = seed.create(req);

    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights/" + saved.getId() + "/cancel"),
        "/flights", null);

    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    FlightResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse.class);
    assertEquals(saved.getId(), response.getId());
    assertEquals(FlightState.CANCELLED, response.getState());
  }

  @Test
  void postCancel_whenFlightDoesNotExist_returns404_notFound() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights/missing-id/cancel"), "/flights",
        null);

    handler.handle(exchange);

    assertEquals(404, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("NOT_FOUND", response.getCode());
  }

  @Test
  void postCancel_whenIdIsBlank_returns400_invalidId() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights//cancel"), "/flights", null);

    handler.handle(exchange);

    assertEquals(400, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("INVALID_ID", response.getCode());
  }

  @Test
  void postCancel_whenFlightIsDone_returns409_conflict() throws IOException {
    Flight flight = new Flight();
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().minusSeconds(3600));
    flight.setBasePrice(1000.0);
    flight.setMinimumPassengers(1);
    flight.setState(FlightState.SCHEDULED);
    Flight saved = new FlightRepository().save(flight);

    FakeHttpExchange exchange = FakeHttpExchange.create("POST", URI.create("/flights/" + saved.getId() + "/cancel"),
        "/flights", null);

    handler.handle(exchange);

    assertEquals(409, exchange.getStatusCode());
    ErrorResponse response = objectMapper.readValue(exchange.getResponseBodyAsString(), ErrorResponse.class);
    assertEquals("CONFLICT", response.getCode());
  }

  @Test
  void get_whenFilteringByStateCancelled_returnsOnlyFutureCancelledFlights() throws IOException {
    Rocket rocket = seedRocket(10);

    FlightService seed = new FlightService();

    CreateFlightRequest futureReq = new CreateFlightRequest();
    futureReq.setRocketId(rocket.getId());
    futureReq.setLaunchDateTime(Instant.now().plusSeconds(3600));
    futureReq.setBasePrice(1000.0);
    futureReq.setMinimumPassengers(1);
    Flight future = seed.create(futureReq);
    seed.cancelById(future.getId());

    Flight pastCancelled = new Flight();
    pastCancelled.setRocketId(rocket.getId());
    pastCancelled.setLaunchDateTime(Instant.now().minusSeconds(3600));
    pastCancelled.setBasePrice(1000.0);
    pastCancelled.setMinimumPassengers(1);
    pastCancelled.setState(FlightState.CANCELLED);
    new FlightRepository().save(pastCancelled);

    FakeHttpExchange exchange = FakeHttpExchange.create("GET", URI.create("/flights?state=CANCELLED"), "/flights",
        null);
    handler.handle(exchange);

    assertEquals(200, exchange.getStatusCode());
    FlightResponse[] responses = objectMapper.readValue(exchange.getResponseBodyAsString(), FlightResponse[].class);
    assertEquals(1, responses.length);
    assertEquals(future.getId(), responses[0].getId());
    assertEquals(FlightState.CANCELLED, responses[0].getState());
  }

  @Test
  void cancelEndpoint_whenMethodIsUnsupported_returns405() throws IOException {
    FakeHttpExchange exchange = FakeHttpExchange.create("PUT", URI.create("/flights/some-id/cancel"), "/flights",
        null);

    handler.handle(exchange);

    assertEquals(405, exchange.getStatusCode());
    assertTrue(exchange.getResponseBodyAsString().contains("Method not allowed"));
  }

  private static Rocket seedRocket(int capacity) {
    RocketService rocketService = new RocketService();
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("Falcon");
    req.setCapacity(capacity);
    return rocketService.create(req);
  }

  private static void saveBookingForFlight(String flightId) {
    BookingRepository repo = new BookingRepository();
    Booking booking = new Booking();
    booking.setFlightId(flightId);
    booking.setPassengerName("Ada");
    booking.setPassengerDocument("DOC");
    booking.setFinalPrice(1000.0);
    repo.save(booking);
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
