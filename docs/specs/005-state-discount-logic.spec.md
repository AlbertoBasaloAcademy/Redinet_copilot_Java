# 005-state-discount-logic Specification

## 1. 👔 Problem Specification

AstroBookings SHALL aplicar reglas de negocio para (a) la transición/derivación de estados de vuelos y (b) el cálculo de descuentos por reserva, usando almacenamiento en memoria y simulando notificaciones y reembolsos mediante logs.

### Derive Flight State Automatically

- **As a** API client
- **I want to** retrieve or list flights with their correct lifecycle state
- **So that** I can display accurate flight status without manual recalculation

### Apply Booking Discount Rules

- **As a** API client
- **I want to** create a booking and get its final price computed by discount rules
- **So that** pricing is consistent and business rules are enforced

### Trigger Notifications/Refunds on Rule-based Cancellation

- **As a** API client
- **I want to** observe that a flight is cancelled when it is too close to launch and minimum passengers are not met
- **So that** the system simulates notifications and refunds as logs

## 2. 🧑‍💻 Solution Overview

Implement the rules in the existing layered, in-memory architecture by centralizing state derivation in the business layer and using booking counts from repositories. No background scheduler is required; state refresh happens on relevant reads/writes.

### Data Models

**Flight (entity)**

- `id` (string, server-generated, read-only)
- `rocketId` (string, required)
- `launchDateTime` (instant/date-time, required)
- `minimumPassengers` (int, required)
- `basePrice` (decimal/double, required)
- `state` (string enum, server-controlled): `SCHEDULED`, `CONFIRMED`, `SOLD_OUT`, `CANCELLED`, `DONE`

**Booking (entity)**

- `id` (string, server-generated, read-only)
- `flightId` (string, required)
- `passengerName` (string, required)
- `passengerDocument` (string, required)
- `discountPercent` (int, derived; allowed values: `0`, `10`, `30`)
- `finalPrice` (decimal/double, derived)

**Booking pricing rule inputs (derived at runtime)**

- `capacity` (from Rocket)
- `bookedPassengersBefore` and `bookedPassengersAfter` (from BookingRepository count by flight)
- `minimumPassengers` (from Flight)

### Software Components

**Presentation layer**

- Existing endpoints remain the primary interface:
  - `POST /bookings` (booking creation triggers discount calc and state refresh)
  - `GET /flights` and `GET /flights/{id}` (reads trigger state refresh)
- No new endpoints are required.

**Business layer**

- `FlightService.refreshState(flightId)` (or equivalent) SHALL:
  - load the flight and associated rocket capacity
  - compute passenger count for the flight from `BookingRepository`
  - derive the flight state using the rules (see “Aspects”)
  - persist the derived state only if it changes
  - write log entries when transitioning to `CONFIRMED` or rule-based `CANCELLED`
  - be idempotent for repeated calls without underlying data changes

- `BookingService.create(...)` (or equivalent) SHALL:
  - validate the booking request
  - call `FlightService.refreshState(flightId)` before accepting the booking
  - reject creation if the flight state is `SOLD_OUT` or `CANCELLED`
  - compute `discountPercent` with precedence rules
  - compute `finalPrice = basePrice * (1 - discountPercent/100)`
  - persist the booking
  - call `FlightService.refreshState(flightId)` after persisting the booking

**Persistence layer**

- `BookingRepository` SHALL provide a way to count bookings by `flightId`.
- `FlightRepository` SHALL store and update the current flight state in-memory.
- `RocketRepository` SHALL provide capacity for a flight’s rocket.

### User Interface

REST API (JSON over HTTP) using existing resources:

- Create booking: `POST /bookings`
  - Response includes `discountPercent` and `finalPrice` (or equivalent fields) derived by business rules.
- Flight retrieval: `GET /flights/{id}`
  - Response includes the derived `state`.
- Flight listing: `GET /flights` (optional `state` filter)
  - Items include the derived `state`.

