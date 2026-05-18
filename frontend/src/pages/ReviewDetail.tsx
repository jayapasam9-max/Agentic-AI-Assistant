import { useMemo } from "react";
import { Link, useParams } from "react-router-dom";
import { AlertCircle, ArrowLeft, ExternalLink } from "lucide-react";
import { StatusBadge } from "@/components/StatusBadge";
import { EventStream } from "@/components/EventStream";
import { ToolCallTimeline } from "@/components/ToolCallTimeline";
import { FindingsList } from "@/components/FindingsList";
import { Button } from "@/components/ui/button";
import { useReviewDetailQuery } from "@/lib/queries";
import { useReviewStream } from "@/lib/sse";
import {
  formatDuration,
  formatNumber,
  relativeTime,
  shortSha,
} from "@/lib/format";
import type { ReviewStatus } from "@/lib/types";

const TERMINAL_STATUSES: ReviewStatus[] = ["COMPLETED", "FAILED"];

export default function ReviewDetail() {
  const { id } = useParams<{ id: string }>();
  const { data, isLoading, isError, error } = useReviewDetailQuery(id);
  const { events, state } = useReviewStream(id);

  const terminal = useMemo(
    () => (data ? TERMINAL_STATUSES.includes(data.summary.status) : false),
    [data],
  );

  if (isLoading) return <DetailSkeleton />;
  if (isError) return <ErrorState message={errMsg(error)} />;
  if (!data) return null;

  const { summary, errorMessage, findings, toolCalls } = data;
  const prUrl = `https://github.com/${summary.repoFullName}/pull/${summary.prNumber}`;

  return (
    <section className="space-y-6">
      <Link
        to="/reviews"
        className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Back to reviews
      </Link>

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-xl font-semibold">{summary.repoFullName}</h1>
            <span className="font-mono text-muted-foreground">
              #{summary.prNumber}
            </span>
            <StatusBadge status={summary.status} />
          </div>
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <span className="font-mono">{shortSha(summary.headSha)}</span>
            <span>Started {relativeTime(summary.startedAt ?? summary.createdAt)}</span>
            <span>Duration {formatDuration(summary.durationMs)}</span>
            <span>
              Tokens {formatNumber(summary.tokensTotal)} ({formatNumber(summary.tokensInput)} in /{" "}
              {formatNumber(summary.tokensOutput)} out)
            </span>
          </div>
        </div>
        <a href={prUrl} target="_blank" rel="noreferrer">
          <Button variant="outline" size="sm">
            <ExternalLink className="h-4 w-4" />
            View PR on GitHub
          </Button>
        </a>
      </header>

      {errorMessage && (
        <div className="flex items-start gap-2 rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-sm">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
          <span className="text-foreground/80">{errorMessage}</span>
        </div>
      )}

      <EventStream events={events} state={state} terminal={terminal} />

      <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
        <section className="space-y-3">
          <h2 className="text-sm font-medium text-muted-foreground">
            Findings ({findings.length})
          </h2>
          <FindingsList findings={findings} />
        </section>
        <aside className="space-y-3">
          <h2 className="text-sm font-medium text-muted-foreground">
            Tool calls ({toolCalls.length})
          </h2>
          <ToolCallTimeline calls={toolCalls} />
        </aside>
      </div>
    </section>
  );
}

function DetailSkeleton() {
  return (
    <div className="space-y-4">
      <div className="h-6 w-2/3 animate-pulse rounded bg-muted/60" />
      <div className="h-72 animate-pulse rounded-lg bg-muted/40" />
      <div className="h-48 animate-pulse rounded-lg bg-muted/40" />
    </div>
  );
}

function ErrorState({ message }: { message: string }) {
  return (
    <div className="grid place-items-center gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-10 text-center">
      <AlertCircle className="h-8 w-8 text-destructive" />
      <div>
        <div className="text-sm font-medium">Couldn't load review</div>
        <p className="mt-1 text-sm text-muted-foreground">{message}</p>
      </div>
    </div>
  );
}

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Unknown error";
}
