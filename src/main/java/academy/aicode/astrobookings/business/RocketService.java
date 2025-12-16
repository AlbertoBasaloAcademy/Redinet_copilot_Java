package academy.aicode.astrobookings.business;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Rocket;
import academy.aicode.astrobookings.presentation.dto.CreateRocketRequest;

/**
 * Servicio de negocio para operaciones sobre `Rocket`.
 * Encapsula validaciones y acceso al repositorio.
 */
public class RocketService {

  private static final Logger LOGGER = Logger.getLogger(RocketService.class.getName());

  private final RocketRepository rocketRepository = new RocketRepository();

  public List<Rocket> findAll() {
    return rocketRepository.findAll();
  }

  public Rocket findById(String id) {
    return rocketRepository.findById(id);
  }

  /**
   * Crea un cohete después de validar la entrada.
   * 
   * @throws IllegalArgumentException si la validación falla
   */
  public Rocket create(Rocket rocket) {
    String error = validateRocket(rocket);
    if (error != null) {
      LOGGER.log(Level.WARNING, "Create rocket validation failed: {0}", error);
      throw new IllegalArgumentException(error);
    }
    return rocketRepository.save(rocket);
  }

  /**
   * Crea un Rocket a partir del DTO de petición, con validaciones y logging.
   *
   * @throws IllegalArgumentException si la validación falla
   */
  public Rocket create(CreateRocketRequest req) {
    if (req == null) {
      LOGGER.warning("CreateRocketRequest was null");
      throw new IllegalArgumentException("Request body must be provided");
    }

    String name = req.getName() == null ? null : req.getName().trim();
    Integer capacity = req.getCapacity();

    if (name == null || name.isEmpty()) {
      LOGGER.warning("Validation failed: name is blank");
      throw new IllegalArgumentException("Rocket name must be provided");
    }
    if (capacity == null || capacity < 1 || capacity > 10) {
      LOGGER.warning("Validation failed: capacity out of range");
      throw new IllegalArgumentException("Rocket capacity must be between 1 and 10");
    }

    Rocket r = new Rocket();
    r.setName(name);
    r.setCapacity(capacity);
    r.setRange(req.getRange());
    r.setSpeed(req.getSpeed());

    try {
      Rocket saved = rocketRepository.save(r);
      LOGGER.log(Level.INFO, "Rocket created: {0}", saved.getId());
      return saved;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to save rocket", e);
      throw new IllegalArgumentException("Failed to create rocket: " + e.getMessage());
    }
  }

  private String validateRocket(Rocket rocket) {
    if (rocket == null) {
      return "Rocket must be provided";
    }
    if (rocket.getName() == null || rocket.getName().trim().isEmpty()) {
      return "Rocket name must be provided";
    }
    if (rocket.getCapacity() == null || rocket.getCapacity() <= 0 || rocket.getCapacity() > 10) {
      return "Rocket capacity must be between 1 and 10";
    }
    return null;
  }

  /**
   * Actualiza parcialmente un cohete existente. Solo los campos no nulos
   * del parámetro `updates` se aplican. Valida los cambios antes de guardar.
   *
   * @param id      id del cohete a actualizar
   * @param updates objeto con los campos a actualizar
   * @return la instancia actualizada
   * @throws IllegalArgumentException para entradas inválidas
   */
  public Rocket update(String id, Rocket updates) {
    if (id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Rocket id must be provided");
    }
    Rocket existing = rocketRepository.findById(id);
    if (existing == null) {
      return null;
    }

    // Apply updates selectively
    if (updates.getName() != null && !updates.getName().trim().isEmpty()) {
      existing.setName(updates.getName());
    }
    if (updates.getCapacity() != null) {
      int cap = updates.getCapacity();
      if (cap <= 0 || cap > 10) {
        throw new IllegalArgumentException("Rocket capacity must be between 1 and 10");
      }
      existing.setCapacity(cap);
    }
    if (updates.getSpeed() != null) {
      existing.setSpeed(updates.getSpeed());
    }

    return rocketRepository.save(existing);
  }

}
