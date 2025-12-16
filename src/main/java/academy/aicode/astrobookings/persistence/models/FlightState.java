package academy.aicode.astrobookings.persistence.models;

/**
 * Flight lifecycle state exposed by the API.
 */
public enum FlightState {
  SCHEDULED,
  CONFIRMED,
  SOLD_OUT,
  CANCELLED,
  DONE
}
