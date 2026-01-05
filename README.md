# Analyzer (FX Deals Import)

## Design Choices

- **PostgreSQL + Docker Compose**: reproducible local setup with real DB.
- **Simple schema initialization**: uses `schema.sql` on startup to create the `deals` table + indexes deterministically.
- **Idempotency / dedupe**: enforced at the DB layer via a **unique index on `deal_id`**, so the same deal can’t be imported twice (even across restarts).
- **No rollback semantics**: batch import processes rows **one-by-one**, returning per-row outcomes so valid rows persist even if other rows fail.
- **Validation**: Bean Validation on request DTO + ISO currency validation to show a scalable validation approach.
- **Error handling**: malformed JSON / wrong request shape returns **HTTP 400**; otherwise returns `200` with per-row results.
- **Logging**: per-row warnings and a batch summary for traceability during imports.
- **Lombok**: used to reduce boilerplate and keep the code more declarative/readable (`@Slf4j`, `@RequiredArgsConstructor`, etc.).
- **Coverage**: JaCoCo report (via `./mvnw verify`) shows **~91% instruction coverage** and **~85% branch coverage**.

## Run (recommended): Docker Compose

Prereqs:
- Docker + Docker Compose

Start PostgreSQL + the app:

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`.

## Test using IntelliJ HTTP Client + verify DB

### Send HTTP requests from IntelliJ

1. Start the stack:

```bash
docker compose up --build -d
```

2. Open `src/main/resources/requests.http`.
3. Run the HTTP test requests

### Verify rows in PostgreSQL (two ways)

#### Option A: via Docker + psql (fastest)

- List tables:

```bash
docker exec -it analyzer-postgres-1 psql -U analyzer -d analyzer -c "\dt"
```

- Check rows:

```bash
docker exec -it analyzer-postgres-1 psql -U analyzer -d analyzer \
  -c "select deal_id, from_currency, to_currency, deal_ts, amount, created_at from deals order by created_at desc;"
```

#### Option B: via IntelliJ Database tool window

1. Open **Database** tool window → **+** → **Data Source** → **PostgreSQL**
2. Set:
   - Host: `localhost`
   - Port: `5432`
   - Database: `analyzer`
   - User: `analyzer`
   - Password: `analyzer`
3. Test Connection → OK
4. Expand schemas → `public` → tables → `deals`, or run:

```sql
select deal_id, from_currency, to_currency, deal_ts, amount, created_at
from deals
order by created_at desc;
```

## Logs

### Docker Compose logs (recommended)

- Tail logs for all services:

```bash
docker compose logs --tail=200
```

- Follow (stream) logs for all services:

```bash
docker compose logs -f
```

- Follow only the app logs:

```bash
docker compose logs -f app
```

- Follow only Postgres logs:

```bash
docker compose logs -f postgres
```

### Direct container logs (alternative)

```bash
docker logs -f analyzer-app-1
docker logs -f analyzer-postgres-1
```

## Run locally (without Docker)

Prereqs:
- Java 17+
- A running PostgreSQL instance (or run only the `postgres` service from compose)

Start only Postgres:

```bash
docker compose up -d postgres
```

Run the app:

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

