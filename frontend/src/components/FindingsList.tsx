import { CheckCircle2 } from "lucide-react";
import { SeverityBadge } from "@/components/SeverityBadge";
import type { Finding } from "@/lib/types";

const SEVERITY_ORDER: Record<string, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
  INFO: 4,
};

export function FindingsList({ findings }: { findings: Finding[] }) {
  if (findings.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
        No findings yet.
      </div>
    );
  }

  const sorted = [...findings].sort(
    (a, b) =>
      (SEVERITY_ORDER[a.severity ?? "INFO"] ?? 99) -
      (SEVERITY_ORDER[b.severity ?? "INFO"] ?? 99),
  );

  return (
    <ul className="space-y-3">
      {sorted.map((f) => (
        <li
          key={f.id}
          className="overflow-hidden rounded-lg border border-border bg-card"
        >
          <div className="flex flex-wrap items-center gap-3 border-b border-border bg-muted/30 px-4 py-2 text-xs">
            {f.severity && <SeverityBadge severity={f.severity} />}
            {f.category && (
              <span className="font-mono text-muted-foreground">
                {f.category}
              </span>
            )}
            <span className="font-mono text-muted-foreground">
              {f.filePath}
              {f.lineNumber != null && `:${f.lineNumber}`}
            </span>
            {f.postedToGithub && (
              <span className="ml-auto flex items-center gap-1 text-emerald-300">
                <CheckCircle2 className="h-3.5 w-3.5" />
                posted
              </span>
            )}
          </div>
          <div className="space-y-2 p-4 text-sm">
            <p className="leading-relaxed">{f.message}</p>
            {f.suggestedFix && (
              <pre className="overflow-x-auto rounded-md border border-border bg-muted/30 p-3 text-xs">
                <code>{f.suggestedFix}</code>
              </pre>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
