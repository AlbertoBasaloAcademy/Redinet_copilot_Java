# Redinet_copilot_Java
Edición Java del curso de Copilot


## Workflow de desarrollo y ejecución

```bash
# Compilar
mvn clean compile

# Empaquetar
mvn clean package

# Empaquetar (sin ejecutar tests)
# Recomendado mientras desarrolla: usa el perfil `skip-tests` o la propiedad `-DskipTests`
# Con perfil:
./mvnw -Pskip-tests clean package
# O con la propiedad:
./mvnw -DskipTests clean package

# Ejecutar
java -jar target/astrobookings-1.0-SNAPSHOT.jar

# Server: http://localhost:8080
```