# Library Management & Lending Microservice

A Spring Boot 4.1 (Java 21) microservice for managing a book catalog, library members and loans.

---

## Quick Start - Run Locally (one line)

Prerequisites: Docker with Compose.

```bash
docker compose up --build
```

That's it. This builds the Java 21 image, starts PostgreSQL 16, runs Flyway migrations + seed data, and starts the app.

| Endpoint | URL |
|---|---|
| API base | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

Stop (keep data):
```bash
docker compose down
```
Stop and wipe data:
```bash
docker compose down -v
```

---

## Run Locally (without Docker for the app)

Prerequisites: Java 21 + Docker (for Postgres).

```bash
# 1. Start Postgres
docker run -d --name pg -p 5432:5432 \
  -e POSTGRES_DB=booklending \
  -e POSTGRES_USER=booklending \
  -e POSTGRES_PASSWORD=booklending \
  postgres:16-alpine

# 2. Start the app
./mvnw spring-boot:run
```

---

## Authentication

HTTP Basic. Use the Swagger UI **Authorize** button to authenticate before calling any API.

![Swagger Authorize Button](docs/swagger-authorize-button.png)

Make sure to authenticate with the below credentials in order to access APIs.

| Username | Password | Role |
|---|---|---|
| `librarian@library.com` | `librarian` | `ROLE_LIBRARIAN` |
| `member@library.com` | `member` | `ROLE_MEMBER` |
| `member2@library.com` | `member2` | `ROLE_MEMBER` |

Role permissions:
- `ROLE_LIBRARIAN` — full catalog management, member management, view all loans
- `ROLE_MEMBER` — browse catalog, borrow/return books, view own loans

---

## Seeded Data

On first run Flyway applies `V2__seed_data.sql` which seeds the following data automatically.

### Members

| Name | Email | Linked User |
|---|---|---|
| Demo Member | member@library.com | `member@library.com` |
| Demo Member 2 | member2@library.com | `member2@library.com` |

### Books

| # | Title | Author | ISBN | Copies |
|---|---|---|---|---|
| 1 | Clean Code | Robert C. Martin | 9780132350884 | 3 |
| 2 | The Pragmatic Programmer | Andrew Hunt, David Thomas | 9780135957059 | 2 |
| 3 | Design Patterns | Gamma, Helm, Johnson, Vlissides | 9780201633610 | 2 |
| 4 | Refactoring | Martin Fowler | 9780134757599 | 4 |
| 5 | Domain-Driven Design | Eric Evans | 9780321125217 | 1 |
| 6 | Effective Java | Joshua Bloch | 9780134685991 | 3 |
| 7 | Java Concurrency in Practice | Brian Goetz | 9780321349606 | 2 |
| 8 | Spring in Action | Craig Walls | 9781617294945 | 3 |
| 9 | Head First Design Patterns | Eric Freeman, Elisabeth Robson | 9780596007126 | 4 |
| 10 | Code Complete | Steve McConnell | 9780735619678 | 2 |

---

## Sample curl Requests

```bash
# List all books (member or librarian)
curl -u member@library.com:member http://localhost:8080/api/books

# Get a single book
curl -u member@library.com:member http://localhost:8080/api/books/<book-uuid>

# Create a book (librarian only)
curl -u librarian@library.com:librarian -X POST http://localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":2}'

# Update a book (librarian only)
curl -u librarian@library.com:librarian -X PUT http://localhost:8080/api/books/<book-uuid> \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":5}'

# Delete a book — only when all copies are available (librarian only)
curl -u librarian@library.com:librarian -X DELETE http://localhost:8080/api/books/<book-uuid>

# Create a member (librarian only)
curl -u librarian@library.com:librarian -X POST http://localhost:8080/api/members \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}'

# List all members (librarian only)
curl -u librarian@library.com:librarian http://localhost:8080/api/members

# Delete a member — only when no active loans (librarian only)
curl -u librarian@library.com:librarian -X DELETE http://localhost:8080/api/members/<member-uuid>

# Borrow a book — member resolved from logged-in user
curl -u member@library.com:member -X POST http://localhost:8080/api/loans/borrow/<book-uuid>

# Return a book (idempotent)
curl -u member@library.com:member -X POST http://localhost:8080/api/loans/return/<book-uuid>

# View own loans (member)
curl -u member@library.com:member http://localhost:8080/api/loans

# View all loans grouped by member (librarian)
curl -u librarian@library.com:librarian http://localhost:8080/api/loans

# View loans for a specific member (librarian)
curl -u librarian@library.com:librarian http://localhost:8080/api/loans/members/<member-uuid>
```

---

## Observability

### Health

```bash
# Basic status — no auth required
curl http://localhost:8080/actuator/health

# Full details (DB, disk space) — librarian auth required
curl -u librarian@library.com:librarian http://localhost:8080/actuator/health
```

### Metrics

```bash
# List all available metric names
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics

# Custom loan counters
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics/loans.borrowed
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics/loans.returned
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics/loans.rejected

# Loan rejections filtered by reason tag
curl -u librarian@library.com:librarian "http://localhost:8080/actuator/metrics/loans.rejected?tag=reason:out_of_stock"
curl -u librarian@library.com:librarian "http://localhost:8080/actuator/metrics/loans.rejected?tag=reason:max_loans_exceeded"
curl -u librarian@library.com:librarian "http://localhost:8080/actuator/metrics/loans.rejected?tag=reason:overdue_loan_exists"

# JVM and HTTP metrics (examples)
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics/jvm.memory.used
curl -u librarian@library.com:librarian http://localhost:8080/actuator/metrics/http.server.requests
```

### Prometheus

```bash
# Full Prometheus scrape dump (all metrics)
curl -u librarian@library.com:librarian http://localhost:8080/actuator/prometheus
```

### Structured Logging

Every request is assigned a `traceId` (from the `X-Request-Id` header, or auto-generated). It appears in every log line:

```
2026-10-30 03:00:00.000 INFO  [http-nio-8080-exec-1] c.l.loans.LoanService [traceId=abc-123] - Book borrowed loanId=... bookId=... memberId=... dueDate=...
```

Pass your own trace ID to correlate logs across services:
```bash
curl -u member@library.com:member \
  -H 'X-Request-Id: my-trace-123' \
  -X POST http://localhost:8080/api/loans/borrow/<book-uuid>
```

---

## Running Tests

```bash
# All tests (requires Docker for Testcontainers integration tests)
./mvnw test

# Unit tests only (no Docker required)
./mvnw test -Dtest="BookServiceTest,MemberServiceTest,LoanServiceTest,LoanPolicyValidatorTest,BookControllerTest,LoanControllerTest"

# Integration + security tests only (requires Docker)
./mvnw test -Dtest="LibraryIntegrationTest,SecurityIntegrationTest"

# Full clean build + test
./mvnw clean test
```

Test reports: `target/surefire-reports/`

---

## Configuration

Borrowing rules are externalized in `application.yml`:

```yaml
library:
  lending:
    max-active-loans: 3
    loan-duration-days: 14
    zone-id: UTC
```

---