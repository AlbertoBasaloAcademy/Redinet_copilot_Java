# Astro Bookings Briefing

AstroBookings será un API para una aplicación de reservas para viajes espaciales. Gestiona cohetes, vuelos y reservas con control de capacidad, precios y estados.

## Funcionalidades clave

- Listar y crear `Rocket` con validación
- Listar `Flight` futuros (filtro estado)
- Crear `Flight` con fecha de lanzamiento futura y precio > 0 → `SCHEDULED`
- Crear `Booking` con descuentos 
- Consultar `Booking` por vuelo/pasajero
- Cancelar `Flight` → `CANCELLED` 

### Entidades

- `Rocket`
    - Nombre (obligatorio), 
    - Capacidad (máx. 10), 
    - Velocidad (opcional), 
    - Alcance (opcional: LEO, MOON, MARS)

- `Flight`
    - Fecha de lanzamiento futura, 
    - Precio base, Mínimo de pasajeros (5 default)
    - Estados: `SCHEDULED`, `CONFIRMED`, `SOLD_OUT`, `CANCELLED` , `DONE` , ambio automático según reservas

- `Booking`
    - Pasajero (nombre, email),  
    - Precio final, cálculo con descuentos

### Lógica

- Reservar solo si vuelo no lleno ni cancelado
- No superar capacidad cohete → `SOLD_OUT` al límite
- Alcanza mínimo pasajeros → `CONFIRMED` + notificar
- Falta 1 semana sin mínimo → `CANCELLED` + notificar + devolver pago
- Completar vuelo → `DONE`

- Descuentos (precedencia, solo uno)
    1. Última plaza → 0%
    2. Falta 1 para completar mínimo → 30%
    3. Resto → 10%

## Requisitos técnicos

- Se usará como demostración y ejercicio en talleres de formación
  - No se necesita persistencia en base de datos, ni seguridad
- Es una aplicación backend con API RESTful
  - Java, Spring Boot recomendado
  - Usar las mínimas dependencias posibles  

