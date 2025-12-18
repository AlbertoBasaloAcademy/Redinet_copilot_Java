package academy.aicode.astrobookings.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.FlightRepository;
import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateBookingRequest;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;

class BookingServiceTest {

  private BookingService bookingService;

  @BeforeEach
  void setUp() {
    clearBookingRepository();
    clearFlightRepository();
    clearRocketRepository();
    bookingService = new BookingService();
  }

  @Test
  void create_whenRequestIsNull_throwsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.create(null));
    assertTrue(ex.getMessage().contains("Request body must be provided"));
  }

  @Test
  void create_whenFlightIdIsBlank_throwsIllegalArgumentException() {
    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId("   ");
    req.setPassengerName("Ada");
    req.setPassengerDocument("P123");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.create(req));
    assertTrue(ex.getMessage().contains("flightId must be provided"));
  }

  @Test
  void create_whenPassengerNameIsBlank_throwsIllegalArgumentException() {
    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId("f-1");
    req.setPassengerName("   ");
    req.setPassengerDocument("P123");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.create(req));
    assertTrue(ex.getMessage().contains("passengerName must be provided"));
  }

  @Test
  void create_whenPassengerDocumentIsBlank_throwsIllegalArgumentException() {
    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId("f-1");
    req.setPassengerName("Ada");
    req.setPassengerDocument("   ");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.create(req));
    assertTrue(ex.getMessage().contains("passengerDocument must be provided"));
  }

  @Test
  void create_whenFlightDoesNotExist_throwsIllegalArgumentException() {
    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId("missing");
    req.setPassengerName("Ada");
    req.setPassengerDocument("P123");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.create(req));
    assertTrue(ex.getMessage().contains("flightId does not exist"));
  }

  @Test
  void create_whenFlightIsCancelled_throwsBookingConflictException() {
    Rocket rocket = seedRocket(2);
    FlightRepository flightRepository = new FlightRepository();

    Flight cancelled = new Flight();
    cancelled.setRocketId(rocket.getId());
    cancelled.setLaunchDateTime(Instant.now().plusSeconds(3600));
    cancelled.setBasePrice(1000.0);
    cancelled.setMinimumPassengers(1);
    cancelled.setState(FlightState.CANCELLED);
    Flight saved = flightRepository.save(cancelled);

    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId(saved.getId());
    req.setPassengerName("Ada");
    req.setPassengerDocument("P123");

    BookingConflictException ex = assertThrows(BookingConflictException.class, () -> bookingService.create(req));
    assertTrue(ex.getMessage().contains("not eligible"));
  }

  @Test
  void create_whenFlightIsSoldOut_throwsBookingConflictException() {
    Rocket rocket = seedRocket(1);
    Flight flight = createFutureFlight(rocket.getId(), 1, 1000.0);

    CreateBookingRequest first = new CreateBookingRequest();
    first.setFlightId(flight.getId());
    first.setPassengerName("Ada");
    first.setPassengerDocument("P123");
    bookingService.create(first);

    CreateBookingRequest second = new CreateBookingRequest();
    second.setFlightId(flight.getId());
    second.setPassengerName("Grace");
    second.setPassengerDocument("P456");

    BookingConflictException ex = assertThrows(BookingConflictException.class, () -> bookingService.create(second));
    assertTrue(ex.getMessage().contains("sold out") || ex.getMessage().contains("not eligible"));
  }

  @Test
  void create_whenRequestIsValid_generatesId_setsCreatedAt_andComputesFinalPriceAndDiscount() {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 2, 1000.0);

    CreateBookingRequest req = new CreateBookingRequest();
    req.setFlightId("  " + flight.getId() + "  ");
    req.setPassengerName("  Ada Lovelace  ");
    req.setPassengerDocument("  P123456  ");

    Booking created = bookingService.create(req);

    assertNotNull(created);
    assertNotNull(created.getId());
    assertNotNull(created.getCreatedAt());
    assertEquals(flight.getId(), created.getFlightId());
    assertEquals("Ada Lovelace", created.getPassengerName());
    assertEquals("P123456", created.getPassengerDocument());
    assertEquals(10, created.getDiscountPercent());
    assertEquals(900.0, created.getFinalPrice());
  }

  @Test
  void create_whenBookingReachesMinimumPassengers_applies30PercentDiscount_andRefreshesFlightToConfirmed() {
    Rocket rocket = seedRocket(5);
    Flight flight = createFutureFlight(rocket.getId(), 2, 1000.0);

    CreateBookingRequest first = new CreateBookingRequest();
    first.setFlightId(flight.getId());
    first.setPassengerName("Ada");
    first.setPassengerDocument("P1");
    bookingService.create(first);

    CreateBookingRequest second = new CreateBookingRequest();
    second.setFlightId(flight.getId());
    second.setPassengerName("Grace");
    second.setPassengerDocument("P2");
    Booking createdSecond = bookingService.create(second);

    assertEquals(30, createdSecond.getDiscountPercent());
    assertEquals(700.0, createdSecond.getFinalPrice());

    Flight refreshed = new FlightService().findById(flight.getId());
    assertNotNull(refreshed);
    assertEquals(FlightState.CONFIRMED, refreshed.getState());
  }

  @Test
  void create_whenLastSeatBooking_applies0PercentDiscount_andRefreshesFlightToSoldOut() {
    Rocket rocket = seedRocket(3);
    Flight flight = createFutureFlight(rocket.getId(), 2, 1000.0);

    CreateBookingRequest b1 = new CreateBookingRequest();
    b1.setFlightId(flight.getId());
    b1.setPassengerName("Ada");
    b1.setPassengerDocument("P1");
    bookingService.create(b1);

    CreateBookingRequest b2 = new CreateBookingRequest();
    b2.setFlightId(flight.getId());
    b2.setPassengerName("Grace");
    b2.setPassengerDocument("P2");
    bookingService.create(b2);

    CreateBookingRequest b3 = new CreateBookingRequest();
    b3.setFlightId(flight.getId());
    b3.setPassengerName("Katherine");
    b3.setPassengerDocument("P3");
    Booking last = bookingService.create(b3);

    assertEquals(0, last.getDiscountPercent());
    assertEquals(1000.0, last.getFinalPrice());

    Flight refreshed = new FlightService().findById(flight.getId());
    assertNotNull(refreshed);
    assertEquals(FlightState.SOLD_OUT, refreshed.getState());
  }

  @Test
  void computeDiscountPercent_whenBookingIsLastSeat_returns0() {
    assertEquals(0, bookingService.computeDiscountPercent(3, 3, 2));
  }

  @Test
  void computeDiscountPercent_whenBookingReachesMinimumPassengers_returns30() {
    assertEquals(30, bookingService.computeDiscountPercent(2, 5, 2));
  }

  @Test
  void computeDiscountPercent_whenOtherBooking_returns10() {
    assertEquals(10, bookingService.computeDiscountPercent(1, 5, 2));
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
}
