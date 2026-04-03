import { useAuth } from "@/auth/AuthProvider";
import { useEffect } from "react";
import { Navigate } from "react-router";

const Login = () => {
  const { ready, authenticated, keycloak } = useAuth();

  useEffect(() => {
    if (ready && !authenticated) {
      keycloak.login({ redirectUri: "/profile" });
    }
  }, [ready, authenticated, keycloak]);

  if (!ready) {
    return <div className="p-6">Redirecting to login...</div>;
  }

  if (authenticated) {
    return <Navigate to="/profile" replace />;
  }

  return null;
};

export default Login;
