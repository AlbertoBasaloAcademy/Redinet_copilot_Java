# 002-flight-management Specification

## 1. 👔 Problem Specification

AstroBookings SHALL expose a minimal REST API to create and list flights, enforcing business validation and exposing a flight lifecycle state for demo/training purposes.

### Create Flight

- **As a** API client
- **I want to** create a flight for a given rocket with a future launch date and a base price
- **So that** customers can later book seats on a valid, scheduled flight

### List/Filter Future Flights

- **As a** API client
- **I want to** list future flights and optionally filter by flight state
- **So that** I can present available flights and their status

### View Flight Status

- **As a** API client
- **I want to** retrieve a flight and observe its current state
- **So that** I can reflect automatic state transitions as bookings and time progress

## 2. 🧑‍💻 Solution Overview

Implement a layered, in-memory flight feature using JDK HTTP server handlers (presentation), a service for validation and state rules (business), and an in-memory repository (persistence).

### Data Models

**Flight (entity)**

- `id` (string, server-generated, read-only)
- `rocketId` (string, required)
- `launchDateTime` (string, required, ISO-8601, must be in the future)
- `basePrice` (number, required, $> 0$)
- `minimumPassengers` (integer, required, $1..capacity(rocket)$)
- `state` (string enum, server-controlled): one of `SCHEDULED`, `CONFIRMED`, `SOLD_OUT`, `CANCELLED`, `DONE`

**CreateFlightRequest (input DTO)**

- `rocketId`, `launchDateTime`, `basePrice`, `minimumPassengers`

**FlightResponse (output DTO)**

- `id`, `rocketId`, `launchDateTime`, `basePrice`, `minimumPassengers`, `state`

### Software Components

**Presentation layer**

- `FlightHandler` exposes HTTP endpoints under `/flights`:
  - `POST /flights` creates a flight
  - `GET /flights` lists future flights; MAY accept optional query `state` to filter
  - `GET /flights/{id}` returns a flight by id

**Business layer**

- `FlightService` validates requests and orchestrates persistence.
- `FlightService` computes/refreshes flight state on reads and on relevant events (e.g., bookings created), without background jobs.
- `RocketService` (or equivalent) is used to validate that `rocketId` exists and to get rocket capacity for validation.

**Persistence layer**

- `FlightRepository` stores flights in memory using Java collections and generates ids.

### User Interface

REST API (JSON over HTTP):

- **Create**: `POST /flights`
  - Request body example:
    - `{ "rocketId": "<rocket-id>", "launchDateTime": "2026-01-15T10:00:00Z", "basePrice": 1000.0, "minimumPassengers": 3 }`
  - Response body example:
    - `{ "id": "<generated>", "rocketId": "<rocket-id>", "launchDateTime": "2026-01-15T10:00:00Z", "basePrice": 1000.0, "minimumPassengers": 3, "state": "SCHEDULED" }`

- **List future flights**: `GET /flights`
  - Response body example: `[{ ... }, { ... }]`

- **Filter by state**: `GET /flights?state=CONFIRMED`
  - Response body example: `[{ ... }]`

- **Get by id**: `GET /flights/{id}`
  - Response body example: `{ ... }`

### Aspects

- **Validation**:
  - reject flights if `rocketId` does not exist
  - enforce `launchDateTime` is in the future
  - enforce `basePrice` is $> 0$
  - enforce `minimumPassengers` is $1..capacity(rocket)$
  - reject invalid enum values for `state` query
- **State transitions (no background scheduler)**:
  - create flight with `state = SCHEDULED`
  - WHILE the flight is not `CANCELLED`, the system SHALL derive:
    - `DONE` when current time is after `launchDateTime`
    - `SOLD_OUT` when booking count reaches rocket capacity
    - `CONFIRMED` when booking count reaches `minimumPassengers`
  - booking count is provided by Booking feature (FR3); if bookings are not implemented yet, the system MAY keep flights as `SCHEDULED`.
- **Error handling**: return JSON error payloads with appropriate HTTP status codes (400/404/405/409).
- **Monitoring/Logging**: log flight creation and state changes at an appropriate level.
- **Security**: none (no authentication/authorization) as per TR1.
- **Performance**: in-memory collections; expected small dataset.

## 3. 🧑‍⚖️ Acceptance Criteria

### Create Flight

- [ ] WHEN the client sends `POST /flights` with a valid payload, the system SHALL create the flight and return HTTP 201 with the created flight (including a server-generated `id` and initial `state = SCHEDULED`).
- [ ] IF the payload contains a `rocketId` that does not exist, THEN the system SHALL return HTTP 400 with a JSON error describing the validation failure.
- [ ] IF `launchDateTime` is not in the future OR `basePrice` is not $> 0$ OR `minimumPassengers` is outside $1..capacity(rocket)$, THEN the system SHALL return HTTP 400 with a JSON error describing the validation failure.

### List/Filter Future Flights

- [ ] WHEN the client sends `GET /flights`, the system SHALL return HTTP 200 with a JSON array containing only flights with `launchDateTime` in the future (which MAY be empty).
- [ ] WHERE the client provides `state` in `GET /flights?state=<value>`, the system SHALL return HTTP 200 with a JSON array of future flights filtered by the requested state.
- [ ] IF the `state` query parameter value is not a supported enum value, THEN the system SHALL return HTTP 400 with a JSON error.

### View Flight Status

- [ ] WHEN the client sends `GET /flights/{id}` for an existing flight, the system SHALL return HTTP 200 with the flight representation, including `state`.
- [ ] WHEN the client sends `GET /flights/{id}` for a non-existent flight, the system SHALL return HTTP 404 with a JSON error.
- [ ] WHILE a flight is not `CANCELLED` AND WHEN its `state` is refreshed on read, the system SHALL return `DONE` for flights past `launchDateTime` and SHALL return `CONFIRMED`/`SOLD_OUT` consistent with current booking counts.

> End of Feature Specification for 002-flight-management, last updated December 16, 2025.
