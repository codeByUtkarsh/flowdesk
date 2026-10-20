# FlowDesk - Database Design

## 1. Purpose

This document defines data ownership, storage models, schemas, indexes, and
retention behavior for FlowDesk. It follows the system design rule that each
service owns its own data and that storage choices match access patterns rather
than forcing one database to serve every workload.

Important modeling rule: service boundaries matter more than relational purity.
IDs from another service may be stored as references, but FlowDesk should not
rely on cross-service database foreign keys between independently owned stores.

## 2. Data Ownership by Service

| Service | Primary store | Why it owns this data |
|---|---|---|
| Auth Service | PostgreSQL | user identity and token state require strong consistency |
| Project Service | PostgreSQL | project membership and roles are relational and security-sensitive |
| Task Service | PostgreSQL | tasks and outbox events need transactional integrity |
| Notification Service | MongoDB | notification payloads vary by type and fit document storage |
| Analytics Service | PostgreSQL read model / replica | aggregated metrics need read-optimized relational queries |
| API Gateway | Redis only for operational state | rate-limit counters are ephemeral, not domain data |
| Shared operational state | Redis | cache, pub/sub relay, idempotency keys, and counters |

## 3. Storage Strategy

### 3.1 Why PostgreSQL for Auth, Project, and Task

These domains need:

- strong transactional guarantees
- foreign-key-like relationships at the domain model level
- predictable updates and concurrency control
- optimistic locking support for tasks

This makes PostgreSQL the right fit for user, project, and task correctness.

### 3.2 Why MongoDB for Notifications

Notifications have variable payloads depending on event type:

- task assigned
- task status changed
- comment added
- mention created

MongoDB fits this because:

- payload shape varies over time
- writes are high-volume and append-heavy
- reads are mostly recipient-based timelines
- TTL expiry is native and simple

### 3.3 Why Redis is operational, not source of truth

Redis is used for fast shared state:

- task query caching
- WebSocket fanout relay
- idempotency event markers
- gateway rate-limit counters

Redis is intentionally not the source of truth for business records.

## 4. Relational Model

### 4.1 Auth Service schema

#### `users`

Stores registered identity records.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Recommended indexes:

- unique index on `email`

#### `refresh_tokens`

Stores refresh token lifecycle for rotation and revocation.

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Recommended indexes:

- `idx_refresh_tokens_user_id` on `(user_id)`
- `idx_refresh_tokens_token_hash` unique on `(token_hash)`
- `idx_refresh_tokens_expires_at` on `(expires_at)`

### 4.2 Project Service schema

#### `projects`

```sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`owner_id` is an Auth Service user reference held by value, not a cross-service
database foreign key.

#### `project_members`

```sql
CREATE TABLE project_members (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (project_id, user_id)
);
```

`user_id` is also a service-level reference to Auth data, validated through
application logic rather than a shared database constraint.

Recommended indexes:

- `idx_projects_owner_id` on `(owner_id)`
- `idx_project_members_user_id` on `(user_id)`
- composite primary key already supports project membership lookups

### 4.3 Task Service schema

#### `tasks`

```sql
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id UUID,
    created_by UUID NOT NULL,
    due_date DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Notes:

- `version` supports optimistic locking
- `project_id` is stored as the owning aggregate reference for read filtering
- `project_id`, `assignee_id`, and `created_by` are service-level references, not cross-service foreign keys

#### `task_comments`

```sql
CREATE TABLE task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    author_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### `task_labels`

```sql
CREATE TABLE task_labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, name)
);
```

#### `task_label_links`

```sql
CREATE TABLE task_label_links (
    task_id UUID NOT NULL,
    label_id UUID NOT NULL,
    PRIMARY KEY (task_id, label_id)
);
```

#### `task_outbox`

```sql
CREATE TABLE task_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);
```

Why outbox lives here:

- task row and outbox row must commit together
- separates durable event intent from broker availability

Recommended indexes:

- `idx_tasks_project_status` on `(project_id, status)`
- `idx_tasks_assignee_id` on `(assignee_id)`
- `idx_task_comments_task_id_created_at` on `(task_id, created_at)`
- `idx_task_outbox_pending` on `(status, created_at)` with a partial or filtered strategy for pending rows
- unique index on `task_outbox(event_id)`

### 4.4 Analytics Service relational read model

Analytics should not query raw transactional tables on the primary path for
every dashboard request. Use a read-optimized model.

#### `project_metrics_daily`

```sql
CREATE TABLE project_metrics_daily (
    project_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    tasks_created_count INT NOT NULL DEFAULT 0,
    tasks_completed_count INT NOT NULL DEFAULT 0,
    tasks_reopened_count INT NOT NULL DEFAULT 0,
    active_members_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, metric_date)
);
```

#### `project_velocity_snapshots`

```sql
CREATE TABLE project_velocity_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    sprint_key VARCHAR(100) NOT NULL,
    completed_points INT NOT NULL DEFAULT 0,
    planned_points INT NOT NULL DEFAULT 0,
    completion_rate NUMERIC(5,2) NOT NULL DEFAULT 0,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Recommended indexes:

