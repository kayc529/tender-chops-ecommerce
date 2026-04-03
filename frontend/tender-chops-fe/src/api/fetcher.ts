import { getValidToken } from "../auth/keycloak";

export async function apiFetch<T>(
  input: RequestInfo,
  init?: RequestInit
): Promise<T> {
  const token = await getValidToken();

  const res = await fetch(`${import.meta.env.VITE_API_BASE}${input}`, {
    ...init,
    headers: {
      ...init?.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      "Content-Type": "application/json",
    },
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`API Error ${res.status}: ${text}`);
  }

  return res.json() as Promise<T>;
}
