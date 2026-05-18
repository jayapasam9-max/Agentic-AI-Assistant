import { useQuery } from "@tanstack/react-query";
import { api } from "./api";
import type { ReviewSummary } from "./types";

export function useReviewsQuery() {
  return useQuery<ReviewSummary[]>({
    queryKey: ["reviews"],
    queryFn: () => api<ReviewSummary[]>("/api/public/reviews"),
    // Refresh in the background every 15s so the dashboard feels live without
    // hammering the backend.
    refetchInterval: 15_000,
  });
}
