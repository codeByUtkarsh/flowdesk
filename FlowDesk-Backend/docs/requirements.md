# FlowDesk — Functional & Non-Functional Requirements

## 0. Why this document exists

The blueprint you have goes straight from "problem statement" to "here's the
architecture." It never states, in testable terms, what the system must do
(FR) or how well it must do it (NFR). That's backwards — architecture is the
*answer* to requirements, and right now you have an answer with no question
attached. This document is that question, reconstructed from your blueprint's
own content. Edit it. Argue with the numbers. Own it before you write code
against it.

---

## 1. Functional Requirements

### Auth Service (8081)
- **FR-AUTH-01** Register with email + password (hashed, never stored/logged plaintext).
- **FR-AUTH-02** Login issues a JWT access token + refresh token.
- **FR-AUTH-03** Refresh token rotates on use; a reused/rotated-out token is rejected.
- **FR-AUTH-04** Logout invalidates the refresh token.
- **FR-AUTH-05** Google OAuth2 login, linked to the existing user record if the email matches.
- **FR-AUTH-06** Any service can validate a JWT independently, without a synchronous call to Auth.

### Project Service (8082)
- **FR-PROJ-01** Create a project; creator becomes Owner.
- **FR-PROJ-02** Update/archive a project (Owner/Admin only).
- **FR-PROJ-03** Add a member by email/user id with an assigned role.
- **FR-PROJ-04** Remove a member (Owner/Admin only; an Admin cannot remove the Owner).
- **FR-PROJ-05** Change a member's role (Owner only).
- **FR-PROJ-06** List members and roles for a project.
- **FR-PROJ-07** Enforce role checks on every mutating endpoint, server-side.

### Task Service (8083)
- **FR-TASK-01** Create a task (title, description, assignee, priority, due date) in a project.
- **FR-TASK-02** Update task fields, including status transitions.
- **FR-TASK-03** Delete/archive a task.
- **FR-TASK-04** Add and list comments on a task.
- **FR-TASK-05** Add/remove labels on a task.
- **FR-TASK-06** List tasks by project, filtered by status (Kanban read path).
- **FR-TASK-07** Reject a write against a stale version with a client-actionable conflict response.
- **FR-TASK-08** Emit a domain event on create/status-change/assign, guaranteed to
  eventually publish even if the process crashes between DB commit and message publish.

### Notification Service (8084)
- **FR-NOTIF-01** Consume task domain events, generate one notification per relevant project member.
- **FR-NOTIF-02** Persist notification (type, payload, read/unread, timestamp).
- **FR-NOTIF-03** Push in real time to any connected client for that user, regardless
  of which service instance consumed the triggering event.
- **FR-NOTIF-04** List a user's notifications; mark read; unread count.
- **FR-NOTIF-05** Auto-expire notifications after 30 days.
- **FR-NOTIF-06** Never process the same event twice, even if delivered twice.

### Analytics Service (8085)
- **FR-AN-01** Consume the same task domain events independently — zero coupling
  to Task or Notification services.
- **FR-AN-02** Compute and serve sprint velocity / completion-rate metrics per project.
- **FR-AN-03** Serve analytics reads without adding load to the primary transactional database.

### Frontend
- **FR-UI-01** Register/login with validation and error states.
- **FR-UI-02** Silently refresh the access token on 401, no forced re-login.
- **FR-UI-03** Dashboard listing the user's projects.
- **FR-UI-04** Kanban board per project, drag-and-drop status change.
- **FR-UI-05** Board updates live via WebSocket, no manual refresh.
- **FR-UI-06** Notification bell with unread count and history.
- **FR-UI-07** Surface an optimistic-lock conflict with a clear recovery action — never a silent failure.

### Cross-cutting
- **FR-X-01** Every request is traceable end-to-end by a single correlation ID across every service it touches.
- **FR-X-02** Per-user rate limiting at the gateway with a clear 429 response.

---

