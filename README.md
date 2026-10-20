# FlowDesk

FlowDesk is a collaborative project and task management platform: a Java/Spring Boot microservices backend (auth, project, task, notification, analytics, API gateway) and a React/Redux frontend, backed by Kafka, Redis, PostgreSQL, MongoDB, and WebSockets.

**Status: in development**

## Engineering standards

Coding rules (feature packages, `ApiResponse`, Axios, `/api/v1/`, etc.): [docs/engineering-standards.md](docs/engineering-standards.md). Cursor loads the same rules from `.cursor/rules/`.

## Repository layout

| Path | Contents |
| --- | --- |
| `FlowDesk-Backend/` | Maven multi-module backend (services, `common-lib`, infra compose, ADRs, quality gates) |
| `FlowDesk-Frontend/` | React/Redux frontend (scaffold placeholder) |
| `.github/workflows/` | CI — runs `mvn verify` against `FlowDesk-Backend/` |

## System Design Concepts Index

| Concept | File path |
| --- | --- |
| Microservices architecture | TODO |
| API Gateway | TODO |
| Service discovery (Eureka) | TODO |
| Event-driven messaging (Kafka) | TODO |
| Caching (Redis) | TODO |
| Relational persistence (PostgreSQL) | TODO |
| Document persistence (MongoDB) | TODO |
| Real-time updates (WebSockets) | TODO |
| Authentication & authorization | TODO |
| Frontend state management (Redux) | TODO |
| Architecture Decision Records | TODO |
| CI quality gates (Checkstyle, Spotless, ArchUnit) | TODO |
