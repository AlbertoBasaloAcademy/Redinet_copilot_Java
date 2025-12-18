package academy.aicode.astrobookings.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import academy.aicode.astrobookings.persistence.BookingRepository;
import academy.aicode.astrobookings.persistence.FlightRepository;
import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateFlightRequest;

/**
 * Business service for Flight operations (validation and state derivation).
 */
public class FlightService {

  private static final Logger LOGGER = Logger.getLogger(FlightService.class.getName());

  private final FlightRepository flightRepository = new FlightRepository();
  private final BookingRepository bookingRepository = new BookingRepository();
  private final RocketService rocketService = new RocketService();

  /**
   * Creates a flight after validating the request.
   *
   * @param request the create flight request
   * @return the created flight
   */
  public Flight create(CreateFlightRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body must be provided");
    }

    String rocketId = request.getRocketId() == null ? null : request.getRocketId().trim();
    if (rocketId == null || rocketId.isEmpty()) {
      throw new IllegalArgumentException("rocketId must be provided");
    }

    Rocket rocket = rocketService.findById(rocketId);
    if (rocket == null) {
      throw new IllegalArgumentException("rocketId does not exist");
    }

    Instant launchDateTime = request.getLaunchDateTime();
    if (launchDateTime == null) {
      throw new IllegalArgumentException("launchDateTime must be provided");
    }

    Instant now = Instant.now();
    if (!launchDateTime.isAfter(now)) {
      throw new IllegalArgumentException("launchDateTime must be in the future");
    }

    Double basePrice = request.getBasePrice();
    if (basePrice == null || !(basePrice > 0.0d)) {
      throw new IllegalArgumentException("basePrice must be greater than 0");
    }

    Integer capacity = rocket.getCapacity();
    if (capacity == null || capacity < 1) {
      throw new IllegalArgumentException("rocket capacity is invalid");
    }

    Integer minimumPassengers = request.getMinimumPassengers();
    if (minimumPassengers == null || minimumPassengers < 1 || minimumPassengers > capacity) {
      throw new IllegalArgumentException("minimumPassengers must be between 1 and rocket capacity");
    }

    Flight flight = new Flight();
    flight.setRocketId(rocketId);
    flight.setLaunchDateTime(launchDateTime);
    flight.setBasePrice(basePrice);
    flight.setMinimumPassengers(minimumPassengers);
    flight.setState(FlightState.SCHEDULED);

