package academy.aicode.astrobookings.business;

import java.util.List;

import academy.aicode.astrobookings.persistence.RocketRepository;
import academy.aicode.astrobookings.persistence.models.Rocket;

/**
 * Servicio de negocio para operaciones sobre `Rocket`.
 * Encapsula validaciones y acceso al repositorio.
 */
public class RocketService {

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
      throw new IllegalArgumentException(error);
    }
    return rocketRepository.save(rocket);
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
