---
description: 'Java Collections Framework usage guidelines'
applyTo: '**/*.java'
---

# Java Collections

AstroBookings persistence is in-memory using Java collections.

## Prefer interfaces

- Use `List`, `Map`, `Set` in APIs; choose concrete implementations internally.
- Keep collection fields private and expose behavior instead of exposing mutable collections.

## Immutability

- Prefer immutable values and defensive copies where needed.
- Use `List.of(...)`, `Map.of(...)`, and `Set.of(...)` for fixed data.

## IDs and lookups

- Prefer `Map<Id, Entity>` for repository storage and O(1) lookup.
- Keep ID generation and uniqueness checks centralized (repository or service).

## Thread-safety

- Assume handlers can run concurrently if the HTTP server uses a thread pool.
- If repositories can be accessed concurrently:
  - use synchronization or `ConcurrentHashMap`
  - ensure compound operations are atomic (check-then-act)

## Streams

- Use streams for transformations and filtering when it improves clarity.
- Avoid streams when a simple loop is clearer.

## Null handling

- Prefer empty collections over `null`.
- Prefer `Optional<T>` for “maybe” values instead of `null`.

