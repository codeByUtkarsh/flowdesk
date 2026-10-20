# FlowDesk — Session Zero

You have a blueprint and (per your notes) a feature-level build spec ready
for an AI coding agent. What you don't have yet is the *engineering
scaffolding* that makes the next 8 weeks actually check your own work instead
of trusting that the code is right. That scaffolding goes in first, before a
single line of business logic. This is Session Zero — it fits in one
10PM–2AM block.

Why this order: a code-quality check and an architecture check are only
useful if they exist *before* the code they're supposed to catch problems
in. Bolting Checkstyle/ArchUnit on in week 6 means five weeks of code never
got checked.

---

## 1. The tooling checklist (what "code quality check, architecture check" means concretely)

| Concern | Tool | What it actually catches |
|---|---|---|
| Formatting consistency | Spotless (or google-java-format) | Bikeshedding eliminated; CI fails on unformatted code |
| Code quality rules | Checkstyle | Unused imports, missing braces, complexity thresholds — build fails on violation |
| Architecture conformance | **ArchUnit** | The thing most tutorials skip. Writes a *test* that fails if a controller calls a repository directly, or TaskCommandService calls TaskQueryService, or any layering rule you define gets violated. This is how you prove the CQRS split is real, not just folder-naming. |
| Integration correctness | Testcontainers | Tests run against a real Postgres/Kafka/Redis/Mongo in Docker, not mocks — catches the bugs mocks hide |
| Build discipline | GitHub Actions | `mvn verify` (build + test + Checkstyle + ArchUnit) runs on every push, even solo — this is what makes your git history itself part of the interview story |
| Decision discipline | ADR template + rule | No new datastore/pattern enters the stack without a written ADR |

---

## 2. The copy-paste prompt (feed this to Claude Code / your coding agent, at the very start, before your feature spec)

```
You are setting up ONLY the engineering foundation for a project called
FlowDesk. Do not implement any business logic yet.

Context: FlowDesk is a Java/Spring Boot microservices project — auth-service,
project-service, task-service, notification-service, analytics-service —
plus a React/Redux frontend, using Kafka, Redis, PostgreSQL, MongoDB, and
WebSockets. I will provide the full architecture blueprint and feature spec
separately, after this scaffolding is done.

Do exactly these 8 things, in order, and stop:

1. Scaffold the git monorepo folder structure: infrastructure/, common-lib/,
   api-gateway/, auth-service/, project-service/, task-service/,
   notification-service/, analytics-service/, docs/adr/, frontend/.

2. Set up a parent Maven POM with dependency management shared across all
   5 service modules plus common-lib.

3. Add to the parent POM:
   - Spotless (or google-java-format) enforced on `mvn verify`
   - Checkstyle with a ruleset that fails the build on violation
   - ArchUnit as a test dependency in every service module

4. Add one starter ArchUnit test per service enforcing: controllers only
   call services; services only call repositories; no service reaches into
   another service's repository or entity package directly.

5. Write infrastructure/docker-compose.yml for Postgres, Redis, MongoDB,
   Kafka + Zookeeper, Eureka — infra only, no application services running yet.

6. Add a GitHub Actions workflow that runs `mvn verify` (build + unit tests +
   Checkstyle + ArchUnit) on every push to any branch.

7. Create docs/adr/ with an ADR template (Context / Decision / Consequences)
   and ADR-000, pre-filled ONLY with the question: "Why microservices for a
   project with no real production traffic?" Leave Decision and Consequences
   blank — I will fill those in myself.

8. Create a root README with: project name, one-paragraph description,
   "Status: in development," and a placeholder "System Design Concepts
   Index" table with the concept-name column filled and the file-path
   column marked TODO.

Do not write Auth service logic, JWT handling, entities, or any controller
code in this pass. Stop once the scaffold and tooling are in place and
`mvn verify` passes green with zero application code. Show me the resulting
folder tree and confirm the build is green before doing anything else.
```

---

## 3. Weekly Definition of Done (reuse for every week of the 8-week plan)

Copy this into each week's checklist. A week isn't done until every box is checked — not when the feature "seems to work."

- [ ] Feature works end-to-end via Postman/browser, not just green unit tests
- [ ] Unit tests cover the write path: happy path + at least one failure/edge case
- [ ] ArchUnit passes — no new layering violations introduced
- [ ] Checkstyle/Spotless clean, CI green
- [ ] Failure-mode manually tested: kill the dependency this week touches (Kafka/Redis/Mongo/DB), observe and note the actual behavior
- [ ] Correlation ID visible in logs for every new endpoint
- [ ] You can explain, unaided, why each new pattern this week exists and what it costs
- [ ] ADR written if a new datastore/tech/pattern was introduced; README updated if scope changed

---

## 4. Tonight's plan (Session Zero, ~4 hours)

- **Hour 1** — Read `flowdesk-requirements.md`. Edit it in your own words — change
  numbers you disagree with, add anything missing. Don't skip this by accepting it as-is.
- **Hour 2** — Run the Session Zero prompt above through your coding agent.
  Watch what it scaffolds; don't just accept a green checkmark — open the
  generated ArchUnit test and confirm you understand what it's actually asserting.
- **Hour 3** — `docker-compose up -d`, confirm Postgres/Redis/Mongo/Kafka/Eureka
  all come up clean. Fix anything that doesn't.
- **Hour 4** — Write ADR-000 yourself: make the honest case for microservices
  on a project with zero real traffic. This is the answer you'll actually be
  asked for in an interview — write it now while the reasoning is fresh, not
  in week 8 as an afterthought.

Milestone for tonight: `mvn verify` green, `docker-compose up -d` clean,
ADR-000 written in your own words, zero feature code. That's a real, checkable
stopping point — not "I'll keep going until I run out of steam."

Week 1 of the original blueprint's build plan (Auth service, JWT, Postman
test) starts next session, on top of this foundation.
