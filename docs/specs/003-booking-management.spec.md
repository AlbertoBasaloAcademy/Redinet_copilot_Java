# 003-booking-management Specification

## 1. 👔 Problem Specification

AstroBookings SHALL expose a minimal REST API to create and retrieve bookings for flights, calculating booking prices via discount rules and enforcing that bookings can only be created for eligible flights.

### Create Booking

- **As a** API client
- **I want to** create a booking for a flight with passenger information
- **So that** a seat is reserved and the final price is calculated by business rules

### List Bookings for a Flight

- **As a** API client
- **I want to** list bookings for a specific flight
- **So that** I can see how many seats are booked and who is travelling

### View Booking Details

- **As a** API client
- **I want to** retrieve a booking by identifier
- **So that** I can confirm passenger and pricing details

## 2. 🧑‍💻 Solution Overview

Implement a layered, in-memory booking feature using JDK HTTP server handlers (presentation), a service for validation and pricing rules (business), and an in-memory repository (persistence). The booking service will consult flight and rocket information to enforce capacity/state rules and to compute discounts.

### Data Models

**Booking (entity)**

- `id` (string, server-generated, read-only)
- `flightId` (string, required)
- `passengerName` (string, required, non-blank)
- `passengerDocument` (string, required, non-blank)
- `finalPrice` (number, server-calculated, read-only)
- `discountPercent` (integer, server-calculated, read-only, one of `0`, `10`, `30`)
- `createdAt` (string, server-generated, ISO-8601, read-only)

**CreateBookingRequest (input DTO)**

- `flightId`, `passengerName`, `passengerDocument`

**BookingResponse (output DTO)**

- `id`, `flightId`, `passengerName`, `passengerDocument`, `finalPrice`, `discountPercent`, `createdAt`

### Software Components

**Presentation layer**

- `BookingHandler` exposes HTTP endpoints under `/bookings`:
  - `POST /bookings` creates a booking
  - `GET /bookings` lists bookings; MUST accept query `flightId` to filter to a flight
  - `GET /bookings/{id}` returns a booking by id

**Business layer**

- `BookingService` validates requests, enforces eligibility rules, computes final price, orchestrates persistence, and triggers flight state refresh.
- `FlightService` (or equivalent) is used to:
  - validate that `flightId` exists
  - read the flight `basePrice`, `launchDateTime`, and `state`
  - refresh the flight state after booking creation
- `RocketService` (or equivalent) is used to obtain rocket capacity for a flight, when needed by discount/state rules.

**Persistence layer**

- `BookingRepository` stores bookings in memory using Java collections and generates ids.

### User Interface

REST API (JSON over HTTP):

- **Create**: `POST /bookings`
  - Request body example:
    - `{ "flightId": "<flight-id>", "passengerName": "Ada Lovelace", "passengerDocument": "P123456" }`
  - Response body example:
    - `{ "id": "<generated>", "flightId": "<flight-id>", "passengerName": "Ada Lovelace", "passengerDocument": "P123456", "finalPrice": 900.0, "discountPercent": 10, "createdAt": "2025-12-16T12:00:00Z" }`

- **List by flight**: `GET /bookings?flightId=<flight-id>`
  - Response body example: `[{ ... }, { ... }]`

- **Get by id**: `GET /bookings/{id}`
  - Response body example: `{ ... }`

### Aspects

- **Validation**:
  - reject missing/blank passenger fields
  - reject `flightId` that does not exist
  - reject booking creation if the flight state is `SOLD_OUT` or `CANCELLED`
- **Pricing / discounts** (single discount per booking; precedence rules):
  - compute price from the flight `basePrice`
  - compute discount based on current booking count for the flight and the flight `minimumPassengers` and rocket capacity:
    - apply `0%` for the last seat booking (no discount)
    - apply `30%` for the booking that makes the flight reach `minimumPassengers` (one before minimum rule as defined by PRD)
    - apply `10%` for other bookings
- **State changes**:
  - after persisting a booking, refresh the flight state so that `CONFIRMED` and `SOLD_OUT` can be reached as seats are booked
  - log notifications for state changes and simulated payment/refund actions per PRD (TR3)
- **Error handling**: return JSON error payloads with appropriate HTTP status codes (400/404/405/409).
- **Security**: none (no authentication/authorization) as per TR1.
- **Performance**: in-memory collections; expected small dataset.

## 3. 🧑‍⚖️ Acceptance Criteria

### Create Booking

- [ ] WHEN the client sends `POST /bookings` with a valid payload for an eligible flight, the system SHALL create the booking and return HTTP 201 with the created booking including a server-generated `id`, `finalPrice`, `discountPercent`, and `createdAt`.
- [ ] IF the payload is missing `flightId` OR `passengerName` OR `passengerDocument`, THEN the system SHALL return HTTP 400 with a JSON error describing the validation failure.
- [ ] IF the referenced flight is `SOLD_OUT` OR `CANCELLED`, THEN the system SHALL return HTTP 409 with a JSON error describing the invalid state.

### List Bookings for a Flight

- [ ] WHEN the client sends `GET /bookings?flightId=<flight-id>`, the system SHALL return HTTP 200 with a JSON array of bookings for that flight (which MAY be empty).
- [ ] IF the client omits `flightId` in `GET /bookings`, THEN the system SHALL return HTTP 400 with a JSON error.
- [ ] WHEN the client sends an unsupported HTTP method to `/bookings`, the system SHALL return HTTP 405 with a JSON error.

### View Booking Details

- [ ] WHEN the client sends `GET /bookings/{id}` for an existing booking, the system SHALL return HTTP 200 with the booking representation.
- [ ] WHEN the client sends `GET /bookings/{id}` for a non-existent booking, the system SHALL return HTTP 404 with a JSON error.
- [ ] WHILE creating a booking AND WHEN the booking is persisted, the system SHALL refresh the referenced flight state so that `CONFIRMED` and `SOLD_OUT` can be reached based on current booking counts.

> End of Feature Specification for 003-booking-management, last updated December 16, 2025.
