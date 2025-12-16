---
description: 'java.util.logging (JUL) usage guidelines'
applyTo: '**/*.java'
---

# java.util.logging (JUL)

AstroBookings uses system logging to simulate integrations and record errors.

## Logger usage

- Use one logger per class:
  - `private static final Logger LOGGER = Logger.getLogger(MyClass.class.getName());`
- Align logger names with package/class names.

## Levels

- Use levels consistently:
  - `SEVERE`: unexpected failures, cannot continue
  - `WARNING`: recoverable issues, invalid inputs, external failures
  - `INFO`: high-level lifecycle and business events
  - `FINE`/`FINER`/`FINEST`: debug/trace (avoid in normal flows)

## Message construction

- Avoid expensive string building when the level may be disabled.
- Prefer parameterized or supplier-based messages where applicable.

## What to log

- Log enough to diagnose:
  - operation name
  - key identifiers (rocketId, bookingId)
  - outcome (created/updated/deleted)
- Do not log sensitive payloads.
- Do not log full stack traces to the client response; log them server-side.

## Error handling pattern

- On unexpected exceptions:
  - log with the exception: `LOGGER.log(Level.SEVERE, "message", ex)`
  - return a generic `500` error response

## Configuration

- Prefer default JDK configuration unless explicitly asked to customize.
- If customization is needed, do it centrally at application startup.

