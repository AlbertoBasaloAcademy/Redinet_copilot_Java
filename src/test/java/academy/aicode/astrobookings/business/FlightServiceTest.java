package academy.aicode.astrobookings.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.FlightRepository;
import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;

class FlightServiceTest {

  private FlightService flightService;

  @BeforeEach
  void setUp() {
    clearFlightRepository();
    clearBookingRepository();
    clearRocketRepository();
    flightService = new FlightService();
  }

  @Test
  void create_whenRequestIsNull_throwsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(null));
    assertTrue(ex.getMessage().contains("Request body must be provided"));
  }

  @Test
  void create_whenRocketIdIsBlank_throwsIllegalArgumentException() {
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId("   ");
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("rocketId must be provided"));
  }

  @Test
  void create_whenRocketDoesNotExist_throwsIllegalArgumentException() {
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId("missing");
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("rocketId does not exist"));
  }

  @Test
  void create_whenLaunchDateTimeIsNull_throwsIllegalArgumentException() {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(null);
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("launchDateTime must be provided"));
  }

  @Test
  void create_whenLaunchDateTimeIsNotInFuture_throwsIllegalArgumentException() {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().minusSeconds(60));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("launchDateTime must be in the future"));
  }

  @ParameterizedTest
  @ValueSource(doubles = { 0.0d, -1.0d })
  void create_whenBasePriceIsNotGreaterThanZero_throwsIllegalArgumentException(double basePrice) {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(basePrice);
    req.setMinimumPassengers(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("basePrice must be greater than 0"));
  }

  @Test
  void create_whenMinimumPassengersIsNull_throwsIllegalArgumentException() {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("minimumPassengers must be between 1 and rocket capacity"));
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 6 })
  void create_whenMinimumPassengersIsOutOfRange_throwsIllegalArgumentException(int minimumPassengers) {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocket.getId());
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(minimumPassengers);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.create(req));
    assertTrue(ex.getMessage().contains("minimumPassengers must be between 1 and rocket capacity"));
  }

  @Test
  void create_whenRequestIsValid_trimsRocketId_generatesId_andSetsScheduledState() {
    Rocket rocket = seedRocket(5);

    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId("  " + rocket.getId() + "  ");
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(3);

    Flight saved = flightService.create(req);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(rocket.getId(), saved.getRocketId());
    assertEquals(FlightState.SCHEDULED, saved.getState());
  }

  @Test
  void refreshStateOnRead_whenLaunchDateTimeIsInPast_setsDone() {
    FlightRepository repo = new FlightRepository();
    Flight flight = new Flight();
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().minusSeconds(3600));
    flight.setBasePrice(1000.0);
    flight.setMinimumPassengers(1);
    flight.setState(FlightState.SCHEDULED);

    Flight saved = repo.save(flight);

    Flight found = flightService.findById(saved.getId());

    assertNotNull(found);
    assertEquals(FlightState.DONE, found.getState());
  }

  @Test
  void refreshStateOnRead_whenCancelled_doesNotChange() {
    Flight flight = new Flight();
    flight.setId("f-1");
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().minusSeconds(3600));
    flight.setState(FlightState.CANCELLED);

    flightService.refreshStateOnRead(flight);

    assertEquals(FlightState.CANCELLED, flight.getState());
  }

  @Test
  void refreshStateOnRead_whenBookingsReachMinimumPassengers_setsConfirmed() {
    Rocket rocket = seedRocket(2);
    Flight flight = createFutureFlight(rocket.getId(), 1);

    saveBookingForFlight(flight.getId());

    flightService.refreshStateOnRead(flight);

    assertEquals(FlightState.CONFIRMED, flight.getState());
  }

  @Test
  void refreshStateOnRead_whenBookingsReachCapacity_setsSoldOut() {
    Rocket rocket = seedRocket(2);
    Flight flight = createFutureFlight(rocket.getId(), 1);

    saveBookingForFlight(flight.getId());
    saveBookingForFlight(flight.getId());

    flightService.refreshStateOnRead(flight);

    assertEquals(FlightState.SOLD_OUT, flight.getState());
  }

  @Test
  void cancelById_whenIdIsBlank_throwsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> flightService.cancelById("   "));
    assertTrue(ex.getMessage().contains("id must be provided"));
  }

  @Test
  void cancelById_whenFlightDoesNotExist_returnsNull() {
    Flight cancelled = flightService.cancelById("missing-id");
    assertNull(cancelled);
  }

  @Test
  void cancelById_whenFlightExistsAndNotDone_setsCancelled_andIsIdempotent() {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 1);

    Flight cancelled1 = flightService.cancelById(flight.getId());
    Flight cancelled2 = flightService.cancelById(flight.getId());

    assertNotNull(cancelled1);
    assertEquals(flight.getId(), cancelled1.getId());
    assertEquals(FlightState.CANCELLED, cancelled1.getState());
    assertNotNull(cancelled2);
    assertEquals(FlightState.CANCELLED, cancelled2.getState());

    assertEquals(1, flightService.findFutureFlights(FlightState.CANCELLED).size());
  }

  @Test
  void cancelById_whenFlightIsDone_throwsIllegalStateException() {
    FlightRepository repo = new FlightRepository();
    Flight flight = new Flight();
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().minusSeconds(3600));
    flight.setBasePrice(1000.0);
    flight.setMinimumPassengers(1);
    flight.setState(FlightState.SCHEDULED);

    Flight saved = repo.save(flight);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> flightService.cancelById(saved.getId()));
    assertTrue(ex.getMessage().contains("DONE"));
  }

  private static Rocket seedRocket(int capacity) {
    RocketService rocketService = new RocketService();
    CreateRocketRequest rocketRequest = new CreateRocketRequest();
    rocketRequest.setName("Falcon");
    rocketRequest.setCapacity(capacity);
    return rocketService.create(rocketRequest);
  }

  private Flight createFutureFlight(String rocketId, int minimumPassengers) {
    CreateFlightRequest req = new CreateFlightRequest();
    req.setRocketId(rocketId);
    req.setLaunchDateTime(Instant.now().plusSeconds(3600));
    req.setBasePrice(1000.0);
    req.setMinimumPassengers(minimumPassengers);
    return flightService.create(req);
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
}
