import { useEffect, useRef } from "react";
import { Radio } from "lucide-react";
import type { StreamEvent, StreamEventType } from "@/lib/types";
import type { StreamState } from "@/lib/sse";
import { cn } from "@/lib/utils";

const TYPE_LABELS: Record<StreamEventType, string> = {
  JOB_STARTED: "job started",
  REASONING_CHUNK: "reasoning",
  TOOL_CALL_STARTED: "tool →",
  TOOL_CALL_COMPLETED: "tool ✓",
  FINDING_EMITTED: "finding",
  JOB_COMPLETED: "job completed",
  JOB_FAILED: "job failed",
};

const TYPE_COLORS: Record<StreamEventType, string> = {
  JOB_STARTED: "text-sky-300",
  REASONING_CHUNK: "text-muted-foreground",
  TOOL_CALL_STARTED: "text-amber-300",
  TOOL_CALL_COMPLETED: "text-emerald-300",
  FINDING_EMITTED: "text-fuchsia-300",
  JOB_COMPLETED: "text-emerald-300",
  JOB_FAILED: "text-red-300",
};

interface EventStreamProps {
  events: StreamEvent[];
  state: StreamState;
  /** Whether the review is in a terminal state (won't get more events). */
  terminal: boolean;
}

export function EventStream({ events, state, terminal }: EventStreamProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll only if user hasn't scrolled up — feels less jarring.
  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) return;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    if (nearBottom) el.scrollTop = el.scrollHeight;
  }, [events.length]);

  return (
    <div className="overflow-hidden rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between border-b border-border bg-muted/30 px-4 py-2 text-xs">
        <div className="flex items-center gap-2 font-medium">
          <Radio className="h-3.5 w-3.5" />
          Live event stream
        </div>
        <ConnectionDot state={state} terminal={terminal} />
      </div>

      <div
        ref={scrollerRef}
        className="h-72 overflow-y-auto px-4 py-3 font-mono text-xs leading-relaxed"
        aria-live="polite"
      >
        {events.length === 0 ? (
          <EmptyHint state={state} terminal={terminal} />
        ) : (
          <ul className="space-y-1">
            {events.map((e, i) => (
              <li key={i} className="flex gap-3">
                <span className="shrink-0 text-muted-foreground">
                  {timeOnly(e.receivedAt)}
                </span>
                <span className={cn("shrink-0 w-28", TYPE_COLORS[e.type])}>
                  {TYPE_LABELS[e.type]}
                </span>
                <span className="min-w-0 break-words text-foreground/80">
                  {previewPayload(e)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function ConnectionDot({
  state,
  terminal,
}: {
  state: StreamState;
  terminal: boolean;
}) {
  if (terminal) {
    return (
      <span className="flex items-center gap-1.5 text-muted-foreground">
        <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground/60" />
        stream ended
      </span>
    );
  }
  if (state === "open") {
    return (
      <span className="flex items-center gap-1.5 text-emerald-300">
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-400" />
        live
      </span>
    );
  }
  if (state === "connecting") {
    return (
      <span className="flex items-center gap-1.5 text-amber-300">
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-400" />
        connecting…
      </span>
    );
  }
  return (
    <span className="flex items-center gap-1.5 text-muted-foreground">
      <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground/60" />
      disconnected
    </span>
  );
}

function EmptyHint({
  state,
  terminal,
}: {
  state: StreamState;
  terminal: boolean;
}) {
  if (terminal) {
    return (
      <p className="text-muted-foreground">
        This review finished before you opened the page, so there are no live
        events to show. Findings and tool calls are below.
      </p>
    );
  }
  if (state === "open") {
    return <p className="text-muted-foreground">Waiting for the agent…</p>;
  }
  return <p className="text-muted-foreground">Connecting to event stream…</p>;
}

function timeOnly(iso: string): string {
  // 14:32:01 (24h, no date — keeps the column narrow)
  return new Date(iso).toLocaleTimeString(undefined, { hour12: false });
}

function previewPayload(e: StreamEvent): string {
  if (!e.payload) return "";
  if (e.type === "FINDING_EMITTED") {
    try {
      const f = JSON.parse(e.payload) as {
        severity?: string;
        filePath?: string;
        lineNumber?: number | null;
        message?: string;
      };
      const where = f.filePath
        ? `${f.filePath}${f.lineNumber ? `:${f.lineNumber}` : ""}`
        : "";
      return `[${f.severity ?? "?"}] ${where} — ${f.message ?? ""}`;
    } catch {
      return e.payload.slice(0, 200);
    }
  }
  // Other events have small free-form payloads (e.g., error message, "").
  return e.payload.slice(0, 240);
}
