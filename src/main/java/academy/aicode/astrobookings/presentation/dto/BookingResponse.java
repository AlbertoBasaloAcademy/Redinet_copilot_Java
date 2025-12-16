package academy.aicode.astrobookings.presentation.dto;

import java.time.Instant;

/**
 * DTO returned by the API to represent a Booking.
 */
public class BookingResponse {
  private String id;
  private String flightId;
  private String passengerName;
  private String passengerDocument;
  private Double finalPrice;
  private Integer discountPercent;
  private Instant createdAt;

  /**
   * Creates an empty response.
   */
  public BookingResponse() {
  }

  /**
   * Creates a response with all fields.
   */
  public BookingResponse(String id, String flightId, String passengerName, String passengerDocument, Double finalPrice,
      Integer discountPercent, Instant createdAt) {
    this.id = id;
    this.flightId = flightId;
    this.passengerName = passengerName;
    this.passengerDocument = passengerDocument;
    this.finalPrice = finalPrice;
    this.discountPercent = discountPercent;
    this.createdAt = createdAt;
  }

  /**
   * Returns the booking id.
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the booking id.
   */
  public void setId(String id) {
    this.id = id;
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

  /**
   * Returns the final price.
   */
  public Double getFinalPrice() {
    return finalPrice;
  }

  /**
   * Sets the final price.
   */
  public void setFinalPrice(Double finalPrice) {
    this.finalPrice = finalPrice;
  }

  /**
   * Returns the discount percent.
   */
  public Integer getDiscountPercent() {
    return discountPercent;
  }

  /**
   * Sets the discount percent.
   */
  public void setDiscountPercent(Integer discountPercent) {
    this.discountPercent = discountPercent;
  }

  /**
   * Returns the creation timestamp.
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets the creation timestamp.
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
