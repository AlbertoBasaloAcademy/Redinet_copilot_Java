package academy.aicode.astrobookings.persistence;

import java.util.HashMap;
import java.util.Map;

import academy.aicode.astrobookings.persistence.models.Rocket;

public class RocketRepository {
  private static final Map<String, Rocket> rockets = new HashMap<>();
  private static int nextId = 0;

  public Rocket save(Rocket rocket) {
    if (rocket.getId() == null) {
      rocket.setId("r" + nextId++);
    }
    rockets.put(rocket.getId(), rocket);
    return rocket;
  }
}