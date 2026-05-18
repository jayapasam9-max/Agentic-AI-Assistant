import { useNavigate } from "react-router-dom";
import { StatusBadge } from "@/components/StatusBadge";
import type { ReviewSummary } from "@/lib/types";
import {
  formatDuration,
  formatNumber,
  relativeTime,
  shortSha,
} from "@/lib/format";

interface ReviewsTableProps {
  rows: ReviewSummary[];
}

export function ReviewsTable({ rows }: ReviewsTableProps) {
  const navigate = useNavigate();

  return (
    <div className="overflow-hidden rounded-lg border border-border bg-card">
      {/* Desktop / tablet: real table. Hidden on small screens in favor of cards. */}
      <table className="hidden w-full text-sm md:table">
        <thead className="bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
          <tr>
            <Th>Repository</Th>
            <Th>PR</Th>
            <Th>Status</Th>
            <Th align="right">Started</Th>
            <Th align="right">Duration</Th>
            <Th align="right">Tokens</Th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {rows.map((r) => (
            <tr
              key={r.id}
              tabIndex={0}
              onClick={() => navigate(`/reviews/${r.id}`)}
              onKeyDown={(e) => {
                if (e.key === "Enter") navigate(`/reviews/${r.id}`);
              }}
              className="cursor-pointer transition-colors hover:bg-accent/40 focus:bg-accent/40 focus:outline-none"
            >
              <Td>
                <div className="font-medium">{r.repoFullName}</div>
                <div className="font-mono text-xs text-muted-foreground">
                  {shortSha(r.headSha)}
                </div>
              </Td>
              <Td className="font-mono">#{r.prNumber}</Td>
              <Td>
                <StatusBadge status={r.status} />
              </Td>
              <Td align="right" className="text-muted-foreground">
                {relativeTime(r.startedAt ?? r.createdAt)}
              </Td>
              <Td align="right" className="font-mono">
                {formatDuration(r.durationMs)}
              </Td>
              <Td align="right" className="font-mono">
                {formatNumber(r.tokensTotal)}
              </Td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile: stacked cards. */}
      <ul className="divide-y divide-border md:hidden">
        {rows.map((r) => (
          <li
            key={r.id}
            onClick={() => navigate(`/reviews/${r.id}`)}
            className="cursor-pointer p-4 transition-colors hover:bg-accent/40"
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <div className="truncate text-sm font-medium">
                  {r.repoFullName}
                </div>
                <div className="font-mono text-xs text-muted-foreground">
                  #{r.prNumber} · {shortSha(r.headSha)}
                </div>
              </div>
              <StatusBadge status={r.status} />
            </div>
            <dl className="mt-3 grid grid-cols-3 gap-2 text-xs">
              <Cell label="Started" value={relativeTime(r.startedAt ?? r.createdAt)} />
              <Cell label="Duration" value={formatDuration(r.durationMs)} />
              <Cell label="Tokens" value={formatNumber(r.tokensTotal)} />
            </dl>
          </li>
        ))}
      </ul>
    </div>
  );
}

function Th({
  children,
  align = "left",
}: {
  children: React.ReactNode;
  align?: "left" | "right";
}) {
  return (
    <th
      className={`px-4 py-2 ${align === "right" ? "text-right" : "text-left"}`}
    >
      {children}
    </th>
  );
}

function Td({
  children,
  align = "left",
  className,
}: {
  children: React.ReactNode;
  align?: "left" | "right";
  className?: string;
}) {
  return (
    <td
      className={`px-4 py-3 align-middle ${
        align === "right" ? "text-right" : "text-left"
      } ${className ?? ""}`}
    >
      {children}
    </td>
  );
}

function Cell({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-mono">{value}</dd>
    </div>
  );
}
