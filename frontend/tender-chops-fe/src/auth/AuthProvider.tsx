import { createContext, useContext, useEffect, useState } from "react";
import { keycloak } from "./keycloak";

type AuthContextType = {
  ready: boolean;
  authenticated: boolean;
  keycloak: typeof keycloak;
};

const AuthContext = createContext<AuthContextType>({
  ready: false,
  authenticated: false,
  keycloak: keycloak,
});

export const useAuth = () => useContext(AuthContext);

const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    //  Init with PKCE + silent SSO
    keycloak
      .init({
        onLoad: "check-sso",
        pkceMethod: "S256",
        checkLoginIframe: true,
        checkLoginIframeInterval: 60,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      .then((auth) => {
        setAuthenticated(auth);
        setReady(true);
      })
      .catch(() => {
        setAuthenticated(false);
        setReady(true);
      });

    // Periodic token refresh every 20s
    const id = setInterval(() => {
      if (!keycloak.authenticated) {
        return;
      }

      keycloak.updateToken(30).catch(() => {
        if (keycloak.authenticated) {
          keycloak.login();
        }
      });
    }, 20_000);

    return () => clearInterval(id);
  }, []);

  return (
    <AuthContext.Provider value={{ ready, authenticated, keycloak }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
