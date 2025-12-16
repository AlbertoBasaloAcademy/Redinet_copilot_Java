# 001-rocket-management Specification

## 1. 👔 Problem Specification

AstroBookings SHALL expose a minimal REST API to create, list, and search rockets, applying business validation rules for demo/training purposes.

### Create Rocket

- **As a** API client
- **I want to** create a rocket with a name and capacity
- **So that** flights can later be planned using a valid rocket

### List Rockets

- **As a** API client
- **I want to** list all rockets
- **So that** I can select an existing rocket for further operations

### Search Rocket

- **As a** API client
- **I want to** retrieve a rocket by identifier (and optionally search by name)
- **So that** I can confirm rocket details or locate a specific rocket

## 2. 🧑‍💻 Solution Overview

Implement a layered, in-memory rocket feature using JDK HTTP server handlers (presentation), a service for validation (business), and an in-memory repository (persistence).

### Data Models

**Rocket (entity)**

- `id` (string, server-generated, read-only)
- `name` (string, required, non-blank)
- `capacity` (integer, required, $1..10$)
- `range` (string enum, optional): one of `LEO`, `MOON`, `MARS`
- `speed` (number, optional)

**CreateRocketRequest (input DTO)**

- `name`, `capacity`, optional `range`, optional `speed`

**RocketResponse (output DTO)**

- `id`, `name`, `capacity`, optional `range`, optional `speed`

### Software Components

**Presentation layer**

- `RocketHandler` exposes HTTP endpoints under `/rockets`:
  - `POST /rockets` creates a rocket
  - `GET /rockets` lists rockets; MAY accept optional query `name` to filter
  - `GET /rockets/{id}` returns a rocket by id

**Business layer**

- `RocketService` validates requests and orchestrates persistence.

**Persistence layer**

- `RocketRepository` stores rockets in memory using Java collections and generates ids.

### User Interface

REST API (JSON over HTTP):

- **Create**: `POST /rockets`
  - Request body example:
    - `{ "name": "Falcon", "capacity": 10, "range": "LEO", "speed": 7.8 }`
  - Response body example:
    - `{ "id": "<generated>", "name": "Falcon", "capacity": 10, "range": "LEO", "speed": 7.8 }`

- **List**: `GET /rockets`
  - Response body example: `[{ ... }, { ... }]`

- **Get by id**: `GET /rockets/{id}`
  - Response body example: `{ ... }`

### Aspects

- **Validation**: enforce required name and capacity bounds; reject invalid enum values for `range`.
- **Error handling**: return JSON error payloads with appropriate HTTP status codes (400/404/405).
- **Monitoring/Logging**: log create operations and validation failures at an appropriate level.
- **Security**: none (no authentication/authorization) as per TR1.
- **Performance**: in-memory collections; expected small dataset.

## 3. 🧑‍⚖️ Acceptance Criteria

### Create Rocket

- [ ] WHEN the client sends `POST /rockets` with a valid rocket payload, the system SHALL create the rocket and return HTTP 201 with the created rocket (including a server-generated `id`).
- [ ] IF the request payload is missing `name` or `name` is blank, THEN the system SHALL return HTTP 400 with a JSON error describing the validation failure.
- [ ] IF the request payload has `capacity` outside $1..10$, THEN the system SHALL return HTTP 400 with a JSON error describing the validation failure.

### List Rockets

- [ ] WHEN the client sends `GET /rockets`, the system SHALL return HTTP 200 with a JSON array of rockets (which MAY be empty).
- [ ] WHERE the client provides a `name` query parameter in `GET /rockets?name=<value>`, the system SHALL return HTTP 200 with a JSON array containing only rockets that match the provided name filter.
- [ ] WHEN the client sends an unsupported HTTP method to `/rockets`, the system SHALL return HTTP 405 with a JSON error.

### Search Rocket

- [ ] WHEN the client sends `GET /rockets/{id}` for an existing rocket, the system SHALL return HTTP 200 with the rocket representation.
- [ ] WHEN the client sends `GET /rockets/{id}` for a non-existent rocket, the system SHALL return HTTP 404 with a JSON error.
- [ ] IF the `{id}` path parameter is missing or blank, THEN the system SHALL return HTTP 400 with a JSON error.

> End of Feature Specification for 001-rocket-management, last updated December 16, 2025.
