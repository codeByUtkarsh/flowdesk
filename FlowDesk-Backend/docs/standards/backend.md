# Backend coding standards (Spring Boot)

Applies to every module under `FlowDesk-Backend/` except where a module is a library (`common-lib` has no controllers).

## Package layout — feature-based, not layer-based

Do **not** use `controller/`, `service/`, `repository/` at the module root.

```text
com.flowdesk.<service>.<feature>/
  api/           # REST controller for this feature only
  service/
  repository/
  dto/
  entity/        # or domain/
  mapper/        # MapStruct
  exception/     # feature-specific exceptions only
```

Example: `com.flowdesk.task.task.api.TaskController`  
Cross-cutting (security filter, exception advice) lives in `com.flowdesk.<service>.shared` or `common-lib`.

Gateway: feature folders for `filter`, `routing` still grouped by concern (`gateway.filter`, `gateway.config`) — no domain entities.

## Controllers

- Never return JPA/Mongo **entities**. Return DTOs only.
- Map entity → DTO with **MapStruct** (no manual field-by-field in controllers).
- `@Valid` on request DTOs only. Business rules (unique email, legal status transition) belong in the **service**.
- Wrap every body in `ApiResponse<T>`.
- Paths under `/api/v1/...`.

## Errors

- One `@RestControllerAdvice` for the whole service (prefer shared advice from `common-lib`).
- Hierarchy: `BaseException` → feature exceptions (`TaskConflictException`, …).
- Map exceptions → `ErrorCode` enum (stable string + HTTP status). No `throw new RuntimeException("oops")` from controllers.

## Config

- Grouped `@ConfigurationProperties` (prefix per concern: `flowdesk.jwt`, `flowdesk.kafka`).
- No scattered `@Value` for business config.
- Secrets only from **environment variables** (never commit passwords).

## Auth & HTTP

- JWT **stateless**. No server sessions.
- Method security: `@PreAuthorize` (or project AOP equivalent documented in ADR) for role checks — UI hiding is not authz.
- CORS in one `@Configuration` bean, not per-controller annotations.

## Logging

- SLF4J only.
- Correlation ID filter: read/create `X-Correlation-ID`, put in **MDC**, include in every log line.

## Tests (minimum per feature)

| Layer | Tool |
|---|---|
| Controller | `@WebMvcTest` — DTO in/out, status codes, validation |
| Persistence | `@DataJpaTest` + **Testcontainers** for Postgres/Mongo as applicable |
| Service | Unit tests with mocked repositories |

Do not substitute Postman-only verification for command-side tests.

## Idempotency

Side-effect `POST`s: require `Idempotency-Key`; persist key → resource id (or hash) so retries are safe.

## Boilerplate before features

Add `ApiResponse`, `ErrorCode`, `BaseException`, `GlobalExceptionHandler`, correlation filter, and MapStruct config **before** the first feature controller.
