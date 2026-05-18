// Mirrors backend com.codereview.agent.api.dto.ReviewSummaryDto.
// Keep these in sync by hand; both files reference the same JSON shape.

export type ReviewStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface ReviewSummary {
  id: string;
  repoFullName: string;
  prNumber: number;
  headSha: string;
  status: ReviewStatus;
  createdAt: string; // ISO-8601
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number | null;
  tokensInput: number;
  tokensOutput: number;
  tokensTotal: number;
}
