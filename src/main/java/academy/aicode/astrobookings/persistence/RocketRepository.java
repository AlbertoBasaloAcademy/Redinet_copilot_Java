package academy.aicode.astrobookings.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import academy.aicode.astrobookings.persistence.models.Rocket;

/**
 * Repositorio sencillo en memoria para almacenar instancias de {@link Rocket}.
 * Genera un id automático cuando el objeto no tiene `id`.
 */
public class RocketRepository {
  private static final Map<String, Rocket> rockets = new HashMap<>();

  /**
   * Guarda el cohete en memoria. Si `rocket.id` es null, se genera uno nuevo.
   * 
   * @param rocket instancia a guardar
   * @return la instancia guardada (con `id` asignado si fue necesario)
   */
  public Rocket save(Rocket rocket) {
    if (rocket.getId() == null) {
      rocket.setId(UUID.randomUUID().toString());
    }
    rockets.put(rocket.getId(), rocket);
    return rocket;
  }

  /**
   * Devuelve todos los cohetes almacenados.
   */
  public java.util.List<Rocket> findAll() {
    return new java.util.ArrayList<>(rockets.values());
  }

  /**
   * Busca un Rocket por su id. Devuelve null si no existe.
   */
  public Rocket findById(String id) {
    return rockets.get(id);
  }
}