- `idx_project_metrics_daily_project_date` on `(project_id, metric_date DESC)`
- `idx_project_velocity_project_sprint` on `(project_id, sprint_key)`

## 5. Document Model for Notifications

### 5.1 `notifications` collection

```json
{
  "_id": "ObjectId",
  "eventId": "uuid",
  "recipientUserId": "uuid",
  "projectId": "uuid",
  "type": "TASK_ASSIGNED",
  "payload": {
    "taskId": "uuid",
    "taskTitle": "Implement JWT refresh token",
    "actorId": "uuid",
    "actorName": "Anjali K.",
    "message": "Anjali assigned you to Implement JWT refresh token"
  },
  "isRead": false,
  "createdAt": "ISODate"
}
```

Why this shape:

- top-level recipient and read state match the primary query path
- `payload` remains flexible by notification type
- `eventId` supports deduplication and traceability

Recommended indexes:

```javascript
db.notifications.createIndex(
  { recipientUserId: 1, isRead: 1, createdAt: -1 }
);

db.notifications.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 2592000 }
);

db.notifications.createIndex(
  { eventId: 1, recipientUserId: 1 },
  { unique: true }
);
```

## 6. Redis Data Model

Redis usage is intentionally split by concern.

### 6.1 Task query cache

Example key:

```text
task:list:{projectId}:{status}:{page}:{size}
```

Value:

- serialized task list / page response

Why:

- read-heavy Kanban board requests benefit from fast cache hits

### 6.2 Notification fanout channels

Example channel:

```text
flowdesk:notif:{projectId}
```

Why:

- one notification-service instance can publish once
- all instances receive and push to locally connected users

### 6.3 Idempotency keys

Example key:

```text
processed_event:{eventId}
```

TTL:

- 24 hours or another window based on retry expectations

Why:

- prevents duplicate processing from at-least-once broker delivery

### 6.4 Gateway rate-limit counters

Example key:

```text
rate_limit:{userId}:{windowStart}
```

TTL:

- aligned to the selected rate-limit window

Why:

- fast, shared counters across all gateway instances

## 7. Query Patterns and Index Rationale

### 7.1 Tasks by project and status

Query:

- get all TODO or IN_PROGRESS tasks for one project

Index:

- `(project_id, status)`

Reason:

- a single-column index on `project_id` still scans every status for that project
- the compound index matches both filter dimensions

### 7.2 Notifications by user and unread state

Query:

- get unread notifications for one user, newest first

Index:

- `(recipientUserId, isRead, createdAt DESC)`

Reason:

- supports both filter and sort in one access path

### 7.3 Pending outbox events

Query:

- fetch oldest pending events to publish

Index:

- partial/filtered index on pending rows

Reason:

- published rows accumulate over time
- indexing only pending rows keeps the index smaller and more selective

## 8. Consistency and Transaction Boundaries

### 8.1 Local transactions

Transactional guarantees are local to each service's own relational store:

- task update + outbox insert
- project membership mutation
- refresh token rotation

### 8.2 No distributed transaction across services

FlowDesk does not use two-phase commit across services. Instead it uses:

- local ACID transactions for source-of-truth writes
- Kafka events for asynchronous propagation
- idempotent consumers for safe retries

This keeps the design operable and realistic for the stack.

## 9. Data Lifecycle and Retention

### 9.1 Notifications

- auto-expire after 30 days via MongoDB TTL

### 9.2 Outbox rows

- keep pending rows until published
- archive or purge published rows on a scheduled retention policy once they are no longer needed for replay or audit

### 9.3 Analytics snapshots

- retain enough history to power sprint and trend views
- older raw aggregates can be compacted into coarser summaries if needed later

### 9.4 Soft delete vs hard delete

- projects and tasks should prefer archive semantics for user-facing recoverability
- join/link rows may be hard-deleted when their owning aggregate is removed

## 10. Migrations and Evolution

- relational schemas should be managed with versioned migrations
- MongoDB collection evolution should be backward compatible by keeping payload parsing tolerant
- Redis keys are ephemeral and versioned through naming conventions where necessary

## 11. Backup and Recovery Expectations

- PostgreSQL backups protect user, project, task, and analytics read models
- MongoDB backups protect notification history, though losing some short-lived history is less severe than losing transactional task data
- Redis is disposable for cache and counters, but operational loss temporarily degrades performance and fanout
- Kafka retention should be long enough to reprocess consumer lag or rebuild read-side data when needed

## 12. Design Summary

FlowDesk uses polyglot persistence intentionally:

- PostgreSQL for correctness-sensitive transactional domains
- MongoDB for flexible, time-bounded notification history
- Redis for fast shared operational state

The database design supports the larger architectural rule of the system: keep
the write path correct first, then make reads, fanout, and side effects fast
and scalable around it.
