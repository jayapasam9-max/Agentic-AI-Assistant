import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { DailyMetric } from "@/lib/types";
import { formatUsd, shortDate } from "@/lib/format";

interface MetricsChartProps {
  data: DailyMetric[];
}

/**
 * Stacked bars for daily completed/failed counts on the left axis, and a
 * cost line ($) on the right axis. One picture covers volume, reliability,
 * and spend at the same time — which is the whole point of this page.
 */
export function MetricsChart({ data }: MetricsChartProps) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="mb-2 text-sm font-medium">Daily reviews & cost</div>
      <div className="h-72 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={data} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
            <CartesianGrid stroke="hsl(var(--border))" strokeDasharray="2 4" vertical={false} />
            <XAxis
              dataKey="day"
              tickFormatter={shortDate}
              tick={{ fill: "hsl(var(--muted-foreground))", fontSize: 11 }}
              stroke="hsl(var(--border))"
            />
            <YAxis
              yAxisId="count"
              allowDecimals={false}
              tick={{ fill: "hsl(var(--muted-foreground))", fontSize: 11 }}
              stroke="hsl(var(--border))"
            />
            <YAxis
              yAxisId="cost"
              orientation="right"
              tickFormatter={(n: number) => formatUsd(n)}
              tick={{ fill: "hsl(var(--muted-foreground))", fontSize: 11 }}
              stroke="hsl(var(--border))"
            />
            <Tooltip
              contentStyle={{
                background: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: 8,
                fontSize: 12,
              }}
              labelFormatter={(label: string) => shortDate(label)}
              formatter={(value: number, name: string) =>
                name === "Cost (USD)" ? [formatUsd(value), name] : [value, name]
              }
            />
            <Legend wrapperStyle={{ fontSize: 12 }} />
            <Bar
              yAxisId="count"
              dataKey="completed"
              stackId="status"
              fill="hsl(160 60% 45%)"
              name="Completed"
              radius={[2, 2, 0, 0]}
            />
            <Bar
              yAxisId="count"
              dataKey="failed"
              stackId="status"
              fill="hsl(0 70% 55%)"
              name="Failed"
              radius={[2, 2, 0, 0]}
            />
            <Line
              yAxisId="cost"
              type="monotone"
              dataKey="costUsd"
              stroke="hsl(199 89% 56%)"
              strokeWidth={2}
              dot={false}
              name="Cost (USD)"
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
