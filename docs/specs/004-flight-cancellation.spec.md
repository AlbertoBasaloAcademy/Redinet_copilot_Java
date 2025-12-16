# 004-flight-cancellation Specification

## 1. 👔 Problem Specification

AstroBookings SHALL support manual flight cancellation via the REST API, updating the flight lifecycle state to `CANCELLED` and simulating notifications and refunds via logs.

### Cancel a Flight

- **As a** API client
- **I want to** cancel an existing flight
- **So that** no further bookings are accepted and customers are notified/refunded (simulated)

### View a Cancelled Flight

- **As a** API client
- **I want to** retrieve a flight and see it is cancelled
- **So that** I can display accurate flight status to users

### List Cancelled Future Flights

- **As a** API client
- **I want to** list future flights filtered by `CANCELLED`
- **So that** I can audit and review cancelled flights

## 2. 🧑‍💻 Solution Overview

Implement manual cancellation using the existing layered, in-memory architecture: a presentation endpoint calling a business method that updates flight state to `CANCELLED` and writes log entries to simulate notifications and refunds.

### Data Models

**Flight (entity)**

- `id` (string, server-generated, read-only)
- `state` (string enum, server-controlled): includes `CANCELLED`
- other fields unchanged

**FlightResponse (output DTO)**

- returns the full flight representation including `state = CANCELLED` after cancellation

**CancelFlight (operation)**

- No request body is required.

### Software Components

**Presentation layer**

- Extend the flight API to support a cancellation action:
  - `POST /flights/{id}/cancel` cancels a flight
- The implementation MAY be done by:
  - extending `FlightHandler` to route `POST` with `/flights/{id}/cancel`, OR
  - adding a dedicated handler bound to a `/flights/{id}/cancel` context.

**Business layer**

- `FlightService.cancelById(id)` (or equivalent) SHALL:
  - validate `id` is non-blank
  - load the flight from `FlightRepository`
  - reject cancellation if the flight is already `DONE`
  - set `state = CANCELLED` and persist the update
  - write log entries simulating notifications and refunds
  - be idempotent when the flight is already `CANCELLED`

**Persistence layer**

- `FlightRepository` stores the flight state in-memory.
- `BookingRepository` MAY be consulted to obtain booking counts (or list) for log messages simulating refunds.

### User Interface

REST API (JSON over HTTP):

- **Cancel**: `POST /flights/{id}/cancel`
  - Request body: empty
  - Response body example:
    - `{ "id": "<id>", "rocketId": "<rocket-id>", "launchDateTime": "2026-01-15T10:00:00Z", "basePrice": 1000.0, "minimumPassengers": 3, "state": "CANCELLED" }`

- **Get by id** (existing): `GET /flights/{id}`
  - Response includes `state` (may be `CANCELLED`).

- **List future flights filtered** (existing): `GET /flights?state=CANCELLED`
  - Returns future flights in `CANCELLED` state.

### Aspects

- **Validation**:
  - reject missing/blank `{id}`
  - reject cancellation for non-existent flights
  - reject cancellation when flight is `DONE`
- **Error handling**:
  - return JSON error payloads with appropriate HTTP status codes (400/404/405/409)
- **Monitoring/Logging**:
  - log flight cancellation
  - log simulated notification and refund actions (TR3)
- **Security**:
  - none (no authentication/authorization) as per TR1
- **Performance**:
  - in-memory collections; expected small dataset

## 3. 🧑‍⚖️ Acceptance Criteria

### Cancel a Flight

- [ ] WHEN the client sends `POST /flights/{id}/cancel` for an existing flight that is not `DONE`, the system SHALL set the flight state to `CANCELLED` and return HTTP 200 with the updated flight representation.
- [ ] WHEN the client sends `POST /flights/{id}/cancel` for a non-existent flight id, the system SHALL return HTTP 404 with a JSON error.
- [ ] IF the client sends `POST /flights/{id}/cancel` for a flight whose state is `DONE`, THEN the system SHALL return HTTP 409 with a JSON error indicating the flight cannot be cancelled.

### View a Cancelled Flight

- [ ] WHEN the client sends `GET /flights/{id}` for a cancelled flight, the system SHALL return HTTP 200 with `state = CANCELLED`.
- [ ] WHEN the client sends `GET /flights/{id}` for a non-existent flight id, the system SHALL return HTTP 404 with a JSON error.
- [ ] IF the `{id}` path parameter is missing or blank, THEN the system SHALL return HTTP 400 with a JSON error.

### List Cancelled Future Flights

- [ ] WHEN the client sends `GET /flights?state=CANCELLED`, the system SHALL return HTTP 200 with a JSON array of future flights whose `state` is `CANCELLED` (which MAY be empty).
- [ ] IF the `state` query parameter value is not a supported enum value, THEN the system SHALL return HTTP 400 with a JSON error.
- [ ] WHEN the client sends an unsupported HTTP method to the cancellation endpoint, the system SHALL return HTTP 405 with a JSON error.

> End of Feature Specification for 004-flight-cancellation, last updated December 16, 2025.
