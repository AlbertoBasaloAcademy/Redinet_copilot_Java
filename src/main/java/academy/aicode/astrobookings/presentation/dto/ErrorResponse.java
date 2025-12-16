package academy.aicode.astrobookings.presentation.dto;

import java.util.Map;

/**
 * DTO para respuestas de error estructuradas.
 */
public class ErrorResponse {
  private String error;
  private String code;
  private Map<String, String> details;

  public ErrorResponse() {
  }

  public ErrorResponse(String error, String code, Map<String, String> details) {
    this.error = error;
    this.code = code;
    this.details = details;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Map<String, String> getDetails() {
    return details;
  }

  public void setDetails(Map<String, String> details) {
    this.details = details;
  }
}
