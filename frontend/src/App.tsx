import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "@/components/Layout";
import Reviews from "@/pages/Reviews";
import ReviewDetail from "@/pages/ReviewDetail";
import Metrics from "@/pages/Metrics";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/reviews" replace />} />
        <Route path="/reviews" element={<Reviews />} />
        <Route path="/reviews/:id" element={<ReviewDetail />} />
        <Route path="/metrics" element={<Metrics />} />
        <Route path="*" element={<Navigate to="/reviews" replace />} />
      </Route>
    </Routes>
  );
}
