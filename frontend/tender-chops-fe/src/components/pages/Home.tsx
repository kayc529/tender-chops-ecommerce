import { useAuth } from "@/auth/AuthProvider";

const Home = () => {
  const { authenticated, keycloak } = useAuth();

  return (
    <div className="w-full flex flex-col">
      <h1>Home</h1>
      {!authenticated ? (
        <button
          className="w-max py-2 px-1 border"
          onClick={() => keycloak.login()}
        >
          Login
        </button>
      ) : (
        <div>
          <p>Hello, {keycloak.tokenParsed?.preferred_username}</p>
          <button
            onClick={() =>
              keycloak.logout({ redirectUri: window.location.origin })
            }
          >
            Logout
          </button>
          <a href="/profile">Profile</a>
        </div>
      )}
    </div>
  );
};

export default Home;
