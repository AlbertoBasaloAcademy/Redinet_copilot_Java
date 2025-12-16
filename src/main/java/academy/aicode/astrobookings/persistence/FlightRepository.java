package academy.aicode.astrobookings.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import academy.aicode.astrobookings.persistence.models.Flight;

/**
 * In-memory repository for {@link Flight} instances.
 */
public class FlightRepository {

  private static final Map<String, Flight> flights = new ConcurrentHashMap<>();

  /**
   * Saves the flight in memory, generating an id if needed.
   *
   * @param flight the flight to save
   * @return the saved flight
   */
  public Flight save(Flight flight) {
    if (flight.getId() == null) {
      flight.setId(UUID.randomUUID().toString());
    }
    flights.put(flight.getId(), flight);
    return flight;
  }

  /**
   * Finds a flight by id.
   *
   * @param id the flight id
   * @return the flight or null
   */
  public Flight findById(String id) {
    return flights.get(id);
  }

  /**
   * Returns all flights currently stored.
   */
  public List<Flight> findAll() {
    return new ArrayList<>(flights.values());
  }
}
