package academy.aicode.astrobookings.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import academy.aicode.astrobookings.persistence.models.Booking;

/**
 * In-memory repository for {@link Booking} instances.
 */
public class BookingRepository {

  private static final Map<String, Booking> bookings = new ConcurrentHashMap<>();

  /**
   * Saves the booking in memory, generating an id if needed.
   *
   * @param booking the booking to save
   * @return the saved booking
   */
  public Booking save(Booking booking) {
    if (booking.getId() == null) {
      booking.setId(UUID.randomUUID().toString());
    }
    if (booking.getCreatedAt() == null) {
      booking.setCreatedAt(Instant.now());
    }
    bookings.put(booking.getId(), booking);
    return booking;
  }

  /**
   * Finds a booking by id.
   *
   * @param id the booking id
   * @return the booking or null
   */
  public Booking findById(String id) {
    return bookings.get(id);
  }

  /**
   * Returns all bookings for a given flight id.
   *
   * @param flightId the flight id
   * @return list of bookings for the flight
   */
  public List<Booking> findByFlightId(String flightId) {
    List<Booking> out = new ArrayList<>();
    for (Booking booking : bookings.values()) {
      if (flightId.equals(booking.getFlightId())) {
        out.add(booking);
      }
    }
    return out;
  }

  /**
   * Counts bookings for a given flight id.
   *
   * @param flightId the flight id
   * @return the count
   */
  public int countByFlightId(String flightId) {
    int count = 0;
    for (Booking booking : bookings.values()) {
      if (flightId.equals(booking.getFlightId())) {
        count++;
      }
    }
    return count;
  }
}
