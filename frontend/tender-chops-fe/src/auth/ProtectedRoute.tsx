import type { JSX } from "react";
import { useAuth } from "./AuthProvider";
import { Navigate } from "react-router";

export default function ProtectedRoute({
  children,
}: {
  children: JSX.Element;
}) {
  const { ready, authenticated } = useAuth();

  if (!ready) {
    return <div>Loading Auth...</div>;
  }

  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
