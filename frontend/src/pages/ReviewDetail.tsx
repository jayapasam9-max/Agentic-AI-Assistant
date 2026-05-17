import { useParams } from "react-router-dom";
import { Badge } from "@/components/ui/badge";

// Day 1 placeholder: real SSE stream lands on Day 3.
export default function ReviewDetail() {
  const { id } = useParams<{ id: string }>();
  return (
    <section className="space-y-4">
      <header>
        <h1 className="text-xl font-semibold">Review #{id}</h1>
        <p className="text-sm text-muted-foreground">
          Live reasoning stream and tool-call timeline.
        </p>
      </header>
      <Badge tone="warning">Day 3: wire EventSource to /api/reviews/:id/stream</Badge>
    </section>
  );
}
