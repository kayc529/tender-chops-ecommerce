import Keycloak, { type KeycloakConfig } from "keycloak-js";

const config: KeycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
};

export const keycloak = new Keycloak(config);

//helper to safely get token
export async function getValidToken(): Promise<string | undefined> {
  //refresh if <30s remaining
  try {
    const refreshed = await keycloak.updateToken(30);
    if (refreshed) {
      // token was refreshed
    }
    return keycloak.token;
  } catch (error) {
    keycloak.login();
    return undefined;
  }
}

//check realm roles
export function hasRealmRole(role: string) {
  return keycloak.hasRealmRole(role);
}

//check resource roles
export function hasClientRole(role: string, clientId?: string) {
  const cid = clientId ?? (import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string);
  return keycloak.hasResourceRole(role, cid);
}
