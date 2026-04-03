import { QueryClientProvider } from "@tanstack/react-query";
import AuthProvider from "./auth/AuthProvider";
import { queryClient } from "./api/queryClient";
import Root from "./Root";

const AppEntryPoint = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Root />
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default AppEntryPoint;
