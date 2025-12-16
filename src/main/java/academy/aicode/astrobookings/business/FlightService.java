package academy.aicode.astrobookings.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
   * Refreshes state derived from time and (future) bookings.
   *
   * @param flight the flight to refresh
   */
  public void refreshStateOnRead(Flight flight) {
    if (flight == null) {
      return;
    }

    FlightState current = flight.getState();
    if (current == FlightState.CANCELLED) {
      return;
    }

    Instant launchDateTime = flight.getLaunchDateTime();
    if (launchDateTime != null && Instant.now().isAfter(launchDateTime)) {
      if (current != FlightState.DONE) {
        flight.setState(FlightState.DONE);
        LOGGER.log(Level.INFO, "Flight state changed to DONE: {0}", flight.getId());
      }
      return;
    }

    // Booking-derived states (CONFIRMED/SOLD_OUT) will be added when Booking
    // feature exists.
    if (current == null) {
      flight.setState(FlightState.SCHEDULED);
    }
  }
}
