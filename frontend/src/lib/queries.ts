import { useQuery } from "@tanstack/react-query";
import { api } from "./api";
import type { ReviewDetail, ReviewSummary } from "./types";

export function useReviewsQuery() {
  return useQuery<ReviewSummary[]>({
    queryKey: ["reviews"],
    queryFn: () => api<ReviewSummary[]>("/api/public/reviews"),
    // Refresh in the background every 15s so the dashboard feels live without
    // hammering the backend.
    refetchInterval: 15_000,
  });
}

export function useReviewDetailQuery(id: string | undefined) {
  return useQuery<ReviewDetail>({
    enabled: Boolean(id),
    queryKey: ["reviews", id],
    queryFn: () => api<ReviewDetail>(`/api/public/reviews/${id}`),
    // Detail also refreshes — running reviews accumulate findings/tool calls
    // that aren't on the SSE stream once the agent finishes (e.g., post-run
    // GitHub comment status flips).
    refetchInterval: (q) => {
      const status = q.state.data?.summary.status;
      return status === "RUNNING" || status === "QUEUED" ? 5_000 : false;
    },
  });
}
