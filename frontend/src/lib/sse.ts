import { useEffect, useState } from "react";
import type { StreamEvent, StreamEventType } from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";

const EVENT_TYPES: readonly StreamEventType[] = [
  "JOB_STARTED",
  "REASONING_CHUNK",
  "TOOL_CALL_STARTED",
  "TOOL_CALL_COMPLETED",
  "FINDING_EMITTED",
  "JOB_COMPLETED",
  "JOB_FAILED",
];

export type StreamState = "connecting" | "open" | "closed";

interface UseReviewStreamResult {
  events: StreamEvent[];
  state: StreamState;
}

/**
 * Subscribe to /api/public/reviews/{id}/stream and accumulate events.
 *
 * <p>Browser EventSource auto-reconnects on transient drops; the slim Day 3
 * plan accepts that as the happy-path and skips a polling fallback.
 */
export function useReviewStream(id: string | undefined): UseReviewStreamResult {
  const [events, setEvents] = useState<StreamEvent[]>([]);
  const [state, setState] = useState<StreamState>("connecting");

  useEffect(() => {
    if (!id) {
      setState("closed");
      return;
    }

    setEvents([]);
    setState("connecting");

    const es = new EventSource(`${API_BASE}/api/public/reviews/${id}/stream`);

    const onOpen = () => setState("open");
    const onError = () => setState("closed");

    es.addEventListener("open", onOpen);
    es.addEventListener("error", onError);

    // The backend uses named SSE events (SseEmitter.event().name(type.name())),
    // so we must subscribe to each type explicitly — EventSource.onmessage
    // only receives events that have no `event:` line.
    const handlers: Array<[string, (e: MessageEvent) => void]> = EVENT_TYPES.map(
      (type) => {
        const handler = (e: MessageEvent) => {
          // The data is the full ReviewEvent record — { jobId, type, payload }.
          // We only need `payload` here; the outer `type` is what got us into
          // this handler.
          let payload = "";
          try {
            const parsed = JSON.parse(e.data) as { payload?: string };
            payload = parsed.payload ?? "";
          } catch {
            payload = String(e.data ?? "");
          }
          setEvents((prev) => [
            ...prev,
            { receivedAt: new Date().toISOString(), type, payload },
          ]);
        };
        es.addEventListener(type, handler as EventListener);
        return [type, handler];
      },
    );

    return () => {
      es.removeEventListener("open", onOpen);
      es.removeEventListener("error", onError);
      handlers.forEach(([type, handler]) =>
        es.removeEventListener(type, handler as EventListener),
      );
      es.close();
    };
  }, [id]);

  return { events, state };
}