## 2. Non-Functional Requirements

*Every number below is a starting draft, not gospel. You should be able to
defend or change any of them if asked "why this number?"*

### Availability
- **NFR-AV-01** Auth, Project, Task (the CP path) — target 99.5% uptime during any demo/interview window.
- **NFR-AV-02** Notification/Analytics may degrade (delayed delivery) without taking
  down core task CRUD — this is what the "kill Notification Service" test in week 7 proves.

### Latency / Performance
- **NFR-PERF-01** Task write (create/update), P95 < 300ms end-to-end through the gateway.
- **NFR-PERF-02** Task list read: cache hit P95 < 50ms, cache miss P95 < 250ms.
- **NFR-PERF-03** Notification delivered to a connected client within P95 2s of the
  triggering event. Note: your 1s Outbox poll interval alone eats half this budget — factor that in.

### Consistency
- **NFR-CONS-01** Task/Project/Auth — read-your-own-write consistency required.
- **NFR-CONS-02** Notification/Analytics — eventual consistency, bounded by NFR-PERF-03. Acceptable per ADR-001.

### Scalability
- **NFR-SCALE-01** Every service runs correctly with 2+ concurrent instances with
  zero code change (stateless, no in-memory session state) — validated, not assumed.
- **NFR-SCALE-02** Adding a Notification Service instance never requires reconnecting
  clients already attached to other instances.

### Durability / Reliability
- **NFR-DUR-01** No task domain event is permanently lost due to a crash between DB
  commit and Kafka publish — needs an actual chaos test (kill the process mid-flow), not just code review.
- **NFR-DUR-02** A duplicate-delivered event never creates a duplicate notification
  or double-counts an analytics metric.

### Security
- **NFR-SEC-01** Passwords hashed (bcrypt/argon2), never logged, never returned in any response.
- **NFR-SEC-02** Access tokens short-lived (≤15 min); refresh tokens rotated on use and revocable.
- **NFR-SEC-03** Role checks enforced server-side on every mutating endpoint — a
  hidden UI button is not access control.
- **NFR-SEC-04** Rate limiting active at the gateway before any service-level logic runs.

### Observability
- **NFR-OBS-01** Every service exposes `/actuator/health` and `/actuator/prometheus`.
- **NFR-OBS-02** 100% of inbound requests carry and propagate a correlation ID, visible end-to-end in Zipkin.
- **NFR-OBS-03** Every consumer failure (Kafka/DB/Redis) logs enough context to diagnose without reproducing locally.

### Maintainability
- **NFR-MAINT-01** No new datastore or messaging technology enters the stack without an ADR.
- **NFR-MAINT-02** Each service is independently buildable and deployable.

### Testability
- **NFR-TEST-01** Command-side write logic (outbox atomicity, optimistic-lock
  conflict) has an automated test — not just manual Postman verification.
- **NFR-TEST-02** At least one integration test per service using Testcontainers
  against a real Postgres/Kafka/Redis/Mongo for the core write path — not mocks.

---

## 3. Explicit non-goals (scope boundary)

8-week portfolio projects reliably balloon into 8-month ones. These are out of scope for the core build:

- Billing/payments
- Multi-tenant orgs beyond project-level membership (no "company" entity)
- Mobile app
- i18n/l10n
- File/image attachments on tasks
- Admin backoffice UI
- SSO beyond Google OAuth

**Bonus enhancements (WhatsApp fallback, Elasticsearch search, Debezium CDC,
AI sprint summarizer) are stretch goals.** Don't start any of them before the
core 8-week plan is demoable end-to-end.

---

## 4. Open questions you own, not me

- Are the SLO numbers above right, or would you set them differently — and can you defend the change?
- Is the 100 req/min rate limit (from the blueprint) right for a demo project, or overkill?
- Does Google OAuth earn its complexity here, or does it just add an auth flow
  without proportional interview value?
- Which single bonus enhancement (if any) will you actually commit to before week 8?
