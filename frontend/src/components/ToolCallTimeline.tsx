import { Check, X, Wrench } from "lucide-react";
import type { ToolCall } from "@/lib/types";
import { formatDuration } from "@/lib/format";
import { cn } from "@/lib/utils";

export function ToolCallTimeline({ calls }: { calls: ToolCall[] }) {
  if (calls.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
        No tool calls recorded yet.
      </div>
    );
  }

  return (
    <ol className="relative space-y-3 border-l border-border pl-6">
      {calls.map((c) => (
        <li key={c.id} className="relative">
          <span
            className={cn(
              "absolute -left-[31px] top-1 grid h-5 w-5 place-items-center rounded-full border border-border bg-background",
              c.success ? "text-emerald-300" : "text-red-300",
            )}
          >
            {c.success ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
          </span>
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1 text-sm">
            <span className="flex items-center gap-1.5 font-mono font-medium">
              <Wrench className="h-3.5 w-3.5 text-muted-foreground" />
              {c.toolName}
            </span>
            <span className="text-xs text-muted-foreground">
              iter {c.iteration} · {formatDuration(c.durationMs)}
            </span>
          </div>
        </li>
      ))}
    </ol>
  );
}
