package academy.aicode.astrobookings.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import academy.aicode.astrobookings.persistence.models.Flight;
import academy.aicode.astrobookings.persistence.models.FlightState;

class FlightRepositoryTest {

  private FlightRepository flightRepository;

  @BeforeEach
  void setUp() {
    clearFlightRepository();
    flightRepository = new FlightRepository();
  }

  @Test
  void save_whenIdIsNull_generatesId_andStoresFlight() {
    Flight flight = new Flight();
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().plusSeconds(3600));
    flight.setBasePrice(1000.0);
    flight.setMinimumPassengers(1);
    flight.setState(FlightState.SCHEDULED);

    Flight saved = flightRepository.save(flight);

    assertNotNull(saved.getId());
    Flight found = flightRepository.findById(saved.getId());
    assertNotNull(found);
    assertEquals("rocket-1", found.getRocketId());
  }

  @Test
  void save_whenIdIsProvided_preservesId() {
    Flight flight = new Flight();
    flight.setId("fixed-id");
    flight.setRocketId("rocket-1");
    flight.setLaunchDateTime(Instant.now().plusSeconds(3600));
    flight.setBasePrice(1000.0);
    flight.setMinimumPassengers(1);
    flight.setState(FlightState.SCHEDULED);

    Flight saved = flightRepository.save(flight);

    assertEquals("fixed-id", saved.getId());
    assertNotNull(flightRepository.findById("fixed-id"));
  }

  @Test
  void findAll_whenEmpty_returnsEmptyList() {
    List<Flight> all = flightRepository.findAll();

    assertNotNull(all);
    assertTrue(all.isEmpty());
  }

  @Test
  void findAll_whenHasItems_returnsAllItems() {
    Flight f1 = new Flight();
    f1.setRocketId("rocket-1");
    f1.setLaunchDateTime(Instant.now().plusSeconds(3600));
    f1.setBasePrice(1000.0);
    f1.setMinimumPassengers(1);
    f1.setState(FlightState.SCHEDULED);
    flightRepository.save(f1);

    Flight f2 = new Flight();
    f2.setRocketId("rocket-2");
    f2.setLaunchDateTime(Instant.now().plusSeconds(7200));
    f2.setBasePrice(1200.0);
    f2.setMinimumPassengers(1);
    f2.setState(FlightState.SCHEDULED);
    flightRepository.save(f2);

    List<Flight> all = flightRepository.findAll();

    assertEquals(2, all.size());
  }

  @Test
  void findById_whenMissing_returnsNull() {
    Flight found = flightRepository.findById("missing-id");
    assertNull(found);
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
}
