package academy.aicode.astrobookings.presentation.dto;

import academy.aicode.astrobookings.persistence.models.Range;

/**
 * DTO para la creación de un Rocket via API.
 */
public class CreateRocketRequest {
  private String name;
  private Integer capacity;
  private Range range;
  private Double speed;

  public CreateRocketRequest() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public Double getSpeed() {
    return speed;
  }

  public void setSpeed(Double speed) {
    this.speed = speed;
  }
}
