---
description: 'JDK HTTP Server (jdk.httpserver) guidelines for REST handlers'
applyTo: '**/*.java'
---

# JDK HTTP Server (jdk.httpserver)

These guidelines apply to the built-in JDK HTTP server (`com.sun.net.httpserver.HttpServer`) used by AstroBookings.

## Architectural rules

- Keep HTTP concerns in `presentation` (handlers): routing, method checks, status codes, headers, parsing/formatting JSON.
- Keep business rules in `business` (services): validation, invariants, orchestration.
- Keep data access in `persistence` (repositories): in-memory collections, id generation, basic persistence operations.
- Handlers must not reach into repositories directly; go through services.

## Server setup

- Prefer a single `HttpServer` instance per application.
- Register one context per resource root (e.g., `/rockets`, `/flights`, `/bookings`).
- Configure an `Executor` before calling `start()`.
  - Use a bounded thread pool for predictable behavior.

## Handler responsibilities

- Validate and normalize:
  - HTTP method (`GET`, `POST`, `PUT`, `DELETE`…)
  - Path segments and IDs
  - Query parameters
  - `Content-Type` / `Accept` as needed for JSON APIs
- Convert:
  - Request body JSON → DTO
  - Domain/DTO → response JSON
- Map errors consistently:
  - Bad input → `400`
  - Not found → `404`
  - Conflict/invalid state → `409`
  - Unexpected errors → `500` (log details, keep response generic)

## Request parsing

- Read request bodies using a fixed charset (typically UTF-8).
- Fail fast if a request body is required but empty.
- Avoid reading the body multiple times.

## Response writing

- Always set:
  - Status code
  - `Content-Type: application/json; charset=utf-8` for JSON responses
- Always close response streams.
- Do not write a body for `204` responses.

## Routing and path matching

- JDK HttpServer routes by “longest matching prefix” of the request path.
- Be careful with context paths that don’t end with `/`.
  - Example: a context registered as `/rockets` may match `/rockets123` unless you guard for exact segments.
- Prefer normalizing paths and then interpreting segments in the handler.

## Threading

- A handler can be invoked concurrently.
- Do not store request-specific data in fields.
- Services/repositories must be safe for concurrent access if the executor has multiple threads.
  - If concurrency is enabled, use thread-safe structures or synchronize repository operations.

## HTTP exchange lifecycle

- Ensure the handler always completes the exchange:
  - Send headers once.
  - Write body (if any).
  - Close the response stream.
- On exceptions:
  - Log details.
  - Return a safe `500` JSON error.

## Minimal REST conventions

- Use nouns for resources: `/rockets`, `/flights`, `/bookings`.
- Use plural resource collections and `/{id}` for single resources.
- Prefer JSON request/response payloads.

## Security notes (training scope)

- Do not implement authentication/authorization unless explicitly requested.
- Validate sizes for inputs that can grow (body, arrays) to avoid memory pressure.
