# Cross-cutting standards

Applies to every FlowDesk service and the frontend.

## Contract before code

- Define request/response DTOs (and OpenAPI once a service has HTTP) **before** implementing controller or UI.
- Shared types that both sides need (event payloads, `ErrorCode`, envelope shape) live in `common-lib` (Java) and are mirrored in frontend `src/shared/api/types.ts` — keep names identical.
- Do not invent a second JSON shape per service.

## HTTP

- All public REST paths: `/api/v1/...` from day one (gateway strips or forwards the prefix consistently).
- Version lives in the URL, not an ad-hoc header.

## Response envelope

Every JSON endpoint returns the same envelope (see backend standards for `ApiResponse<T>`):

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-08-18T12:00:00Z"
}
```

On failure: `success: false`, `data: null`, `error: { "code": "TASK_CONFLICT", "message": "..." }`.

Frontend unwraps `data` in Axios interceptors; UI never parses a raw entity.

## Errors

- Backend: `ErrorCode` enum → HTTP status + stable code string.
- Frontend: `ErrorCode` → toast/copy map. Unknown codes fall back to a generic message, never dump stack traces.

## Idempotency

- Every **side-effect** `POST` (create task, add member, etc.) accepts `Idempotency-Key` header (UUID).
- Same key + same actor + same payload → same result, no duplicate rows or duplicate Kafka events.
- Safe `GET`s do not require the header.

## Local infra

- One command from repo conventions: Docker Compose for Postgres, Redis, MongoDB, Kafka, Zookeeper, Eureka (already under `FlowDesk-Backend/infrastructure/`).
- App config: host/ports via env; **no secrets in git**.

## Tracing

- Gateway injects `X-Correlation-ID` (or creates one). Every service puts it in MDC/logs and forwards it on outbound calls and in domain events when possible.

## Boilerplate first

Before any feature package:

1. `ApiResponse`, `ErrorCode`, `BaseException`, `GlobalExceptionHandler` in `common-lib` (or copied pattern per service only if lib is not ready — prefer lib).
2. Frontend: Axios client, envelope unwrap, ErrorBoundary, env config.
3. Then feature folders.
