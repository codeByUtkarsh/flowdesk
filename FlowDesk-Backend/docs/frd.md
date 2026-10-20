# FlowDesk - Functional Requirements Document

## 1. Document Purpose

This document defines what FlowDesk must do as a product before committing to
how it is implemented. It translates the blueprint into a requirements-first
view that can be reviewed, tested, and defended in interviews.

FlowDesk is a real-time collaborative project management platform for small
teams that need projects, tasks, comments, roles, notifications, and analytics
in one place.

## 2. Product Vision

FlowDesk gives teams a single source of truth for project execution:

- projects and members are managed centrally
- tasks move through a live workflow without page refreshes
- updates generate durable notifications
- analytics are computed without slowing transactional paths

The system is intended to be realistic enough to demonstrate distributed-system
trade-offs while still being buildable in an 8-week portfolio schedule.

## 3. Target Users

### 3.1 Primary user groups

- Startup engineering teams tracking feature delivery and bugs
- Student project groups coordinating assignments and deadlines
- Freelancers collaborating with clients on deliverables
- Small businesses managing internal tasks and operations

### 3.2 Personas

- Owner: creates projects, manages admins, controls membership and project state
- Admin: manages most project operations except owner-only actions
- Member: works on assigned tasks, comments, and consumes notifications

## 4. Scope

### 4.1 In scope

- Email/password authentication and JWT-based session flow
- Google OAuth login
- Project creation and member role management
- Task CRUD, comments, labels, assignee changes, and status transitions
- Real-time notifications for task activity
- Project-level analytics such as sprint velocity and completion rate
- Web UI with dashboard, Kanban board, and notification center

### 4.2 Out of scope

- Billing and payments
- Mobile apps
- Organization-level tenant hierarchy beyond project membership
- File attachments
- Admin backoffice
- SSO providers beyond Google OAuth
- Bonus features like WhatsApp fallback, Elasticsearch search, Debezium, and AI sprint summaries in the core release

## 5. Assumptions

- Users interact through a web frontend and a gateway-backed API surface
- Core transactional data must be correct even when side effects are delayed
- Event-driven side effects are acceptable when bounded and observable
- The system should support multi-instance deployment for all backend services

## 6. Functional Requirements

### 6.1 Authentication and Identity

- FR-AUTH-01: Users can register with email, password, and display name.
- FR-AUTH-02: Passwords are hashed before storage and never returned to clients.
- FR-AUTH-03: Users can log in with email and password to receive an access token and refresh token.
- FR-AUTH-04: Refresh token rotation is enforced on refresh requests.
- FR-AUTH-05: Reused or revoked refresh tokens are rejected.
- FR-AUTH-06: Users can log out and invalidate their refresh token.
- FR-AUTH-07: Users can sign in with Google OAuth and be linked to an existing account if the email matches.
- FR-AUTH-08: Other services can validate JWTs locally without a synchronous dependency on Auth Service.

### 6.2 Project Management

- FR-PROJ-01: A user can create a project and becomes its Owner by default.
- FR-PROJ-02: Owners and Admins can update project metadata.
- FR-PROJ-03: Owners and Admins can archive a project.
- FR-PROJ-04: Owners and Admins can add members to a project.
- FR-PROJ-05: Owners and Admins can remove members, except an Admin cannot remove the Owner.
- FR-PROJ-06: Only Owners can change another member's role.
- FR-PROJ-07: Users can list project members and their roles.
- FR-PROJ-08: Role checks are enforced server-side on every mutating project endpoint.

### 6.3 Task Management

- FR-TASK-01: Users can create a task within a project.
- FR-TASK-02: A task includes at least title, description, status, priority, assignee, due date, and creator.
- FR-TASK-03: Users can update task fields, including status transitions.
- FR-TASK-04: Users can delete or archive a task.
- FR-TASK-05: Users can add comments to a task.
- FR-TASK-06: Users can list comments for a task.
- FR-TASK-07: Users can add labels to a task.
- FR-TASK-08: Users can remove labels from a task.
- FR-TASK-09: Users can list tasks by project and status for Kanban-style views.
- FR-TASK-10: Concurrent stale writes are rejected with a conflict response instead of silently overwriting data.
- FR-TASK-11: Task create, assign, and status-change operations produce domain events.
- FR-TASK-12: Domain events are guaranteed to be published eventually, even if the service crashes between database commit and broker publish.

### 6.4 Notifications

- FR-NOTIF-01: The system generates notifications from task domain events.
- FR-NOTIF-02: Notifications are created for relevant project members.
- FR-NOTIF-03: Notifications are persisted with type, payload, read state, recipient, and timestamp.
- FR-NOTIF-04: Users can retrieve notification history.
- FR-NOTIF-05: Users can mark notifications as read.
- FR-NOTIF-06: Users can retrieve unread notification counts.
- FR-NOTIF-07: Connected users receive notifications in real time without page refresh.
- FR-NOTIF-08: Real-time delivery works correctly across multiple notification-service instances.
- FR-NOTIF-09: Duplicate delivery from the broker does not create duplicate user-facing notifications.
- FR-NOTIF-10: Notifications older than 30 days are automatically expired.

### 6.5 Analytics

- FR-AN-01: Analytics Service consumes the same task events independently from Notification Service.
- FR-AN-02: The system computes project-level metrics such as sprint velocity and completion rate.
- FR-AN-03: Analytics queries do not add load to the primary transactional write path.
- FR-AN-04: Analytics reads are exposed through API endpoints suitable for dashboards and reports.

### 6.6 Frontend Experience

