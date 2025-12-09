# Astrobookings — Documentación y Estructura

Este documento explica cómo entender, compilar y ejecutar la solución ligera `astrobookings` y describe la estructura de carpetas del proyecto.

**Resumen de la solución**
- Aplicación Java que expone un servidor HTTP embebido (`com.sun.net.httpserver.HttpServer`) en el puerto `8080`.
- Maneja el recurso `/rockets` con operaciones de creación y consulta en memoria.
- Incluye un ejemplo mínimo (`MinimalApp`) para demostrar la serialización con Jackson.

**Pasos para compilar y ejecutar (bash / Windows WSL / Git Bash)**
- Compilar:

```bash
./mvnw -DskipTests clean package
```

- Ejecutar usando el plugin `exec` de Maven (la `mainClass` está configurada en el `pom.xml`):

```bash
./mvnw exec:java
```

- Alternativa: ejecutar la clase compilada directamente (después de `package`):

```bash
java -cp target/classes:$(echo ~/.m2/repository/com/fasterxml/jackson/*/*.jar | tr ' ' ':') academy.aicode.astrobookings.AstrobookingsApplication
```

Nota: el `pom.xml` ya apunta a `academy.aicode.astrobookings.AstrobookingsApplication` como `mainClass` para los plugins `exec`, `jar` y `shade`.

**Endpoints**
- `POST /rockets` — crea un `Rocket` a partir de un JSON en el body.
  - Request Content-Type: `application/json`
  - Respuestas:
    - `201 Created` con el JSON del cohete guardado cuando la creación es válida.
    - `400 Bad Request` si el JSON o la validación fallan.
  - Validaciones aplicadas en la capa de negocio (`RocketService`):
    - `name` es obligatorio y no puede estar vacío.
    - `capacity` debe estar entre `1` y `10`.
  - Ejemplo CURL:

```bash
curl -X POST http://localhost:8080/rockets \
  -H "Content-Type: application/json" \
  -d '{"name":"Falcon","capacity":5,"speed":12345.67}'
```

- `GET /rockets` — devuelve la lista de todos los cohetes (200).
- `GET /rockets/{id}` — devuelve el cohete con ese `id` (200) o `404 Not Found` si no existe.

**Estructura de carpetas (resumen)**
- `src/main/java` — Código principal
  - `academy.aicode.astrobookings` — paquete raíz
    - `AstrobookingsApplication.java` — clase `main` que arranca el servidor HTTP (puerto `8080`).
    - `presentation` — handlers HTTP
      - `BaseHandler.java` — utilidades comunes y manejo de respuestas (configura `ObjectMapper`).
      - `RocketHandler.java` — handler para `/rockets` (soporta `GET` y `POST`).
    - `business` — lógica de negocio
      - `RocketService.java` — validaciones y coordinación con el repositorio.
    - `persistence` — almacenamiento en memoria
      - `RocketRepository.java` — repositorio en memoria que genera ids (`r0`, `r1`, ...).
      - `models` — modelos de dominio
        - `Rocket.java` — modelo `Rocket` (`id: String`, `name: String`, `capacity: int`, `speed: Double`).
- `src/minimal/java` — ejemplo mínimo (`MinimalApp`) que muestra serialización con Jackson.
- `src/test/java` — pruebas (JUnit 5).
- `src/main/resources` — recursos; `application.properties` presente (actualmente sin configuración significativa).

**Descripción breve de las clases clave**
- `AstrobookingsApplication` — crea un `HttpServer` en el puerto `8080` y registra el contexto `/rockets`.
- `RocketHandler` — procesa `POST` (creación) y `GET` (búsqueda/listado) del recurso `/rockets`.
- `RocketService` — encapsula validaciones (nombre obligatorio, capacidad entre 1 y 10) y delega al repositorio.
- `RocketRepository` — almacenamiento en memoria usando `HashMap`; asigna ids automáticos (`r0`, `r1`, ...).
- `BaseHandler` — proporciona un `ObjectMapper` configurado con `JavaTimeModule` y utilidades para enviar respuestas JSON (`Content-Type: application/json`).
- `MinimalApp` — ejemplo independiente para depuración/serialización con Jackson.

**Notas técnicas y recomendaciones**
- El `pom.xml` ya incluye configuración para ejecutar la aplicación con `exec:java` y para crear un JAR ejecutable (`maven-shade-plugin`) apuntando a la clase `AstrobookingsApplication`.
- Si se desea persistencia real, sustituir `RocketRepository` por una implementación conectada a BD y ajustar `RocketService`.
- Añadir pruebas de integración para el handler HTTP si se requiere mayor cobertura.

---
Generado/actualizado para reflejar el estado actual del código fuente.
