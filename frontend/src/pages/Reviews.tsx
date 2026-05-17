import { Badge } from "@/components/ui/badge";

// Day 1 placeholder: real data lands on Day 2 from GET /api/public/reviews.
export default function Reviews() {
  return (
    <section className="space-y-4">
      <header className="flex items-baseline justify-between">
        <div>
          <h1 className="text-xl font-semibold">Reviews</h1>
          <p className="text-sm text-muted-foreground">
            Recent pull-request reviews the agent has executed.
          </p>
        </div>
        <Badge tone="info">Day 2: wire to /api/public/reviews</Badge>
      </header>
      <div className="rounded-lg border border-dashed border-border p-10 text-center text-sm text-muted-foreground">
        Reviews table will live here.
      </div>
    </section>
  );
}
