package academy.aicode.astrobookings.business;

import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.models.Booking;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateBookingRequest;

/**
 * Business service for Booking operations: validation, pricing rules, and
 * persistence orchestration.
 */
public class BookingService {

  private static final Logger LOGGER = Logger.getLogger(BookingService.class.getName());

  private final BookingRepository bookingRepository = new BookingRepository();
  private final FlightService flightService = new FlightService();
  private final RocketService rocketService = new RocketService();

  /**
   * Creates a booking after validating the request and computing the final price.
   *
   * @param request the create booking request
   * @return the created booking
   */
  public Booking create(CreateBookingRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body must be provided");
    }

    String flightId = request.getFlightId() == null ? null : request.getFlightId().trim();
    if (flightId == null || flightId.isEmpty()) {
      throw new IllegalArgumentException("flightId must be provided");
    }

    String passengerName = request.getPassengerName() == null ? null : request.getPassengerName().trim();
    if (passengerName == null || passengerName.isEmpty()) {
      throw new IllegalArgumentException("passengerName must be provided");
    }

    String passengerDocument = request.getPassengerDocument() == null ? null : request.getPassengerDocument().trim();
    if (passengerDocument == null || passengerDocument.isEmpty()) {
      throw new IllegalArgumentException("passengerDocument must be provided");
    }

    Flight flight = flightService.findById(flightId);
    if (flight == null) {
      throw new IllegalArgumentException("flightId does not exist");
    }

    if (flight.getState() == FlightState.CANCELLED || flight.getState() == FlightState.SOLD_OUT) {
      throw new BookingConflictException("flight is not eligible for booking");
    }

    Rocket rocket = rocketService.findById(flight.getRocketId());
    if (rocket == null || rocket.getCapacity() == null || rocket.getCapacity() < 1) {
      throw new IllegalArgumentException("rocket capacity is invalid");
    }

    int capacity = rocket.getCapacity();
    int currentBookings = bookingRepository.countByFlightId(flightId);

    if (currentBookings >= capacity) {
      throw new BookingConflictException("flight is sold out");
    }

    int bookingNumber = currentBookings + 1;
    int discountPercent = computeDiscountPercent(bookingNumber, capacity, flight.getMinimumPassengers());

    Double basePrice = flight.getBasePrice();
    if (basePrice == null || !(basePrice > 0.0d)) {
      throw new IllegalArgumentException("flight basePrice is invalid");
    }

    double finalPrice = basePrice.doubleValue() * (100.0d - discountPercent) / 100.0d;

    Booking booking = new Booking();
    booking.setFlightId(flightId);
    booking.setPassengerName(passengerName);
    booking.setPassengerDocument(passengerDocument);
    booking.setDiscountPercent(discountPercent);
    booking.setFinalPrice(finalPrice);
    booking.setCreatedAt(Instant.now());

    Booking saved = bookingRepository.save(booking);
    LOGGER.log(Level.INFO, "Booking created: {0}", saved.getId());

    flightService.refreshStateOnRead(flight);
    return saved;
  }

  /**
   * Returns a booking by id.
   *
   * @param id booking id
   * @return booking or null
   */
  public Booking findById(String id) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("id must be provided");
    }
    return bookingRepository.findById(id.trim());
  }

  /**
   * Lists bookings for a flight.
   *
   * @param flightId flight id
   * @return list of bookings (may be empty)
   */
  public List<Booking> findByFlightId(String flightId) {
    if (flightId == null || flightId.trim().isEmpty()) {
      throw new IllegalArgumentException("flightId must be provided");
    }

    Flight flight = flightService.findById(flightId.trim());
    if (flight == null) {
      throw new IllegalArgumentException("flightId does not exist");
    }

    return bookingRepository.findByFlightId(flightId.trim());
  }

  /**
   * Computes the discount percent for the booking.
   */
  public int computeDiscountPercent(int bookingNumber, int capacity, Integer minimumPassengers) {
    if (bookingNumber == capacity) {
      return 0;
    }

    if (minimumPassengers != null && bookingNumber == minimumPassengers.intValue()) {
      return 30;
    }

    return 10;
  }
}
