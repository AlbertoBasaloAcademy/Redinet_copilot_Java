# Astrobookings — Documentación y Estructura

Este documento explica cómo entender, compilar y ejecutar la solución ligera `astrobookings` y describe la estructura de carpetas del proyecto.

**Resumen de la solución**
- Es una pequeña aplicación Java que expone un endpoint HTTP (`/rockets`) usando el `com.sun.net.httpserver.HttpServer` del JDK.
- Permite crear (POST) objetos `Rocket` en memoria (repositorio en memoria).
- Incluye un ejemplo minimal (`MinimalApp`) para serializar un `Rocket` con Jackson.

**Pasos para compilar y ejecutar (bash / Windows WSL / Git Bash)**
- Compilar:

```bash
./mvnw -DskipTests clean package
```

- Ejecutar usando el plugin `exec` de Maven (recomendado para ejecutar la clase `main` directamente):

```bash
./mvnw exec:java -Dexec.mainClass=academy.aicode.astrobookings.AstrobookingsApplication
```

- Alternativa: ejecutar la clase compilada directamente (después de `package`), ajustando el classpath si hace falta:

```bash
java -cp target/classes:$(echo ~/.m2/repository/com/fasterxml/jackson/*/*.jar | tr ' ' ':') academy.aicode.astrobookings.AstrobookingsApplication
```

Nota: el `pom.xml` contiene referencias a `com.astrobookings.AstroBookingsApp` en las configuraciones de los plugins; para ejecutar con Maven se usa el parámetro `-Dexec.mainClass` mostrado arriba.

**Endpoint principal**
- `POST /rockets` — crea un `Rocket` a partir de un JSON en el body.
  - Request Content-Type: `application/json`
  - Validaciones:
    - `name` es obligatorio y no vacío
    - `capacity` debe estar entre 1 y 10
  - Ejemplo CURL:

```bash
curl -X POST http://localhost:8080/rockets \
  -H "Content-Type: application/json" \
  -d '{"name":"Falcon","capacity":5,"speed":12345.67}'
```

**Estructura de carpetas (resumen)**
- `src/main/java` — Código principal
  - `academy.aicode.astrobookings` — paquete raíz
    - `AstrobookingsApplication.java` — clase `main` que arranca el servidor HTTP
    - `presentation` — handlers HTTP
      - `BaseHandler.java` — utilidades comunes y manejo de respuestas
      - `RocketHandler.java` — handler para `/rockets`
    - `persistence` — almacenamiento en memoria
      - `RocketRepository.java` — repositorio en memoria
      - `models` — modelos de dominio
        - `Rocket.java` — modelo `Rocket` (id, name, capacity, speed)
- `src/minimal/java` — ejemplo minimal que muestra serialización con Jackson
- `src/test/java` — pruebas (si las hay)
- `resources/application.properties` — propiedades de la aplicación (actualmente vacías)

**Descripción breve de las clases clave**
- `AstrobookingsApplication` — inicializa `HttpServer` en el puerto `8080` y registra el contexto `/rockets`.
- `RocketHandler` — procesa sólo peticiones `POST`, valida el `Rocket` y lo guarda en `RocketRepository`.
- `RocketRepository` — almacenamiento sencillo en memoria usando `HashMap`.
- `BaseHandler` — contiene utilidades JSON (`ObjectMapper`) y métodos para enviar respuestas HTTP.
- `MinimalApp` — ejemplo independiente para despliegue rápido/depuración.

**Siguientes pasos recomendados**
- Ajustar `pom.xml` para un `mainClass` coherente si desea empaquetar un JAR ejecutable.
- Añadir persistencia real (BD) si se requieren datos persistentes.
- Añadir pruebas integradas para el handler HTTP.

---
Generado por la actividad de documentación. Si quieres que también cree un `README.md` más corto o que arregle `pom.xml` para que el `maven-shade-plugin` apunte a la clase correcta, dime y lo hago.
