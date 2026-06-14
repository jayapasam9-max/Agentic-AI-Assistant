import { useMemo, useState } from "react";
import {
  AlertCircle,
  Coins,
  DollarSign,
  ListChecks,
  PercentCircle,
} from "lucide-react";
import { StatCard } from "@/components/StatCard";
import { MetricsChart } from "@/components/MetricsChart";
import { Button } from "@/components/ui/button";
import { useDailyMetricsQuery } from "@/lib/queries";
import { useDocumentTitle } from "@/lib/useDocumentTitle";
import type { DailyMetric } from "@/lib/types";
import { formatNumber, formatUsd } from "@/lib/format";
import { cn } from "@/lib/utils";

const RANGES = [7, 14, 30] as const;
type Range = (typeof RANGES)[number];

export default function Metrics() {
  useDocumentTitle("Metrics");
  const [days, setDays] = useState<Range>(14);
  const { data, isLoading, isError, error, refetch } = useDailyMetricsQuery(days);

  const dense = useMemo(() => fillMissingDays(data ?? [], days), [data, days]);
  const totals = useMemo(() => sumTotals(dense), [dense]);

  return (
    <section className="space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">Metrics</h1>
          <p className="text-sm text-muted-foreground">
            Daily review volume, token usage, and estimated cost.
          </p>
        </div>
        <RangeToggle value={days} onChange={setDays} />
      </header>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Total reviews"
          value={isLoading ? "—" : String(totals.reviews)}
          hint={`Last ${days} days`}
          icon={ListChecks}
        />
        <StatCard
          label="Success rate"
          value={isLoading ? "—" : formatPercent(totals.successRate)}
          hint={
            totals.completed + totals.failed === 0
              ? "No completed reviews yet"
              : `${totals.completed} ok · ${totals.failed} failed`
          }
          icon={PercentCircle}
        />
        <StatCard
          label="Tokens"
          value={isLoading ? "—" : formatNumber(totals.tokensTotal)}
          hint={`${formatNumber(totals.tokensInput)} in / ${formatNumber(totals.tokensOutput)} out`}
          icon={Coins}
        />
        <StatCard
          label="Est. cost"
          value={isLoading ? "—" : formatUsd(totals.costUsd)}
          hint="Approximate Opus-class pricing"
          icon={DollarSign}
        />
      </div>

      {isError ? (
        <ErrorState
          message={error instanceof Error ? error.message : "Unknown error"}
          onRetry={() => refetch()}
        />
      ) : (
        <MetricsChart data={dense} />
      )}
    </section>
  );
}

function RangeToggle({
  value,
  onChange,
}: {
  value: Range;
  onChange: (r: Range) => void;
}) {
  return (
    <div className="inline-flex rounded-md border border-border p-0.5 text-xs">
      {RANGES.map((r) => (
        <button
          key={r}
          onClick={() => onChange(r)}
          className={cn(
            "rounded-sm px-2.5 py-1 transition-colors",
            value === r
              ? "bg-accent text-accent-foreground"
              : "text-muted-foreground hover:text-foreground",
          )}
        >
          {r}d
        </button>
      ))}
    </div>
  );
}

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="grid place-items-center gap-3 rounded-lg border border-destructive/40 bg-destructive/5 p-10 text-center">
      <AlertCircle className="h-8 w-8 text-destructive" />
      <div>
        <div className="text-sm font-medium">Couldn't load metrics</div>
        <p className="mt-1 text-sm text-muted-foreground">{message}</p>
      </div>
      <Button variant="outline" size="sm" onClick={onRetry}>
        Try again
      </Button>
    </div>
  );
}

/**
 * Backend omits days with zero activity (Postgres GROUP BY). Fill them in
 * with zero rows so the chart's x-axis stays continuous across the range.
 */
function fillMissingDays(rows: DailyMetric[], days: number): DailyMetric[] {
  const byDay = new Map(rows.map((r) => [r.day, r]));
  const out: DailyMetric[] = [];
  const today = new Date();
  today.setUTCHours(0, 0, 0, 0);
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setUTCDate(today.getUTCDate() - i);
    const key = d.toISOString().slice(0, 10);
    out.push(
      byDay.get(key) ?? {
        day: key,
        reviews: 0,
        completed: 0,
        failed: 0,
        tokensInput: 0,
        tokensOutput: 0,
        tokensTotal: 0,
        costUsd: 0,
      },
    );
  }
  return out;
}

function sumTotals(rows: DailyMetric[]) {
  const acc = rows.reduce(
    (a, r) => ({
      reviews: a.reviews + r.reviews,
      completed: a.completed + r.completed,
      failed: a.failed + r.failed,
      tokensInput: a.tokensInput + r.tokensInput,
      tokensOutput: a.tokensOutput + r.tokensOutput,
      tokensTotal: a.tokensTotal + r.tokensTotal,
      costUsd: a.costUsd + r.costUsd,
    }),
    {
      reviews: 0,
      completed: 0,
      failed: 0,
      tokensInput: 0,
      tokensOutput: 0,
      tokensTotal: 0,
      costUsd: 0,
    },
  );
  const denom = acc.completed + acc.failed;
  const successRate = denom === 0 ? null : acc.completed / denom;
  return { ...acc, successRate };
}

function formatPercent(rate: number | null): string {
  if (rate == null) return "—";
  return `${Math.round(rate * 100)}%`;
}
