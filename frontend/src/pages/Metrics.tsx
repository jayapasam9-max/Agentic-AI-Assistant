import { Badge } from "@/components/ui/badge";

// Day 1 placeholder: real chart lands on Day 4 from GET /api/public/metrics/daily.
export default function Metrics() {
  return (
    <section className="space-y-4">
      <header className="flex items-baseline justify-between">
        <div>
          <h1 className="text-xl font-semibold">Metrics</h1>
          <p className="text-sm text-muted-foreground">
            Daily review volume, token usage, and estimated cost.
          </p>
        </div>
        <Badge tone="info">Day 4: wire to /api/public/metrics/daily</Badge>
      </header>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {["Total reviews", "Success rate", "Tokens", "Est. cost"].map((label) => (
          <div
            key={label}
            className="rounded-lg border border-border bg-card p-4"
          >
            <div className="text-xs uppercase tracking-wide text-muted-foreground">
              {label}
            </div>
            <div className="mt-2 text-2xl font-semibold">—</div>
          </div>
        ))}
      </div>
    </section>
  );
}
