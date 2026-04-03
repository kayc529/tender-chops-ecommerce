import { createBrowserRouter, RouterProvider } from "react-router";
import Home from "./components/pages/Home";
import Login from "./components/pages/Login";
import Profile from "./components/pages/Profile";
import Cart from "./components/pages/Cart";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/auth/AuthProvider";
import { useEffect } from "react";
import { apiFetch } from "@/api/fetcher";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Home />,
  },
  {
    path: "/login",
    element: <Login />,
  },
  {
    path: "/profile",
    element: <Profile />,
  },
  {
    path: "/cart",
    element: <Cart />,
  },
]);

const Root = () => {
  const queryClient = useQueryClient();
  const { authenticated, keycloak } = useAuth();

  const sessionId = keycloak?.tokenParsed?.sid as string | undefined;

  console.log("sessionId: ", sessionId);

  useEffect(() => {
    if (!authenticated || !sessionId) {
      return;
    }

    const LAST_KEY = "handshake:lastSessionState";
    const last = localStorage.getItem(LAST_KEY);

    //if still the same session then no need to call the post-login api again
    if (last === sessionId) {
      return;
    }

    //save sessionState in local storage if new session
    localStorage.setItem(LAST_KEY, sessionId);

    const run = async () => {
      try {
        queryClient.fetchQuery({
          queryKey: ["handshake", sessionId],
          queryFn: () => apiFetch("/users/me"),
          retry: 3,
          retryDelay: 2000,
          staleTime: 0,
        });
      } catch (error) {
        console.error("Handshake failed: " + error);
      }
    };

    /* TODO uncomment when handshake API is ready */
    // run();
  }, [authenticated, sessionId, queryClient]);

  return <RouterProvider router={router} />;
};

export default Root;
