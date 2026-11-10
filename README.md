# Pula Watch

Open-source uptime monitoring. Pula Watch checks your websites and APIs on a schedule, records every result, and marks a monitor DOWN after repeated failures.

Built as a hands-on DevOps project: Java 21, Spring Boot 4, PostgreSQL, Testcontainers, Docker, GitHub Actions, Trivy.

## Features (Phase 1)

- REST API to create, list, update and delete monitors
- Scheduler that claims due monitors with `SELECT ... FOR UPDATE SKIP LOCKED`, so several replicas can run without double-checking
- HTTP checks on Java 21 virtual threads
- Status state machine: UNKNOWN → UP, or SUSPECT → DOWN after 3 consecutive failures
- SSRF protection: URLs that resolve to loopback, private, link-local (cloud metadata) or reserved addresses are rejected, both on save and before every check
- Prometheus metrics and Kubernetes-style health probes via Spring Boot Actuator

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

Start Postgres and the app together:

```
docker compose up --build
```

Create a monitor:

```
curl -X POST http://localhost:8080/api/monitors -H "Content-Type: application/json" -d "{\"name\":\"Example\",\"url\":\"https://example.com\",\"intervalSeconds\":60}"
```

See its recent results (replace 1 with the id returned above):

```
curl http://localhost:8080/api/monitors/1/results
```

## Run the tests

Unit tests plus integration tests against a real Postgres in Docker (Testcontainers):

```
mvn verify
```

## API

| Method | Path | Purpose |
|---|---|---|
| GET | /api/monitors | List monitors |
| POST | /api/monitors | Create a monitor |
| GET | /api/monitors/{id} | Get one monitor |
| PUT | /api/monitors/{id} | Update a monitor |
| DELETE | /api/monitors/{id} | Delete a monitor |
| GET | /api/monitors/{id}/results | Last 50 check results |
| GET | /actuator/health | Health |
| GET | /actuator/prometheus | Metrics |

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| DB_URL | jdbc:postgresql://localhost:5432/pulawatch | JDBC URL |
| DB_USER | pulawatch | Database user |
| DB_PASSWORD | pulawatch | Database password |
| ALLOW_PRIVATE_TARGETS | false | Set true only in local dev to monitor localhost |

## Roadmap

- [x] Phase 1: monitors API, scheduler, HTTP checks, SSRF guard, CI
- [ ] Phase 2: email + Telegram alerts on status change
- [ ] Phase 3: login and public status pages
- [ ] Phase 4: Terraform + k3s deployment, OWASP ZAP in CI, Grafana dashboards
- [ ] Phase 5: second-region probe that confirms outages before alerting

## License

Apache-2.0
