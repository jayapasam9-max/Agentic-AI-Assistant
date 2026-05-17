# Phase 5 — React Dashboard (resume edition)

Status: planning
Owner: @jayapasam9-max
Target window: **5 evenings (~15 hours total)**
Cost: **$0/month**
North-star metric: a recruiter clicks the live URL, watches 10 seconds of streaming agent reasoning, and reads the README.

## What this is, and what it is not

This is a portfolio dashboard for a full-stack (React + Java/Spring) role. Every decision is filtered through: *does this make the interview screen more likely?*

It **is not** a production app. We are not building auth, retention jobs, materialized views, multi-tenant features, or a full observability stack. Those land in a hypothetical Phase 5.5 only if the project ever turns commercial.

## What a recruiter / interviewer actually sees

In order, and weighted by how much it matters:

1. **The live URL works on their phone in under 2 seconds.** Above all else.
2. **A screenshot or GIF in the README** that makes them want to click. Streaming reasoning is the hook.
3. **A README that explains the architecture in one diagram and 6 bullets.** Tech stack badges at the top.
4. **One "wow" interaction** — for us, the live SSE reasoning stream.
5. **Clean, typed code with a sane folder layout** when they peek at the source.
6. **A polished list/table view** that proves you can do the boring 80% well.

Everything in the plan below maps to one of those six items. Nothing else makes the cut.

## The story we are telling

"I designed and built an end-to-end agentic system: a Spring Boot service runs an LLM agent with five tools against pull-request diffs, streams its reasoning over Server-Sent Events, and a React + TypeScript dashboard visualizes runs live and shows aggregate metrics. Deployed on a $0 free tier."

That sentence is the resume bullet. The dashboard exists to make that sentence believable in ten seconds.

## Stack (no surprises)

Vite + React 18 + TypeScript, Tailwind + shadcn/ui, TanStack Query, Recharts, lucide-react. Deployed to Vercel Hobby. Backend is the existing Spring Boot on Render with the existing SSE endpoint. No auth — the dashboard is read-only and public; it talks to a read-only `/api/public/*` namespace we add to the backend.

## Five-day build

### Day 1 — Scaffold and ship a live URL (3h)
- `frontend/` with Vite + React + TS + Tailwind + shadcn baseline. Strict TS config.
- App shell: header with logo and a GitHub-link button, sidebar with two routes (`/reviews`, `/metrics`), responsive.
- Connect the repo to Vercel; first deploy.
- Output: a live `*.vercel.app` URL with a "Hello" page. **Day 1 is not done until that URL is shareable.**

### Day 2 — Reviews list (3h)
- Backend: add `GET /api/public/reviews` returning the last 50 reviews (no auth, no pagination, no filters — keep it small). Open CORS for the Vercel origin.
- Frontend: `/reviews` page with a table — Repo, PR, Title, Status badge, Started (relative time), Duration, Tokens. Empty / loading / error states. Click a row → `/reviews/:id`.
- Use TanStack Query with `staleTime: 30s`; show a tiny "live" dot in the header.
- Output: a real table populated from real data, looks good on mobile.

### Day 3 — Live review detail with SSE (4h, the centerpiece)
- Backend: confirm `/api/reviews/{id}/stream` works through Render's proxy from the Vercel origin; add `/api/public/reviews/{id}` for the static fields.
- Frontend: `/reviews/:id` with three panes — header (repo / PR / status / link out), **streaming reasoning** (auto-scrolling, monospace, token-by-token via `EventSource`), tool-call timeline below.
- One nice touch: show the system prompt collapsed at the top, expandable. Signals you understand the agent.
- Output: open a running review and watch tokens stream in. Record a 10-second screen GIF the moment this works.

### Day 4 — One analytics chart that pays off (2h)
- Backend: `GET /api/public/metrics/daily?days=14` returning `[{day, reviews, tokens_in, tokens_out, cost_usd}]`. SQL aggregation done on the fly; no MVs needed at this volume.
- Frontend: `/metrics` page with four stat cards (total reviews / success rate / total tokens / est. cost) and one Recharts bar+line combo: daily reviews bars, cost line on a second axis.
- Output: a dashboard view that makes the README screenshot.

### Day 5 — Polish, README, demo GIF, deploy (3h)
- Polish: dark mode toggle, mobile breakpoints, loading skeletons everywhere, Sentry free wired up, favicon, page titles.
- A11y quick pass: keyboard nav on the table, focus rings, semantic HTML.
- One Vitest test per page (smoke level — proves you set up testing).
- README in `frontend/`: tech badges, live URL, one architecture diagram (the same one from `PHASE_5_DASHBOARDS.md`), 6 bullets on key decisions, 3 screenshots, the 10-second GIF from Day 3, run instructions.
- Update the top-level repo `README.md`: check off the dashboard row, add the live URL and a hero screenshot near the top.
- Output: someone landing on the GitHub repo from a job application sees a live URL, a GIF of streaming reasoning, and a tight architecture story — in under 30 seconds.

## Backend changes (the smallest possible diff)

Three additive endpoints, no schema changes:

- `GET /api/public/reviews` — last 50 reviews, ordered by `created_at desc`.
- `GET /api/public/reviews/{id}` — single review with `tool_calls` and `findings`.
- `GET /api/public/metrics/daily?days=14` — daily aggregate, computed inline.

The existing `/api/reviews/{id}/stream` SSE endpoint is moved (or aliased) under `/api/public/` and gets CORS headers for the Vercel origin. The agent path is untouched.

## Cuts (and why each one is the right call here)

| Cut | Why |
|---|---|
| GitHub OAuth / auth | Half a day for zero recruiter signal. Read-only public data is fine; the repo it shows is yours. |
| Materialized views, retention jobs | Premature at portfolio data volumes. SQL aggregate on the fly. |
| SSE reconnect logic, polling fallback | Happy path is enough for a demo. Document the limitation in the README; that itself is a signal. |
| Policy editor / multi-tenant | Out of scope for the story we're telling. |
| Prometheus / Grafana | Free tier won't host it; the one Recharts chart covers the analytics story. |
| Full E2E test suite | One smoke test per page is the right cost/benefit. |

## Done criteria

- Live URL on Vercel, sub-2s TTFB on a mobile network.
- README has: badges, live URL, architecture diagram, 6 bullets, 3 screenshots, one < 15s GIF of the SSE stream.
- Top-level repo README links to the live URL above the fold.
- A recruiter could land on the repo and pitch you internally without reading any code.

## Resume bullets this unlocks

- *Built a TypeScript React dashboard (Vite, Tailwind, TanStack Query, Recharts) that visualizes a live LLM agent reasoning over Server-Sent Events; deployed on Vercel Hobby with zero infra cost.*
- *Designed and shipped an end-to-end agentic code-review system — Spring Boot + LangChain4j + Claude with native tool-calling, Postgres/pgvector for semantic search over repo history, Kafka for async orchestration, React for the operator UI.*
- *Optimized for the free tier: Render + Neon backend, Vercel frontend, $0/month, while serving live streaming reasoning to the dashboard.*

## What we're explicitly leaving for later

A 5.5 phase (auth, policy editor, retention, real metrics aggregations) lives in `PHASE_5_5_PRODUCTIONIZE.md` once Phase 5 ships. It exists in this repo as a TODO — that, too, is a signal interviewers like.
