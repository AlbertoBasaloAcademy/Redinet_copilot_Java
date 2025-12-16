package academy.aicode.astrobookings.persistence.models;

import java.time.Instant;

/**
 * In-memory domain entity that represents a flight.
 */
public class Flight {
  private String id;
  private String rocketId;
  private Instant launchDateTime;
  private Double basePrice;
  private Integer minimumPassengers;
  private FlightState state;

  /**
   * Creates an empty flight instance.
   */
  public Flight() {
  }

  /**
   * Creates a flight instance.
   *
   * @param id                the flight id
   * @param rocketId          the rocket id
   * @param launchDateTime    the launch date/time
   * @param basePrice         the base price
   * @param minimumPassengers the minimum passengers
   * @param state             the current flight state
   */
  public Flight(String id, String rocketId, Instant launchDateTime, Double basePrice, Integer minimumPassengers,
      FlightState state) {
    this.id = id;
    this.rocketId = rocketId;
    this.launchDateTime = launchDateTime;
    this.basePrice = basePrice;
    this.minimumPassengers = minimumPassengers;
    this.state = state;
  }

  /**
   * Returns the flight id.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the flight id.
   */
  public void setId(String id) {
    this.id = id;
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
   * Returns the minimum passengers required to confirm the flight.
   */
  public Integer getMinimumPassengers() {
    return minimumPassengers;
  }

  /**
   * Sets the minimum passengers required to confirm the flight.
   */
  public void setMinimumPassengers(Integer minimumPassengers) {
    this.minimumPassengers = minimumPassengers;
  }

  /**
   * Returns the current flight state.
   */
  public FlightState getState() {
    return state;
  }

  /**
   * Sets the current flight state.
   */
  public void setState(FlightState state) {
    this.state = state;
  }
}
