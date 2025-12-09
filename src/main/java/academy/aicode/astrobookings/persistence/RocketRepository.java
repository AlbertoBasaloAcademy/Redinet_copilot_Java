package academy.aicode.astrobookings.persistence;

import java.util.HashMap;
import java.util.Map;

import academy.aicode.astrobookings.persistence.models.Rocket;

/**
 * Repositorio sencillo en memoria para almacenar instancias de {@link Rocket}.
 * Genera un id automático cuando el objeto no tiene `id`.
 */
public class RocketRepository {
  private static final Map<String, Rocket> rockets = new HashMap<>();
  private static int nextId = 0;

  /**
   * Guarda el cohete en memoria. Si `rocket.id` es null, se genera uno nuevo.
   * 
   * @param rocket instancia a guardar
   * @return la instancia guardada (con `id` asignado si fue necesario)
   */
  public Rocket save(Rocket rocket) {
    if (rocket.getId() == null) {
      rocket.setId("r" + nextId++);
    }
    rockets.put(rocket.getId(), rocket);
    return rocket;
  }
}