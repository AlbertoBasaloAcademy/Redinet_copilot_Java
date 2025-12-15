---
description: 'Apache Maven build and dependency management guidelines'
applyTo: '**/pom.xml'
---

# Maven

AstroBookings uses Maven for builds and dependency management.

## Use the wrapper

- Prefer the Maven Wrapper to ensure consistent Maven versions.
- On Windows use: `mvnw.cmd <goals>`.
- Typical commands:
  - `mvnw.cmd clean compile`
  - `mvnw.cmd -DskipTests package` (compile/package without running tests)

## Lifecycle basics

- `validate` → `compile` → `test` → `package` → `verify` → `install`.
- Use the smallest goal that satisfies the task (e.g., `compile` when you only need compilation).

## POM conventions

- Keep versions centralized:
  - use `<properties>` for dependency/plugin versions.
- Prefer minimal dependencies; do not add frameworks that contradict the project goal (no Spring Boot).
- Pin plugin versions (avoid relying on transitive defaults).

## Dependency hygiene

- Avoid redundant dependencies.
- Avoid multiple versions of the same library.
- Use exclusions deliberately and document why.

## Reproducibility

- Prefer stable, deterministic builds:
  - avoid snapshots unless necessary
  - keep plugin versions pinned

