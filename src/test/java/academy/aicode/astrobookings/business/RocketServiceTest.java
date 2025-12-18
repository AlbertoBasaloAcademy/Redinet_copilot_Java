package academy.aicode.astrobookings.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Range;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;

class RocketServiceTest {

  private RocketService rocketService;

  @BeforeEach
  void setUp() {
    clearRocketRepository();
    rocketService = new RocketService();
  }

  @Test
  void create_whenRequestIsNull_throwsIllegalArgumentException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> rocketService.create((CreateRocketRequest) null));
    assertTrue(ex.getMessage().contains("Request body must be provided"));
  }

  @Test
  void create_whenNameIsNull_throwsIllegalArgumentException() {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName(null);
    req.setCapacity(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rocketService.create(req));
    assertTrue(ex.getMessage().contains("Rocket name must be provided"));
  }

  @Test
  void create_whenNameIsBlank_throwsIllegalArgumentException() {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("   ");
    req.setCapacity(1);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rocketService.create(req));
    assertTrue(ex.getMessage().contains("Rocket name must be provided"));
  }

  @Test
  void create_whenCapacityIsNull_throwsIllegalArgumentException() {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("Falcon");
    req.setCapacity(null);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rocketService.create(req));
    assertTrue(ex.getMessage().contains("Rocket capacity must be between 1 and 10"));
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, 11 })
  void create_whenCapacityIsOutOfRange_throwsIllegalArgumentException(int capacity) {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("Falcon");
    req.setCapacity(capacity);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rocketService.create(req));
    assertTrue(ex.getMessage().contains("Rocket capacity must be between 1 and 10"));
  }

  @Test
  void create_whenRequestIsValid_trimsName_generatesId_andPersistsOptionalFields() {
    CreateRocketRequest req = new CreateRocketRequest();
    req.setName("  Falcon  ");
    req.setCapacity(10);
    req.setRange(Range.LEO);
    req.setSpeed(7.8);

    Rocket saved = rocketService.create(req);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals("Falcon", saved.getName());
    assertEquals(10, saved.getCapacity());
    assertEquals(Range.LEO, saved.getRange());
    assertEquals(7.8, saved.getSpeed());

    Rocket found = rocketService.findById(saved.getId());
    assertNotNull(found);
    assertEquals(saved.getId(), found.getId());
  }

  @Test
  void findById_whenRocketDoesNotExist_returnsNull() {
    Rocket found = rocketService.findById("missing-id");
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
