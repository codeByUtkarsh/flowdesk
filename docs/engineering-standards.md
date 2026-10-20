# FlowDesk engineering standards

Canonical rules for how we build. Agents load the same content as Cursor rules
under `.cursor/rules/`. Humans use these markdown files.

| Track | Doc | Cursor rule |
|---|---|---|
| Backend | [FlowDesk-Backend/docs/standards/backend.md](../FlowDesk-Backend/docs/standards/backend.md) | `.cursor/rules/backend-java.mdc` |
| Frontend | [FlowDesk-Frontend/docs/frontend-coding-standards.md](../FlowDesk-Frontend/docs/frontend-coding-standards.md) | `.cursor/rules/frontend-react.mdc` |
| UI / visual | [frontend-design-directive.md](../frontend-design-directive.md) (Horizon 2050) | `.cursor/rules/frontend-horizon-2050.mdc` + skill `.cursor/skills/flowdesk-ui/` |
| Cross-cutting | [cross-cutting.md](cross-cutting.md) | `.cursor/rules/api-contract.mdc` |

**Order of work:** API contract → shared envelope/error codes → boilerplate (handlers, Axios, ErrorBoundary) → feature code. Never the reverse.
