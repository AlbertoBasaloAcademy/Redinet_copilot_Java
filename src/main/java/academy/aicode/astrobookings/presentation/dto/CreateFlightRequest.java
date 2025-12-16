package academy.aicode.astrobookings.presentation.dto;

import java.time.Instant;

/**
 * DTO used to create a Flight via the API.
 */
public class CreateFlightRequest {
  private String rocketId;
  private Instant launchDateTime;
  private Double basePrice;
  private Integer minimumPassengers;

  /**
   * Creates an empty request.
   */
  public CreateFlightRequest() {
  }

  /**
   * Returns the rocket id.
   */
  public String getRocketId() {
    return rocketId;
  }

  /**
   * Sets the rocket id.
   */
  public void setRocketId(String rocketId) {
    this.rocketId = rocketId;
  }

  /**
   * Returns the launch date/time.
   */
  public Instant getLaunchDateTime() {
    return launchDateTime;
  }

  /**
   * Sets the launch date/time.
   */
  public void setLaunchDateTime(Instant launchDateTime) {
    this.launchDateTime = launchDateTime;
  }

  /**
   * Returns the base price.
   */
  public Double getBasePrice() {
    return basePrice;
  }

  /**
   * Sets the base price.
   */
  public void setBasePrice(Double basePrice) {
    this.basePrice = basePrice;
  }

  /**
   * Returns the minimum passengers required.
   */
  public Integer getMinimumPassengers() {
    return minimumPassengers;
  }

  /**
   * Sets the minimum passengers required.
   */
  public void setMinimumPassengers(Integer minimumPassengers) {
    this.minimumPassengers = minimumPassengers;
  }
}