- FR-UI-01: Users can register and log in through the web UI with validation and error handling.
- FR-UI-02: Access tokens are refreshed automatically after expiration without forcing a manual login.
- FR-UI-03: Users can see a dashboard of their projects.
- FR-UI-04: Users can open a project Kanban board and view tasks by status.
- FR-UI-05: Users can drag and drop tasks across workflow columns.
- FR-UI-06: Task board changes are reflected live through real-time updates.
- FR-UI-07: Users can access a notification bell with unread count and history.
- FR-UI-08: Optimistic-lock conflicts are shown clearly with a recovery action, such as refresh and retry.

### 6.7 Cross-Cutting Product Requirements

- FR-X-01: Every inbound request receives a correlation ID that can be traced end to end.
- FR-X-02: Gateway-level rate limiting is enforced per user with a clear 429 response.
- FR-X-03: All backend services expose health endpoints for runtime checks.
- FR-X-04: The platform remains usable for core CRUD even if async side-effect services degrade temporarily.

## 7. Core User Workflows

### 7.1 User updates task status

1. User opens a board and moves a task to a new status.
2. Request enters through the API Gateway.
3. Gateway validates JWT, injects correlation ID, and enforces rate limiting.
4. Task Service updates the task in PostgreSQL.
5. The same transaction stores an outbox event.
6. Query-side cache for affected task lists is invalidated.
7. Notification Service later consumes the event, stores notifications, and pushes to connected users.
8. Analytics Service consumes the same event and updates project metrics independently.

### 7.2 Owner manages membership

1. Owner opens project settings.
2. Owner adds or removes members or changes roles.
3. Project Service validates the caller's role server-side.
4. Updated membership is persisted and reflected in downstream task and notification eligibility.

### 7.3 User receives notification

1. Task side effect generates a domain event.
2. Notification Service stores a notification record for recipients.
3. Redis Pub/Sub relays the fanout signal to all notification-service instances.
4. Each instance pushes only to its locally connected users.
5. User sees the notification instantly if online, otherwise later in notification history.

## 8. Non-Functional Requirements

### 8.1 Availability

- NFR-AV-01: Auth, Project, and Task paths target 99.5% availability during demo and interview usage.
- NFR-AV-02: Notification and Analytics degradation must not block core task CRUD.

### 8.2 Performance

- NFR-PERF-01: Task create and update requests should meet P95 latency below 300 ms through the gateway.
- NFR-PERF-02: Task list reads should meet P95 latency below 50 ms on cache hit and below 250 ms on cache miss.
- NFR-PERF-03: Notification delivery to connected users should meet P95 below 2 seconds from task mutation to client push.

### 8.3 Consistency

- NFR-CONS-01: Auth, Project, and Task reads require strong user-visible correctness.
- NFR-CONS-02: Notifications and analytics may be eventually consistent within an explicitly acceptable delay window.

### 8.4 Scalability

- NFR-SCALE-01: Every service must support horizontal scaling without code change.
- NFR-SCALE-02: No service may depend on in-memory session state for correctness.
- NFR-SCALE-03: Adding notification-service instances must not require reconnecting existing clients.

### 8.5 Reliability and Durability

- NFR-DUR-01: No task domain event may be permanently lost due to a crash between database commit and broker publish.
- NFR-DUR-02: Duplicate event delivery must not create duplicate notifications or double-count analytics.
- NFR-DUR-03: Failure of Kafka, Redis, MongoDB, or a downstream service must degrade observably rather than silently corrupting data.

### 8.6 Security

- NFR-SEC-01: Passwords must be hashed with a modern algorithm such as bcrypt or argon2.
- NFR-SEC-02: Access tokens are short-lived and refresh tokens are rotated and revocable.
- NFR-SEC-03: Authorization checks are enforced server-side for every mutating operation.
- NFR-SEC-04: Rate limiting occurs before downstream business logic.

### 8.7 Observability

- NFR-OBS-01: Every service exposes `/actuator/health` and `/actuator/prometheus`.
- NFR-OBS-02: Correlation IDs are propagated across synchronous and asynchronous boundaries where possible.
- NFR-OBS-03: Metrics, logs, and traces are sufficient to diagnose cross-service failures.

### 8.8 Maintainability and Testability

- NFR-MAINT-01: Each service is independently buildable and deployable.
- NFR-MAINT-02: No new datastore or messaging technology is introduced without an ADR.
- NFR-TEST-01: Command-side write paths require automated tests for happy path and failure path.
- NFR-TEST-02: At least one integration test per service should run against real infrastructure via Testcontainers for core flows.

## 9. Acceptance Criteria

### 9.1 MVP acceptance

FlowDesk MVP is acceptable when all of the following are true:

- a user can register, log in, and access protected APIs
- an owner can create a project and add members
- a member can create and update tasks on a Kanban board
- stale concurrent task updates return a conflict instead of overwriting data
- task updates trigger durable notifications
- connected users receive notifications without refresh
- analytics can show project metrics from consumed task events

### 9.2 Demo acceptance

- all services run through the gateway
- notification and analytics side effects are visibly decoupled from task writes
- failure of a side-effect dependency can be demonstrated without breaking core task CRUD
- logs and traces can explain a full "update task" request across the system

## 10. Release Boundaries

### 10.1 Core release

The core release for the 8-week build includes the five backend services, the
gateway, the shared library, the frontend, and the observability/deployment
scaffolding needed to demonstrate correctness.

### 10.2 Stretch goals

Stretch goals are explicitly post-MVP:

- AI sprint summarizer
- Elasticsearch-based search
- WhatsApp notification fallback
- Debezium-based CDC replacing the polling outbox publisher

## 11. Open Product Questions

- Is Google OAuth worth the added scope for the MVP, or should it remain demo-only?
- Is the 100 requests per minute limit the right default for this user profile?
- Should comments and labels be included in MVP week sequencing or deferred behind task CRUD and notifications?
- What exact analytics views are required for MVP versus later polish?
