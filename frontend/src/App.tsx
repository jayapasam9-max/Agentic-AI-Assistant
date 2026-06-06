import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "@/components/Layout";
import Reviews from "@/pages/Reviews";
import ReviewDetail from "@/pages/ReviewDetail";

// Metrics pulls in Recharts (~110 KB gzipped). Lazy-load it so users hitting
// /reviews don't pay for the chart code they never see.
const Metrics = lazy(() => import("@/pages/Metrics"));

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/reviews" replace />} />
        <Route path="/reviews" element={<Reviews />} />
        <Route path="/reviews/:id" element={<ReviewDetail />} />
        <Route
          path="/metrics"
          element={
            <Suspense fallback={<RouteFallback />}>
              <Metrics />
            </Suspense>
          }
        />
        <Route path="*" element={<Navigate to="/reviews" replace />} />
      </Route>
    </Routes>
  );
}

function RouteFallback() {
  return (
    <div className="space-y-3">
      <div className="h-6 w-1/3 animate-pulse rounded bg-muted/60" />
      <div className="h-20 w-full animate-pulse rounded-lg bg-muted/40" />
      <div className="h-72 w-full animate-pulse rounded-lg bg-muted/40" />
    </div>
  );
}
