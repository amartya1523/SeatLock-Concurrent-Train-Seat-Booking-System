# SeatLock - Concurrent Train Seat Booking System

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-149ECA?logo=react&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

SeatLock is a production-minded train seat reservation application built to stay correct under contention. It presents one coach with 20 seats while safely handling 100 simultaneous customers, two-minute temporary holds, booking confirmation, cancellation, expiry, and live availability.

## Highlights

- 20-seat interactive coach with Available, Selected, Held, and Booked states
- Transactional two-minute seat holds with a real-time countdown
- Race-safe confirmation using PostgreSQL row locks and unique constraints
- Automatic expiry cleanup plus request-time expiry checks
- Responsive React + TypeScript interface with clear success and error feedback
- Booking history API, health endpoint, Swagger UI, and Postman collection
- Flyway-managed schema, Docker Compose, and a 100-customer concurrency test

## Architecture

```text
React client (port 5173)
        | REST / JSON
        v
Spring Boot API (port 8080)
  Controller -> Service (@Transactional) -> Repository
        | JPA / Hibernate
        v
PostgreSQL 16 (host port 55432)
```

```text
SeatLock/
├── frontend/                 React 19, TypeScript, Vite, Nginx
├── backend/                  Java 17, Spring Boot, JPA, Flyway
├── docs/                     Postman collection and screenshots
├── docker-compose.yml        Complete local stack
├── Makefile                  Common development commands
└── README.md
```

## Run locally with Docker (recommended)

Prerequisites: Docker Desktop with Docker Compose v2 and ports `5173`, `8080`, and `55432` available.

```bash
git clone https://github.com/amartya1523/SeatLock-Concurrent-Train-Seat-Booking-System..git
cd SeatLock-Concurrent-Train-Seat-Booking-System.
docker compose up --build
```

Open:

- Application: [http://localhost:5173](http://localhost:5173)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Health check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

Stop with `docker compose down`. Add `-v` to erase local booking data.

## Run services separately

Prerequisites: Java 17+, Node.js 20+, npm, and Docker or PostgreSQL 14+.

```bash
# Terminal 1: database
docker compose up -d database

# Terminal 2: API (the wrapper downloads Maven automatically)
cd backend
DB_URL=jdbc:postgresql://localhost:55432/seatlock ./mvnw spring-boot:run

# Terminal 3: client
cd frontend
npm install
npm run dev
```

Flyway creates the schema and seeds seats `1A` through `5D` on first startup. Copy `.env.example` to customize configuration.

## Booking flow

1. The client fetches all 20 seats from the backend.
2. A customer selects up to six seats and submits their details.
3. The API locks the requested database rows and creates a two-minute reservation.
4. The customer may confirm, cancel, or let the reservation expire.
5. Confirmation creates a durable booking and unique reference such as `SL-4A21F93B`.

Expired holds are treated as available immediately in every read and write path. A scheduled cleanup marks old reservations `EXPIRED` every 15 seconds for housekeeping; correctness does not depend on scheduler timing.

## Interface

![SeatLock booking interface](docs/screenshots/seatlock-home.png)

## How duplicate bookings are prevented

SeatLock uses complementary safeguards:

1. **Short database transactions.** Hold, confirm, and cancel operations are atomic Spring transactions.
2. **Pessimistic row locks.** Requested seats use `PESSIMISTIC_WRITE`. Competing transactions wait, then re-check after the winner commits.
3. **Deterministic lock order.** Seat numbers are sorted before locking, reducing deadlock risk for multi-seat holds.
4. **Checks inside the lock.** Active reservations and bookings are queried after the requested rows are locked.
5. **Database invariant.** `booking_seats.seat_id` is `UNIQUE`, so PostgreSQL rejects any duplicate even if an application defect bypasses the service check.
6. **Locked confirmation.** Confirmation re-locks the reservation and its seats and rejects expired reservations.

The invariant is: **a seat ID may appear in at most one confirmed booking**. Therefore 100 competing requests can never confirm more than 20 seats.

## REST API

| Method | Endpoint | Purpose | Success |
|---|---|---|---|
| `GET` | `/api/v1/seats` | Live state of all seats | `200` |
| `POST` | `/api/v1/reservations` | Hold selected seats for two minutes | `201` |
| `POST` | `/api/v1/reservations/{id}/confirm` | Simulate payment and confirm | `200` |
| `POST` | `/api/v1/reservations/{id}/cancel` | Cancel an active hold | `200` |
| `GET` | `/api/v1/bookings` | Confirmed booking history | `200` |

### Create a reservation

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H 'Content-Type: application/json' \
  -d '{
    "customerName": "Amartya Singh",
    "customerEmail": "amartya@example.com",
    "seatNumbers": ["1A", "1B"]
  }'
```

```json
{
  "reservationId": "2a74a31c-48c6-4e72-8532-3ccb4bf16888",
  "status": "ACTIVE",
  "seatNumbers": ["1A", "1B"],
  "expiresAt": "2026-11-02T09:00:00Z",
  "remainingSeconds": 119
}
```

Errors use a stable shape with `400` validation, `404` missing resource, `409` conflict, or `410` expired reservation.

```json
{
  "timestamp": "2026-11-02T08:58:14Z",
  "status": 409,
  "code": "SEAT_UNAVAILABLE",
  "message": "One or more selected seats are no longer available."
}
```

Import [`docs/SeatLock.postman_collection.json`](docs/SeatLock.postman_collection.json) for the full workflow, or open `/v3/api-docs` for generated OpenAPI JSON.

## Testing

```bash
cd backend
./mvnw test

cd ../frontend
npm run lint
npm run build
```

`ConcurrentBookingIntegrationTest` starts isolated PostgreSQL, releases all 100 customers simultaneously, and verifies exactly 20 unique confirmations with no duplicate seat. `ReservationLifecycleIntegrationTest` verifies cancellation release, automatic expiry, and rejection of expired confirmation. Docker must be running for these integration tests.

An additional end-to-end load run against the real API produced:

```json
{ "requests": 100, "holds": 20, "confirmed": 20, "uniqueSeats": 20, "duplicates": 0 }
```

## Configuration

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/seatlock` | JDBC URL |
| `DB_USERNAME` | `seatlock` | Database user |
| `DB_PASSWORD` | `seatlock` | Local password |
| `SERVER_PORT` | `8080` | API port |
| `ALLOWED_ORIGIN` | `http://localhost:5173` | CORS origin |
| `LOCK_DURATION` | `PT2M` | ISO-8601 hold duration |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Client API base |

Checked-in credentials are local defaults only. No production secrets or API keys are stored here.

## Design decisions

- **Pessimistic over optimistic locking:** contention is central, so serialization per seat gives deterministic behaviour and clear conflict responses.
- **UTC persisted instants:** the API stays timezone-neutral and the client derives its countdown from absolute expiry.
- **Polling over WebSockets:** eight-second polling stays simple; the server remains authoritative when a hold is requested.
- **Anonymous checkout:** authentication is intentionally outside the core concurrency problem; the domain can accept customer IDs later.

## Author

Designed and developed by **Amartya Singh**.

This project was completed with assistance from generative development tools. The architecture, implementation decisions, testing, verification, and final review were performed and are understood by the author.
