# FlowDesk Frontend Design Directive — “Horizon 2050”

**Status:** binding for all UI work  
**Root file:** this document (`frontend-design-directive.md`)  
**Code layout:** still follows [FlowDesk-Frontend/docs/frontend-coding-standards.md](FlowDesk-Frontend/docs/frontend-coding-standards.md) (feature folders, Axios, Query vs Redux). This file is **visual and motion only**.

FlowDesk must not look like a generic 2020s flat SaaS dashboard (white cards, 8px radius, indigo buttons, Inter everywhere). It should feel like a **control surface from 2050**: dark, glass, light as a material, depth you can believe.

---

## 1. North star

A Kanban and auth experience that could sit on a glass desk in a dim ops room — not a Notion clone with a dark theme toggle.

**Auth screens = flagship.**  
**App chrome / dashboards / boards = clarity first, same materials, almost no 3D.**

---

## 2. Color and light

### Canvas

- Default is **dark-first**. Page background is near-black with a slow, low-contrast radial wash — not a flat `#000` void and not a loud purple gradient wallpaper.
- Suggested tokens (implement as CSS variables; hex may shift ±5% as long as contrast holds):

| Token | Role | Notes |
|---|---|---|
| `--canvas` | Page void | `#07080d` range |
| `--glass` | Panel fill | `rgba(16, 22, 36, 0.45)` + backdrop blur |
| `--glass-stroke` | Panel edge | `rgba(180, 220, 255, 0.18)` |
| `--text` | Primary copy | `#e8f4ff` or warmer off-white |
| `--text-muted` | Secondary | ~70% of `--text` |
| `--accent-a` | Cyan (primary glow) | electric cyan, e.g. `#22e8ff` |
| `--accent-b` | Violet or plasma pink | pick **one** companion, e.g. `#b56bff` **or** `#ff2fa0` — not both on the same screen |

### Accents are light sources, not fills

- Saturated cyan / violet / plasma pink appear as **glow, rim light, hairline, cursor bloom** — not as large solid button fills.
- Primary actions: dark glass + cyan rim + soft outer glow. Hover increases glow radius, not a new flat color.
- Never more than **two** accent hues on one screen.

### Contrast guardrail

- Body and labels on glass: **WCAG AA** against the *effective* background (sample the frosted stack, not the void behind it).
- If a gradient/shader sits behind text, add a **scrim** (dark translucent layer) or increase panel opacity. Unreadable glow text is a defect, not a vibe.

---

## 3. Materials: glassmorphism with real depth

### Glass panels

- `backdrop-filter: blur(...)` (plus `-webkit-`) on auth form, nav, modals, command palettes.
- Translucent fill + 1px luminous stroke + inner highlight on the top edge.
- Do **not** fake depth with only `box-shadow: 0 20px 40px rgba(0,0,0,.4)` on opaque cards.

### Real depth (auth / hero only)

- CSS `perspective` + `transform: rotateX/Y/translateZ` on the floating form **or**
- React Three Fiber / Three.js full-viewport background.
- Dashboard **must not** run a persistent WebGL scene behind tables and boards.

### Degradation

- `prefers-reduced-motion: reduce` → no shader animation, no 3D camera drift, no spring choreography; static glass + still gradient is enough.
- Mobile / low GPU: disable R3F/WebGL; keep glass + accents. Detect via matchMedia, `navigator.hardwareConcurrency`, or a failed WebGL context.

---

## 4. Typography

| Role | Character | Notes |
|---|---|---|
| Headings / display | Geometric, technical, slightly machine | e.g. *Syne*, *Orbitron* (sparingly), *Rajdhani*, *Exo 2*, or *Geist* if it stays sharp — not playful scripts |
| Body / UI | Clean, high x-height | e.g. *IBM Plex Sans*, *Source Sans 3*, *Geist Sans* |
| Mono / IDs | Tabular | task ids, timestamps, metrics |

- Headings: **generous letter-spacing** (`0.04em`–`0.12em` on display sizes).
- Do not use Inter + default Tailwind as the brand.
- Line length on auth copy stays short; dashboards keep 14–16px body for data.

---

## 5. Motion

Motion is **how the product feels**, not a lottie in the corner.

### Required

- **Page-load choreography:** staggered fade/rise of chrome → content (springs, not linear 200ms fades on everything).
- **Spring-based** route and panel transitions (e.g. Motion/Framer Motion).
- **Cursor-reactive** element on **one** surface per screen (auth: form tilt or shader follow; dashboard: subtle spotlight on the focused card — not both a 3D globe *and* a trailing blob).

### Signature moment

**Exactly one** signature animated or 3D moment per screen.

| Screen | Signature (pick one) |
|---|---|
| Login / signup | Full-bleed 3D or shader field + floating glass form |
| Dashboard | Soft light sweep on KPI glass **or** number count-up — not a 3D hero |
| Kanban | Column appear + card lift on drag — no particle field |
| Notifications | Bell pulse / list slide — no second 3D canvas |

### Forbidden

- Autoplaying loops on every card.
- Confetti, bounce on every click, parallax on tables.
- Multiple WebGL canvases.

`prefers-reduced-motion`: instant states, no signature loop.

---

## 6. Auth (flagship)

Must include:

1. Animated **3D or shader** background (particles, lattice, plasma field, or CSS 3D grid — actually moving).
2. **Floating glass** login/signup panel (centered or offset), slight `translateZ` / tilt toward cursor (capped, accessible).
3. Accent glow as if the form is lit by the scene, not a CSS `background: cyan`.
4. Clear primary field contrast; placeholders never equal body color.

Signup and login share the scene; switch forms with a spring, do not reload a second 3D world.

---

## 7. Dashboards, boards, settings

- Same **tokens**: dark canvas, glass side nav / top bar, cyan/violet as glow on active nav and primary buttons.
- **Pull back 3D.** Data density wins. Kanban columns = glass rails; cards = slightly more opaque glass so title + meta stay crisp.
- Motion: layout springs, drag, stale-version 409 as a sharp but on-brand toast — not a full-screen shader.
- Charts: dark grid, accent as series glow; no rainbow categorical palettes.

---

## 8. Components (minimum set)

When scaffolding UI, implement these as shared primitives under `src/shared/ui/`:

- `GlassPanel`
- `GlowButton` (rim + glow, not flat fill)
- `Field` (label + input on glass, visible caret)
- `Toast` wired to `ErrorCode` mapper
- `ErrorBoundary` (already in coding standards)

Do not introduce a third-party “MUI dark theme” as the look.

---

## 9. Implementation notes (when code exists)

- CSS variables on `:root` / `[data-theme="horizon"]`.
- Fonts via self-host or official CDN; document in `index.html`.
- R3F only on auth (and optional marketing hero). Lazy-load the canvas.
- Tailwind is allowed if tokens map to this directive — default `slate-900` + `indigo-500` is a **reject**.

---

## 10. Review checklist (every PR that touches UI)

- [ ] Dark-first; not a recolored SaaS kit
- [ ] Glass is translucent + blur, not opaque gray + shadow
- [ ] At most two accent hues; used as light, not paint
- [ ] Display font on headings, tracking on display sizes
- [ ] One signature motion/3D moment only
- [ ] Auth has 3D/shader + floating glass form
- [ ] Dashboard readable; no persistent 3D behind data
- [ ] Text AA on glass; scrim if needed
- [ ] `prefers-reduced-motion` respected
- [ ] Mobile: 3D off or simplified, layout still usable
