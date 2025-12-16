package academy.aicode.astrobookings.presentation.dto;

/**
 * DTO used to create a Booking via the API.
 */
public class CreateBookingRequest {
  private String flightId;
  private String passengerName;
  private String passengerDocument;

  /**
   * Creates an empty request.
   */
  public CreateBookingRequest() {
  }

  /**
   * Returns the flight id.
   */
  public String getFlightId() {
    return flightId;
  }

  /**
   * Sets the flight id.
   */
  public void setFlightId(String flightId) {
    this.flightId = flightId;
  }

  /**
   * Returns the passenger name.
   */
  public String getPassengerName() {
    return passengerName;
  }

  /**
   * Sets the passenger name.
   */
  public void setPassengerName(String passengerName) {
    this.passengerName = passengerName;
  }

  /**
   * Returns the passenger document.
   */
  public String getPassengerDocument() {
    return passengerDocument;
  }

  /**
   * Sets the passenger document.
   */
  public void setPassengerDocument(String passengerDocument) {
    this.passengerDocument = passengerDocument;
  }
}
