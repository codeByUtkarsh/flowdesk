# Frontend coding standards (React)

Root: `FlowDesk-Frontend/src/`.

Visual language is **Horizon 2050** — see repo-root [frontend-design-directive.md](../../frontend-design-directive.md). Structure and data-fetching rules in this file; look-and-motion in the directive.

## Folder layout — feature-based (mirrors backend)

```text
src/
  app/                 # providers, router, store
  shared/
    api/               # axios instance, unwrap, types
    errors/            # ErrorCode → toast map
    ui/                # ErrorBoundary, primitives
    config/            # env
  features/
    task/
      components/
      hooks/
      api.ts
      slice.ts         # client-only UI state, if any
      types.ts         # Zod-inferred types
    auth/
    project/
    notification/
```

Do not create a global `components/tasks` + `services/taskApi` split that ignores features.

## HTTP

- **One** Axios instance (`shared/api/client.ts`).
- Interceptors:
  1. Attach access token
  2. Unwrap `ApiResponse.data` on success
  3. Map `error.code` through the toast mapper; reject with a typed error
  4. On 401, try refresh once, then logout
- Base URL from `import.meta.env.VITE_API_URL` (or `REACT_APP_API_URL`). **No hardcoded hosts.**
- All calls hit `/api/v1/...` via the gateway.

## State

- **Server state:** React Query **or** RTK Query — pick one per app and stay with it. Default: React Query.
- **Client state:** Redux Toolkit or `useState` — auth session UI, active project id, notification drawer open. Do not put task lists in Redux if they already live in React Query.

## Validation

- **Zod first.** Schema in `types.ts` (or `schema.ts`); TypeScript types via `z.infer`.
- Same schema for forms and for parsing API payloads where practical.

## Errors

- Central `errorCodeToMessage: Record<ErrorCode, string>` matching backend `ErrorCode`.
- Unknown code → generic toast. Never `error.stack` in the UI.
- `ErrorBoundary` at app root for render crashes.

## Boilerplate before features

1. Env config
2. Axios client + interceptors
3. `ErrorBoundary`
4. ErrorCode mapper
5. Then `features/auth/...`
