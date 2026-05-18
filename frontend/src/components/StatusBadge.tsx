import { Badge } from "@/components/ui/badge";
import type { ReviewStatus } from "@/lib/types";
import { cn } from "@/lib/utils";

const TONES: Record<ReviewStatus, React.ComponentProps<typeof Badge>["tone"]> = {
  QUEUED: "neutral",
  RUNNING: "info",
  COMPLETED: "success",
  FAILED: "danger",
};

const LABELS: Record<ReviewStatus, string> = {
  QUEUED: "Queued",
  RUNNING: "Running",
  COMPLETED: "Completed",
  FAILED: "Failed",
};

export function StatusBadge({ status }: { status: ReviewStatus }) {
  return (
    <Badge tone={TONES[status]} className="font-medium">
      <span
        className={cn(
          "h-1.5 w-1.5 rounded-full bg-current",
          status === "RUNNING" && "animate-pulse",
        )}
      />
      {LABELS[status]}
    </Badge>
  );
}
