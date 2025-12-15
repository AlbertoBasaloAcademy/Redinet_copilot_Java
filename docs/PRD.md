# AstroBookings P.R.D.

Product Requirements Document for AstroBookings

## Overview

**AstroBookings** aims to provide a backend API for managing space travel bookings, including rockets, flights, and reservations, with business logic for capacity, pricing, and state transitions. The system is designed for demonstration and training purposes, not for production use.

### Goals

- Allow creation and management of rockets, flights, and bookings with business validation.
- Demonstrate layered Java backend architecture for educational workshops.

### Context diagram

```mermaid
C4Context
      Person(customer, "Customer", "User who books space flights")
      System_Boundary(system, "AstroBookings API") {
        Container(api, "REST API", "Spring Boot App", "Handles all business logic for rockets, flights, and bookings")
      }
      Person(customer) -> Container(api)
```

## Functional Requirements

### FR1 Rocket Management

Allow creation, listing, and search of rockets with validation on name (required), capacity (max 10), and optional speed/range (LEO, MOON, MARS).

### FR2 Flight Management

Enable creation of flights with future launch dates, base price > 0, and automatic state transitions (SCHEDULED, CONFIRMED, SOLD_OUT, CANCELLED, DONE) based on bookings and time. List/filter future flights by state.

### FR3 Booking Management

Allow creation of bookings for flights, with passenger info and price calculation (discounts applied by rules). Bookings trigger state changes (e.g., CONFIRMED when minimum reached, SOLD_OUT at capacity). Bookings can only be made if flight is not SOLD_OUT or CANCELLED.

### FR4 Flight Cancellation

Support manual cancellation of flights, updating state to CANCELLED, simulating notifications and refunds via logs.

### FR5 Automatic State and Discount Logic

Implement business rules for state transitions and discounts:
- Confirm flight when minimum passengers reached, notify via log.
- Mark as SOLD_OUT at rocket capacity.
- Cancel flight 1 week before launch if minimum not reached, notify and refund via log.
- Apply only one discount per booking (precedence: last seat 0%, one before minimum 30%, others 10%).

## Technical Requirements

### TR1 No Database or Security

No persistent storage or authentication required; all data is in-memory for demonstration.

### TR2 RESTful API with Java & Spring Boot

Backend must expose a RESTful API using Java and Spring Boot, with minimal dependencies.

### TR3 Simulated Payments and Notifications

Payments and notifications are simulated using logs; no real integrations.

### TR4 Manual Flight Cancellation

Flight cancellation is performed manually via API, not automatically.

> End of PRD for AstroBookings, last updated on December 15, 2025.