    Flight saved = flightRepository.save(flight);
    LOGGER.log(Level.INFO, "Flight created: {0}", saved.getId());
    return saved;
  }

  /**
   * Returns a flight by id, refreshing its derived state on read.
   *
   * @param id the flight id
   * @return the flight or null
   */
  public Flight findById(String id) {
    Flight flight = flightRepository.findById(id);
    if (flight == null) {
      return null;
    }

    refreshStateOnRead(flight);
    return flight;
  }

  /**
   * Lists future flights (launchDateTime after now), optionally filtering by
   * state.
   *
   * @param stateFilter optional state filter
   * @return list of future flights
   */
  public List<Flight> findFutureFlights(FlightState stateFilter) {
    Instant now = Instant.now();
    List<Flight> out = new ArrayList<>();

    for (Flight flight : flightRepository.findAll()) {
      refreshStateOnRead(flight);

      Instant launchDateTime = flight.getLaunchDateTime();
      if (launchDateTime == null || !launchDateTime.isAfter(now)) {
        continue;
      }

      if (stateFilter != null && flight.getState() != stateFilter) {
        continue;
      }

      out.add(flight);
    }

    return out;
  }

  /**
   * Cancels an existing flight, setting its state to
   * {@link FlightState#CANCELLED}.
   * The operation is idempotent for already cancelled flights.
   *
   * @param id the flight id
   * @return the updated flight, or null if not found
   */
  public Flight cancelById(String id) {
    String trimmedId = id == null ? null : id.trim();
    if (trimmedId == null || trimmedId.isEmpty()) {
      throw new IllegalArgumentException("id must be provided");
    }

    Flight flight = flightRepository.findById(trimmedId);
    if (flight == null) {
      return null;
    }

    refreshStateOnRead(flight);

    FlightState state = flight.getState();
    if (state == FlightState.DONE) {
      throw new IllegalStateException("flight is DONE and cannot be cancelled");
    }

    if (state == FlightState.CANCELLED) {
      return flight;
    }

    flight.setState(FlightState.CANCELLED);
    Flight saved = flightRepository.save(flight);

    int bookings = bookingRepository.countByFlightId(saved.getId());
    LOGGER.log(Level.INFO, "Flight cancelled: {0}", saved.getId());
    LOGGER.log(Level.INFO, "Simulating cancellation notification for flight: {0}", saved.getId());
    LOGGER.log(Level.INFO, "Simulating refunds for {0} bookings on flight: {1}",
        new Object[] { bookings, saved.getId() });

    return saved;
  }

  /**
   * Refreshes state derived from time and (future) bookings.
   *
   * @param flight the flight to refresh
   */
  public void refreshStateOnRead(Flight flight) {
    if (flight == null) {
      return;
    }

    // Be careful with CANCELLED/DONE precedence: DONE takes precedence after launch
    FlightState current = flight.getState();

    Instant launchDateTime = flight.getLaunchDateTime();
    if (launchDateTime != null && Instant.now().isAfter(launchDateTime)) {
      if (current != FlightState.DONE) {
        flight.setState(FlightState.DONE);
        LOGGER.log(Level.INFO, "Flight state changed to DONE: {0}", flight.getId());
      }
      return;
    }

    // If already cancelled and not yet past launch, keep cancelled (do not reopen)
    if (current == FlightState.CANCELLED) {
      return;
    }

    String rocketId = flight.getRocketId();
    if (rocketId == null || rocketId.trim().isEmpty()) {
      if (current != FlightState.SCHEDULED) {
        flight.setState(FlightState.SCHEDULED);
      }
      return;
    }

    Rocket rocket = rocketService.findById(rocketId.trim());
    Integer capacity = rocket == null ? null : rocket.getCapacity();
    if (capacity == null || capacity < 1) {
      if (current != FlightState.SCHEDULED) {
        flight.setState(FlightState.SCHEDULED);
      }
      return;
    }

    int bookings = bookingRepository.countByFlightId(flight.getId());

    // Rule-based cancellation: if within 7 days of launch and below minimum, cancel
    if (launchDateTime != null && Instant.now().isAfter(launchDateTime.minusSeconds(7 * 24 * 3600))) {
      Integer minimum = flight.getMinimumPassengers();
      if (minimum != null && bookings < minimum.intValue()) {
        if (current != FlightState.CANCELLED) {
          flight.setState(FlightState.CANCELLED);
          LOGGER.log(Level.INFO, "Flight state changed to CANCELLED (rule-based): {0}", flight.getId());
          LOGGER.log(Level.INFO, "Simulating cancellation notification for flight: {0}", flight.getId());
          LOGGER.log(Level.INFO, "Simulating refunds for {0} bookings on flight: {1}",
              new Object[] { bookings, flight.getId() });
        }
        return;
      }
    }

    FlightState desired;
    if (bookings >= capacity.intValue()) {
      desired = FlightState.SOLD_OUT;
    } else if (flight.getMinimumPassengers() != null && bookings >= flight.getMinimumPassengers().intValue()) {
      desired = FlightState.CONFIRMED;
    } else {
      desired = FlightState.SCHEDULED;
    }

    if (current != desired) {
      flight.setState(desired);
      LOGGER.log(Level.INFO, "Flight state changed to {0}: {1}", new Object[] { desired, flight.getId() });
      if (desired == FlightState.CONFIRMED) {
        LOGGER.log(Level.INFO, "Simulating payment capture and confirmation notification for flight: {0}",
            flight.getId());
      } else if (desired == FlightState.SOLD_OUT) {
        LOGGER.log(Level.INFO, "Simulating SOLD_OUT notification for flight: {0}", flight.getId());
      }
    }
  }
}
