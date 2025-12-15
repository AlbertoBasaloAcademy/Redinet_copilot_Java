---
description: 'JUnit 5 (Jupiter) test guidelines'
applyTo: '**/*Test*.java'
---

# JUnit 5 (JUnit Jupiter)

AstroBookings uses JUnit 5 for unit tests.

## Test scope

- Prefer unit tests for business services and repository logic.
- Avoid integration-style tests (network ports, file system, time, concurrency) unless explicitly requested.

## Structure

- Use Arrange / Act / Assert.
- Keep one behavior per test.
- Prefer descriptive test names.

## Core annotations

- Use `@Test` for tests.
- Use `@BeforeEach` / `@AfterEach` for per-test setup/teardown.
- Use `@Nested` to group related scenarios.
- Use `@ParameterizedTest` with `@ValueSource`, `@CsvSource`, or `@MethodSource` for input matrices.

## Assertions

- Prefer `org.junit.jupiter.api.Assertions`:
  - `assertEquals`, `assertTrue`, `assertThrows`, `assertAll`
- For exceptions:
  - assert the exception type and the relevant part of the message (avoid brittle full-message comparisons).

## Determinism

- No reliance on execution order.
- No shared mutable static state.
- Avoid sleeps and timing assumptions.

## Test data

- Prefer small, explicit inputs.
- Use helpers/builders only when they reduce repetition without obscuring intent.

