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
    if (rocket.getCapacity() <= 0 || rocket.getCapacity() > 10) {
      return "Rocket capacity must be between 1 and 10";
    }
    return null;
  }

}
