package academy.aicode.astrobookings.presentation.dto;

import academy.aicode.astrobookings.persistence.models.Range;

/**
 * DTO de salida para representar un Rocket en respuestas HTTP.
 */
public class RocketResponse {
  private String id;
  private String name;
  private Integer capacity;
  private Range range;
  private Double speed;

  public RocketResponse() {
  }

  public RocketResponse(String id, String name, Integer capacity, Range range, Double speed) {
    this.id = id;
    this.name = name;
    this.capacity = capacity;
    this.range = range;
    this.speed = speed;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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
