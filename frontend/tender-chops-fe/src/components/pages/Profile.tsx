import { useAuth } from "@/auth/AuthProvider";
import ProtectedRoute from "@/auth/ProtectedRoute";

const Profile = () => {
  return (
    <ProtectedRoute>
      <ProfileInner />
    </ProtectedRoute>
  );
};

const ProfileInner = () => {
  const { keycloak } = useAuth();

  return (
    <div className="p-6">
      <h1>Dashboard</h1>
      <p>User: {keycloak.tokenParsed?.preferred_username}</p>
      <a href="/">Home</a>
    </div>
  );
};

export default Profile;
