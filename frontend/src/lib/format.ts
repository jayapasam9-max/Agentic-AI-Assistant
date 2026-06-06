// Tiny formatters — no date-fns/dayjs dependency so the JS bundle stays small.

const UNITS: ReadonlyArray<readonly [number, string]> = [
  [60, "s"],
  [60, "m"],
  [24, "h"],
  [7, "d"],
  [Infinity, "w"],
];

/** Compact human-readable "x ago" — e.g. "12s ago", "3m ago", "2h ago". */
export function relativeTime(iso: string | null | undefined, now = Date.now()): string {
  if (!iso) return "—";
  const ms = now - new Date(iso).getTime();
  if (ms < 0) return "just now";
  let value = ms / 1000;
  let label = "s";
  for (const [step, nextLabel] of UNITS) {
    if (value < step) break;
    value = value / step;
    label = nextLabel;
  }
  return `${Math.floor(value)}${label} ago`;
}

/** Compact duration — "0.4s", "12s", "3m 04s". */
export function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return "—";
  if (ms < 1000) return `${ms}ms`;
  const totalSeconds = Math.floor(ms / 1000);
  if (totalSeconds < 60) return `${totalSeconds}s`;
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${m}m ${String(s).padStart(2, "0")}s`;
}

/** 13023 → "13.0k", 1_234_567 → "1.2M". */
export function formatNumber(n: number | null | undefined): string {
  if (n == null) return "—";
  if (n < 1000) return String(n);
  if (n < 1_000_000) return `${(n / 1000).toFixed(1)}k`;
  return `${(n / 1_000_000).toFixed(1)}M`;
}

/** Trim "a1b2c3d4..." to "a1b2c3d". */
export function shortSha(sha: string | null | undefined): string {
  if (!sha) return "—";
  return sha.slice(0, 7);
}

/** "$0.12", "<$0.01", "$12.34", "$1.2k". Compact but informative. */
export function formatUsd(n: number | null | undefined): string {
  if (n == null) return "—";
  if (n === 0) return "$0.00";
  if (n < 0.01) return "<$0.01";
  if (n < 100) return `$${n.toFixed(2)}`;
  if (n < 1000) return `$${n.toFixed(0)}`;
  if (n < 1_000_000) return `$${(n / 1000).toFixed(1)}k`;
  return `$${(n / 1_000_000).toFixed(1)}M`;
}

/** "May 17" — short month + day, for chart axes. */
export function shortDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(`${iso}T00:00:00Z`);
  return d.toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  });
}
