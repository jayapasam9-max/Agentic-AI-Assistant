import { Badge } from "@/components/ui/badge";
import type { Severity } from "@/lib/types";

const TONES: Record<Severity, React.ComponentProps<typeof Badge>["tone"]> = {
  INFO: "neutral",
  LOW: "info",
  MEDIUM: "warning",
  HIGH: "warning",
  CRITICAL: "danger",
};

export function SeverityBadge({ severity }: { severity: Severity }) {
  return (
    <Badge tone={TONES[severity]} className="font-mono">
      {severity}
    </Badge>
  );
}
