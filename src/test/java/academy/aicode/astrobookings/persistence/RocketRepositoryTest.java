package academy.aicode.astrobookings.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import academy.aicode.astrobookings.persistence.models.Range;
import academy.aicode.astrobookings.persistence.models.Rocket;

class RocketRepositoryTest {

  private RocketRepository rocketRepository;

  @BeforeEach
  void setUp() {
    clearRocketRepository();
    rocketRepository = new RocketRepository();
  }

  @Test
  void save_whenIdIsNull_generatesId_andStoresRocket() {
    Rocket rocket = new Rocket();
    rocket.setName("Falcon");
    rocket.setCapacity(10);
    rocket.setRange(Range.LEO);
    rocket.setSpeed(7.8);

    Rocket saved = rocketRepository.save(rocket);

    assertNotNull(saved.getId());
    Rocket found = rocketRepository.findById(saved.getId());
    assertNotNull(found);
    assertEquals("Falcon", found.getName());
  }

  @Test
  void save_whenIdIsProvided_preservesId() {
    Rocket rocket = new Rocket();
    rocket.setId("fixed-id");
    rocket.setName("Falcon");
    rocket.setCapacity(10);

    Rocket saved = rocketRepository.save(rocket);

    assertEquals("fixed-id", saved.getId());
    assertNotNull(rocketRepository.findById("fixed-id"));
  }

  @Test
  void findAll_whenEmpty_returnsEmptyList() {
    List<Rocket> all = rocketRepository.findAll();
    assertNotNull(all);
    assertTrue(all.isEmpty());
  }

  @Test
  void findAll_whenHasItems_returnsAllItems() {
    Rocket r1 = new Rocket();
    r1.setName("Falcon");
    r1.setCapacity(10);
    rocketRepository.save(r1);

    Rocket r2 = new Rocket();
    r2.setName("Starship");
    r2.setCapacity(10);
    rocketRepository.save(r2);

    List<Rocket> all = rocketRepository.findAll();
    assertEquals(2, all.size());
  }

  @Test
  void findById_whenMissing_returnsNull() {
    Rocket found = rocketRepository.findById("missing-id");
    assertNull(found);
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
}
