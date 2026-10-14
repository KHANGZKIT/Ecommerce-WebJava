export class ApiError extends Error {
  status: number;
  errors?: Record<string, string> | string[];

  constructor(status: number, message: string, errors?: Record<string, string> | string[]) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errors = errors;
  }
}

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL !== undefined
    ? process.env.NEXT_PUBLIC_API_BASE_URL
    : typeof window !== "undefined"
    ? ""
    : "http://localhost:8080";

export function getStoredToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return localStorage.getItem("techgear_auth_token");
}

export function setStoredToken(token: string | null): void {
  if (typeof window === "undefined") return;
  if (token) {
    localStorage.setItem("techgear_auth_token", token);
  } else {
    localStorage.removeItem("techgear_auth_token");
  }
}

export interface RequestOptions extends RequestInit {
  token?: string | null;
  params?: Record<string, string | number | boolean | undefined>;
}

export async function fetchApi<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { token, params, headers: customHeaders, ...restOptions } = options;

  let url = `${API_BASE_URL}${endpoint.startsWith("/") ? endpoint : `/${endpoint}`}`;

  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes("?") ? "&" : "?") + queryString;
    }
  }

  const effectiveToken = token !== undefined ? token : getStoredToken();

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...(effectiveToken ? { Authorization: `Bearer ${effectiveToken}` } : {}),
    ...((customHeaders as Record<string, string>) || {}),
  };

  const response = await fetch(url, {
    ...restOptions,
    headers,
  });

  if (response.status === 204) {
    return {} as T;
  }

  let data: any = null;
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    // FE-15: HTTP 401 Session Clearance & Interception
    // When backend rejects session on an authenticated endpoint, clear credentials and navigate to /login.
    // Excludes /api/v1/auth/login where 401 represents invalid credentials on login attempt.
    if (response.status === 401 && !endpoint.includes("/api/v1/auth/login")) {
      setStoredToken(null);
      if (typeof window !== "undefined") {
        try {
          localStorage.removeItem("techgear_user_profile");
          window.dispatchEvent(new CustomEvent("techgear:auth:unauthorized"));
          const path = window.location.pathname;
          if (path !== "/login" && path !== "/register") {
            window.location.href = "/login";
          }
        } catch {
          // ignore storage or navigation errors in non-browser context
        }
      }
    }

    const errorMessage =
      data?.message ||
      (typeof data === "string" && data) ||
      `HTTP error ${response.status}: ${response.statusText}`;

    throw new ApiError(response.status, errorMessage, data?.errors);
  }

  return data as T;
}
