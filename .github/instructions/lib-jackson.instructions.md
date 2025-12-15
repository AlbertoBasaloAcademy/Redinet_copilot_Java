---
description: 'Jackson JSON (2.x) usage and best practices'
applyTo: '**/*.java'
---

# Jackson (JSON)

AstroBookings uses Jackson for JSON serialization/deserialization.

## Dependency and scope

- Keep Jackson usage in the presentation layer when possible (handlers and request/response DTOs).
- Do not leak persistence models directly over the wire unless explicitly desired.

## ObjectMapper usage

- Create the `ObjectMapper` once and reuse it.
  - Treat it as an application-level singleton.
- Prefer configuring the mapper at startup, not per-request.
- Use `ObjectReader`/`ObjectWriter` when you need per-call configuration without mutating the base mapper.

## DTO and model guidelines

- Prefer Java records for request/response DTOs.
- Keep DTOs small and stable; separate them from domain models if the domain contains internal-only fields.
- Prefer explicit types over raw `Map<String,Object>` for request bodies.

## Deserialization safety

- Validate input beyond “JSON is well-formed”:
  - required fields present
  - ranges and formats
  - enum values
- Prefer failing fast on invalid payloads and returning `400` with a concise error payload.
- If compatibility requires accepting extra fields, disable unknown-property failure:
  - `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`
  - Only do this intentionally; otherwise keep strict.

## Serialization rules

- Keep output deterministic:
  - stable property names
  - consistent date/time formats (if introduced)
- Do not serialize stack traces or internal exception objects.

## Common patterns

- Parse request body:
  - `mapper.readValue(StringOrStream, DtoClass.class)`
- Serialize response:
  - `mapper.writeValueAsString(responseDto)`
- Generic collections:
  - use `TypeReference<T>` for correct generic typing.

## Performance tips

- Reuse the `ObjectMapper`.
- Avoid converting POJO → JSON → POJO for mapping; use explicit mapping or `convertValue` if appropriate.
- For large payloads, prefer streaming APIs (`JsonParser`/`JsonGenerator`) only when necessary.

## Error handling

- Catch `JsonProcessingException` and return `400`.
- Log at a useful level with context (endpoint + correlation/id if available), but do not log full sensitive payloads.