### Aspects

**State derivation rules (simplest deterministic order)**

For a given flight with `launchDateTime`, `minimumPassengers`, rocket `capacity`, and current `bookedPassengers`:

1. **DONE**: IF `now` is after `launchDateTime`, THEN the flight state SHALL be `DONE`.
2. **CANCELLED (rule-based)**: IF `now` is on/after (`launchDateTime` minus 7 days) AND `bookedPassengers` is less than `minimumPassengers`, THEN the flight state SHALL be `CANCELLED` and the system SHALL log simulated notifications/refunds.
3. **SOLD_OUT**: IF `bookedPassengers` equals `capacity`, THEN the flight state SHALL be `SOLD_OUT`.
4. **CONFIRMED**: IF `bookedPassengers` is greater than or equal to `minimumPassengers`, THEN the flight state SHALL be `CONFIRMED` and the system SHALL log a simulated notification.
5. **SCHEDULED**: OTHERWISE the flight state SHALL be `SCHEDULED`.

Notes:
- Manual cancellation (FR4/TR4) remains supported via the cancellation endpoint; rule-based cancellation is derived/applied during state refresh (no background job).
- State refresh MUST NOT re-open a flight once `CANCELLED` or `DONE`, except that `DONE` takes precedence after launch time.

**Discount rules (single discount with precedence)**

Given `bookedPassengersBefore` and `bookedPassengersAfter = bookedPassengersBefore + 1`:

1. **Last seat**: IF `bookedPassengersAfter` equals `capacity`, THEN the booking discount SHALL be `0%`.
2. **One before minimum**: IF `bookedPassengersBefore` equals (`minimumPassengers - 1`), THEN the booking discount SHALL be `30%`.
3. **Default**: OTHERWISE the booking discount SHALL be `10%`.

**Validation & error handling**

- Booking creation SHALL be rejected with HTTP 409 when flight is `SOLD_OUT` or `CANCELLED`.
- Invalid state query values SHALL return HTTP 400 with a JSON error.
- All simulated notifications/refunds SHALL be implemented as log entries (TR3).

## 3. 🧑‍⚖️ Acceptance Criteria

### Derive Flight State Automatically

- [ ] WHEN the client requests a flight via `GET /flights/{id}`, the system SHALL refresh and return the flight with a `state` consistent with the state derivation rules.
- [ ] WHEN the client requests future flights via `GET /flights`, the system SHALL refresh and return each listed flight with a `state` consistent with the state derivation rules.
- [ ] IF the client supplies an unsupported `state` filter value to `GET /flights?state=...`, THEN the system SHALL return HTTP 400 with a JSON error.

### Apply Booking Discount Rules

- [ ] WHEN the client creates a booking, the system SHALL compute exactly one discount percent using precedence (last seat `0%`, one-before-minimum `30%`, otherwise `10%`) and return it in the booking response.
- [ ] WHEN the client creates a booking, the system SHALL compute and return the final price as $\text{finalPrice} = \text{basePrice} \cdot (1 - \text{discountPercent}/100)$.
- [ ] IF the flight state is `SOLD_OUT` or `CANCELLED` at booking time, THEN the system SHALL reject `POST /bookings` with HTTP 409 and a JSON error.

### Trigger Notifications/Refunds on Rule-based Cancellation

- [ ] IF a flight is within 7 days of `launchDateTime` AND has fewer than `minimumPassengers`, THEN the system SHALL set `state = CANCELLED` during state refresh and SHALL write logs simulating notifications and refunds.
- [ ] WHEN a flight reaches `minimumPassengers` during booking creation, the system SHALL set `state = CONFIRMED` during refresh and SHALL write a log simulating notification.
- [ ] WHILE a flight is `CANCELLED`, the system SHALL reject new bookings for that flight with HTTP 409.

> End of Feature Specification for 005-state-discount-logic, last updated December 18, 2025.
