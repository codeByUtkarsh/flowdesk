---
name: flowdesk-ui
description: >-
  Design, build, or review FlowDesk React UI using Horizon 2050 (dark glass,
  glow accents, flagship 3D on auth/hero). Use whenever creating or changing
  React components, pages, forms, dashboards, auth screens, landing/hero,
  cards, modals, Tailwind tokens, motion, or Three.js/R3F for this project.
---

# flowdesk-ui

Load **before** generating or reviewing any FlowDesk frontend. Sources (do not reinvent): repo `frontend-design-directive.md`, `FlowDesk-Frontend/docs/frontend-coding-standards.md`, `docs/cross-cutting.md`.

Reject default Tailwind SaaS (Inter + `slate-900` + `indigo-500` + opaque cards + drop-shadow “depth”).

## Visual (Horizon 2050) — default, not optional

- **Dark-first canvas:** `--canvas` ~`#07080d`, quiet radial wash — not flat black, not loud purple wallpaper.
- **Glass:** `backdrop-filter: blur` + translucent `--glass` `rgba(16,22,36,0.45)` + luminous `--glass-stroke`. Inner top highlight. **Not** opaque gray + `box-shadow` fake depth.
- **Real depth:** CSS `perspective` / `rotateX|Y` / `translateZ` **or** R3F/Three.js. Auth/hero only for persistent 3D.
- **Accents as light:** `--accent-a` electric cyan `#22e8ff`; **one** companion `--accent-b` violet `#b56bff` **or** plasma pink `#ff2fa0` — never both on one screen. Glow, rim, hairline, cursor bloom. Primary CTA = dark glass + cyan rim + outer glow. Max two hues.
- **Type:** geometric/technical display (Syne, Rajdhani, Exo 2; Orbitron sparingly) with tracking `0.04em`–`0.12em`. Body: IBM Plex Sans / Source Sans 3 / Geist Sans. Mono for IDs/metrics. No Inter-as-brand.
- **Motion is core:** page-load spring choreography (chrome → content), Framer Motion springs on routes/panels, **one** cursor-reactive surface per screen.

### Screen split (mandatory)

| Surface | Treatment |
|---|---|
| Login, signup, landing/hero | **Flagship:** animated 3D/shader background + floating glass form/panel. Shared scene; form switch is a spring, not a second WebGL world. Lazy-load R3F. |
| Dashboard, Kanban, tables, settings | Same dark/glass/glow tokens. **No persistent WebGL** behind data. Slightly more opaque glass on cards. One small signature (KPI sweep **or** count-up **or** drag lift) — not a 3D hero. |

### Guardrails

- Text **WCAG AA** on the *effective* glass stack; scrim or raise opacity if shader sits behind copy. Unreadable glow = defect.
- `prefers-reduced-motion: reduce` → static glass + still gradient; no shader/camera/springs.
- **One** signature animated/3D moment per screen — not five competing effects. No multi-canvas WebGL. No loop on every card.
- Mobile / failed WebGL / low GPU: drop R3F; keep glass + accents.

## Stack

- 3D: **Three.js / React Three Fiber** (auth + landing hero only).
- Motion: **Framer Motion** springs.
- Layout: **Tailwind with custom tokens** (`--canvas`, `--glass`, `--accent-a/b`) — not default palette.
- Shared primitives: `src/shared/ui/` → `GlassPanel`, `GlowButton`, `Field`, `Toast`, `ErrorBoundary`. No MUI-dark-theme as the look.

## Project conventions (frontend)

```text
src/features/<name>/{components,hooks,api.ts,slice.ts,types.ts}
src/shared/{api,errors,ui,config}
src/app/   # providers, router
```

- **One** Axios client (`shared/api/client.ts`): attach JWT, unwrap `ApiResponse.data`, map `error.code` → toast, 401 → refresh once. Base URL from env (`VITE_API_URL`). Paths `/api/v1/...`. No hardcoded hosts.
- **React Query** = server state (lists, task board). **Redux/`useState`** = client UI only (drawer open, active project id). Do not store server lists in Redux.
- **Zod first** in `types.ts` / `schema.ts`; `z.infer` for TS; same schema for forms.
- `ErrorCode` → toast map matching backend. Unknown code → generic copy. Never `error.stack` in UI.

## Trigger checklist (silent, before any component)

1. Dark canvas + real glass + glow accents (not flat SaaS)?
2. Auth/hero = flagship 3D+glass; dashboard = materials only, 3D pulled back?
3. Exactly **one** signature motion/3D moment chosen (name it)?
4. Form/data text still AA-readable; reduced-motion and mobile 3D degrade planned?
5. Feature folder + Axios + React Query + Zod respected if the change touches data?
