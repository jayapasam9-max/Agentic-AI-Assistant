import { AlertCircle, Inbox } from "lucide-react";
import { ReviewsTable } from "@/components/ReviewsTable";
import { Button } from "@/components/ui/button";
import { useReviewsQuery } from "@/lib/queries";
import { useDocumentTitle } from "@/lib/useDocumentTitle";

export default function Reviews() {
  useDocumentTitle("Reviews");
  const { data, isLoading, isError, error, refetch, isFetching } =
    useReviewsQuery();

  return (
    <section className="space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">Reviews</h1>
          <p className="text-sm text-muted-foreground">
            Recent pull-request reviews the agent has executed. Refreshes every
            15s.
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          {isFetching ? (
            <span className="inline-flex items-center gap-1">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-primary" />
              Refreshing…
            </span>
          ) : (
            <span>{data?.length ?? 0} rows</span>
          )}
        </div>
      </header>

      {isLoading && <Skeleton />}
      {isError && (
        <ErrorState
          message={error instanceof Error ? error.message : "Unknown error"}
          onRetry={() => refetch()}
        />
      )}
      {!isLoading && !isError && data && data.length === 0 && <EmptyState />}
      {!isLoading && !isError && data && data.length > 0 && (
        <ReviewsTable rows={data} />
      )}
    </section>
  );
}

function Skeleton() {
  return (
    <div className="overflow-hidden rounded-lg border border-border bg-card">
      <div className="space-y-3 p-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="h-10 animate-pulse rounded bg-muted/60"
            style={{ animationDelay: `${i * 60}ms` }}
          />
        ))}
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="grid place-items-center gap-3 rounded-lg border border-dashed border-border p-10 text-center">
      <Inbox className="h-8 w-8 text-muted-foreground" />
      <div>
        <div className="text-sm font-medium">No reviews yet</div>
        <p className="mx-auto mt-1 max-w-sm text-sm text-muted-foreground">
          Open a pull request on a repo that has the GitHub App installed, and
          it will appear here within seconds.
        </p>
      </div>
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
        <div className="text-sm font-medium">Couldn't load reviews</div>
        <p className="mt-1 text-sm text-muted-foreground">{message}</p>
      </div>
      <Button variant="outline" size="sm" onClick={onRetry}>
        Try again
      </Button>
    </div>
  );
}
