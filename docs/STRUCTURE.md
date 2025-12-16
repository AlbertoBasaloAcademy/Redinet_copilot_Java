# AstroBookings STRUCTURE

Structure Document for AstroBookings

## Overview

**AstroBookings** is a layered (n-tier) architecture, designed for maintainability, separation of concerns, and educational clarity, using Java with native JDK HTTP server for minimal dependencies and in-memory data management.

## Architecture

The system follows a classic layered architecture with clear separation of concerns:

- **Presentation Layer**: HTTP handlers using JDK's built-in HTTP server for REST endpoints.
- **Business Layer**: Service classes containing business logic, validation, and orchestration.
- **Persistence Layer**: In-memory repositories using Java collections for data storage.

Key principles:
- Clear separation of concerns between layers.
- Minimal dependencies (only Jackson for JSON, JUnit for testing).
- No Spring Boot, database, or security (for demo/training purposes).
- All state and logic are managed in-memory using Java collections.
- Simulated external integrations (logs for notifications/payments).

### Folder Structure

Main folders and their purposes:

```
src/main/java/academy/aicode/astrobookings/
├── AstrobookingsApplication.java    # Main entry point, HTTP server setup
├── presentation/                    # HTTP handlers for REST endpoints
│   ├── BaseHandler.java            # Common handler functionality
│   ├── RocketHandler.java          # Rocket CRUD operations
│   ├── FlightHandler.java          # Flight management (to be implemented)
│   └── BookingHandler.java         # Booking operations (to be implemented)
├── business/                        # Business logic and validation
│   ├── RocketService.java          # Rocket business operations
│   ├── FlightService.java          # Flight state management (to be implemented)
│   └── BookingService.java         # Booking logic with discounts (to be implemented)
└── persistence/                     # Data access layer
    ├── RocketRepository.java       # In-memory rocket storage
    ├── FlightRepository.java       # In-memory flight storage (to be implemented)
    ├── BookingRepository.java      # In-memory booking storage (to be implemented)
    └── models/                      # Domain entities
        ├── Rocket.java             # Rocket entity
        ├── Flight.java             # Flight entity (to be implemented)
        └── Booking.java            # Booking entity (to be implemented)
```

### Rocket Management

This project includes a simple Rocket Management feature implementing REST endpoints and validation rules.

- **Endpoints:**
    - `POST /rockets` — create a new rocket (returns 201 and the created resource).
    - `GET /rockets` — list rockets (supports optional `name` query filter).
    - `GET /rockets/{id}` — fetch a rocket by id (returns 200 or 404).

- **Validation rules:**
    - `name` is required and must be non-blank.
    - `capacity` is required and must be an integer between 1 and 10.
    - `range` is optional and must be one of `LEO`, `MOON`, `MARS`.
    - `speed` is optional (double).

- **Data transfer objects (DTOs):**
    - Request DTO: `src/main/java/academy/aicode/astrobookings/presentation/dto/CreateRocketRequest.java`
    - Response DTO: `src/main/java/academy/aicode/astrobookings/presentation/dto/RocketResponse.java`
    - Error schema: `src/main/java/academy/aicode/astrobookings/presentation/dto/ErrorResponse.java`

- **Model and enums:**
    - Domain model: `src/main/java/academy/aicode/astrobookings/persistence/models/Rocket.java` (now contains `Range range` and `Double speed`).
    - Range enum: `src/main/java/academy/aicode/astrobookings/persistence/models/Range.java` (`LEO`, `MOON`, `MARS`).

- **IDs and errors:**
    - Rocket IDs are generated as UUID strings via `UUID.randomUUID().toString()`.
    - Error responses follow the structure: `{ "error":"...", "code":"...", "details": {"field":"...","message":"..."} }` and use appropriate HTTP statuses (400/404/405/201/200/500).

- **Logging:** Uses `java.util.logging` for service and handler logging (no extra dependencies).

### Components diagram

```mermaid
flowchart TD
    Customer["Customer<br/>User who books space flights"]
    
    subgraph API["AstroBookings API"]
        Handlers["HTTP Handlers<br/>JDK HTTP Server<br/>Handle REST requests"]
        Services["Business Services<br/>Java<br/>Business logic and validation"]
        Repos["Repositories<br/>Java Collections<br/>In-memory data storage"]
    end
    
    Logs["Log Files<br/>System Logs<br/>Simulated notifications and payments"]
    
    Customer --> Handlers
    Handlers --> Services
    Services --> Repos
    Services --> Logs
```

## Development

### Technical Stack 

- **Java 21**: Core programming language
- **JDK HTTP Server**: Built-in web server (no Spring Boot)
- **Jackson 2.15.2**: JSON serialization/deserialization
- **JUnit 5.10.0**: Unit testing framework
- **Maven**: Build and dependency management
- **Java Collections**: In-memory data storage
- **System Logging**: Simulated external integrations

### Development Workflow

```bash
# Build and run
mvn clean compile
mvn exec:java -Dexec.mainClass="academy.aicode.astrobookings.AstrobookingsApplication"

# Testing
mvn test

# Development guidelines
- Feature branches from main
- Code reviews for all changes
- Manual testing with HTTP client (Postman, curl)
- No CI/CD required (training environment)
- Documentation updates in /docs
```

> End of STRUCTURE document for AstroBookings, last updated on December 15, 2025.