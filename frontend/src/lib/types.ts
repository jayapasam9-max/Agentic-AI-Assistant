// Mirrors backend com.codereview.agent.api.dto.* records.
// Keep these in sync by hand; both files reference the same JSON shape.

export type ReviewStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export type Severity = "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type Category =
  | "SECURITY"
  | "PERFORMANCE"
  | "STYLE"
  | "BUG"
  | "MAINTAINABILITY";

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

export interface Finding {
  id: string;
  filePath: string;
  lineNumber: number | null;
  severity: Severity | null;
  category: Category | null;
  message: string;
  suggestedFix: string | null;
  postedToGithub: boolean;
  createdAt: string;
}

export interface ToolCall {
  id: string;
  iteration: number;
  toolName: string;
  durationMs: number | null;
  success: boolean;
  createdAt: string;
}

export interface ReviewDetail {
  summary: ReviewSummary;
  errorMessage: string | null;
  findings: Finding[];
  toolCalls: ToolCall[];
}

export interface DailyMetric {
  day: string; // YYYY-MM-DD (Postgres date)
  reviews: number;
  completed: number;
  failed: number;
  tokensInput: number;
  tokensOutput: number;
  tokensTotal: number;
  costUsd: number;
}

/** Server-Sent Event types emitted by the agent during a review. */
export type StreamEventType =
  | "JOB_STARTED"
  | "REASONING_CHUNK"
  | "TOOL_CALL_STARTED"
  | "TOOL_CALL_COMPLETED"
  | "FINDING_EMITTED"
  | "JOB_COMPLETED"
  | "JOB_FAILED";

export interface StreamEvent {
  /** Local timestamp the event was received in the browser. */
  receivedAt: string;
  type: StreamEventType;
  /** Raw payload string. For FINDING_EMITTED this is itself JSON. */
  payload: string;
}
