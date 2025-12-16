package academy.aicode.astrobookings.business;

/**
 * Exception thrown when a booking cannot be created due to a business conflict
 * (typically an invalid flight state or capacity constraints).
 */
public class BookingConflictException extends RuntimeException {

  /**
   * Creates a conflict exception with a message.
   *
   * @param message the error message
   */
  public BookingConflictException(String message) {
    super(message);
  }
}
