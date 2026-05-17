# Operator dashboard

React + TypeScript dashboard for the Agentic Code Review Assistant. Watches reviews stream live over Server-Sent Events and surfaces token usage and cost over time.

**Stack:** Vite, React 18, TypeScript (strict), Tailwind, shadcn-style primitives, TanStack Query, Recharts, React Router.

## Quickstart

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open http://localhost:5173.

In dev the Vite proxy forwards `/api/*` to `VITE_BACKEND_URL` (default `http://localhost:8080`) so the SPA and the Spring Boot backend can run on different ports without CORS pain.

## Scripts

| Script | What it does |
|---|---|
| `npm run dev` | Vite dev server with HMR on :5173 |
| `npm run build` | Type-check (`tsc -b`) then build with Vite |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | ESLint over `src/` |
| `npm run typecheck` | `tsc -b --noEmit` |
| `npm run format` | Prettier write |

## Layout

```
frontend/
├── index.html
├── public/                static assets
├── src/
│   ├── components/
│   │   ├── Layout.tsx     app shell — header, sidebar, footer
│   │   └── ui/            shadcn-style primitives (Button, Badge)
│   ├── pages/
│   │   ├── Reviews.tsx        list view  (Day 2)
│   │   ├── ReviewDetail.tsx   live SSE viewer (Day 3)
│   │   └── Metrics.tsx        analytics page (Day 4)
│   ├── lib/
│   │   ├── api.ts         fetch wrapper
│   │   └── utils.ts       `cn()` for class composition
│   ├── App.tsx            routes
│   └── main.tsx           React + Router + Query bootstrap
├── tailwind.config.js
├── tsconfig.json          strict TS, `@/*` path alias
├── vercel.json            SPA rewrites
└── vite.config.ts         `/api` proxy, `@/` alias
```

## Deploying to Vercel

1. Connect the repo on Vercel and set the project root to `frontend/`.
2. Build command: `npm run build`. Output directory: `dist`.
3. Set `VITE_API_BASE_URL` in the Vercel project to the Render backend origin (e.g. `https://agentic-ai-assistant.onrender.com`).
4. Every push to `main` deploys to production; every PR gets a preview URL.

## Day-by-day plan

This repo's `PHASE_5_DASHBOARDS.md` has the five-day build. Day 1 (this scaffold) is shipped; Day 2 wires the reviews list, Day 3 wires the SSE stream, Day 4 wires the metrics chart, Day 5 is polish + GIF + README.
