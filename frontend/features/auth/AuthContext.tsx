"use client";

import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";
import { UserProfileDto } from "@/types/user";
import { getStoredToken, setStoredToken } from "@/lib/api/client";
import { getMeApi, loginApi, registerApi } from "@/lib/api/auth";

interface AuthContextType {
  user: UserProfileDto | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (req: LoginRequest) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const USER_STORAGE_KEY = "techgear_user_profile";

/**
 * Validates JWT token expiration timestamp on client side (FE-15 Section 8.2).
 * Returns true if token is malformed, missing exp claim, or timestamp is past current time.
 */
function isJwtExpired(token: string): boolean {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return true;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    const payload = JSON.parse(jsonPayload);
    if (typeof payload.exp !== "number") return false;
    return payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<UserProfileDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Synchronize state upon global 401 session expiration event (FE-15 Section 8.3)
  useEffect(() => {
    if (typeof window === "undefined") return;
    const handleUnauthorized = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener("techgear:auth:unauthorized", handleUnauthorized);
    return () => {
      window.removeEventListener("techgear:auth:unauthorized", handleUnauthorized);
    };
  }, []);

  // Initialize from storage on mount
  useEffect(() => {
    try {
      const storedToken = getStoredToken();
      const storedUserJson = localStorage.getItem(USER_STORAGE_KEY);
      
      if (storedToken) {
        // Fast-path client-side expiration check (FE-15 Section 8.2)
        if (isJwtExpired(storedToken)) {
          setStoredToken(null);
          localStorage.removeItem(USER_STORAGE_KEY);
          setToken(null);
          setUser(null);
          setIsLoading(false);
          return;
        }

        setToken(storedToken);
        if (storedUserJson) {
          try {
            setUser(JSON.parse(storedUserJson));
          } catch {
            // invalid JSON, ignore
          }
        }
        // Verify token with backend
        getMeApi(storedToken)
          .then((freshUser) => {
            setUser(freshUser);
            localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(freshUser));
          })
          .catch(() => {
            // If token is rejected or invalid, clear state
            setStoredToken(null);
            localStorage.removeItem(USER_STORAGE_KEY);
            setToken(null);
            setUser(null);
          })
          .finally(() => {
            setIsLoading(false);
          });
      } else {
        setIsLoading(false);
      }
    } catch {
      setIsLoading(false);
    }
  }, []);

  const handleAuthSuccess = useCallback((authData: AuthResponse) => {
    setStoredToken(authData.accessToken);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(authData.user));
    setToken(authData.accessToken);
    setUser(authData.user);
  }, []);

  const login = useCallback(async (req: LoginRequest) => {
    const res = await loginApi(req);
    handleAuthSuccess(res);
  }, [handleAuthSuccess]);

  const register = useCallback(async (req: RegisterRequest) => {
    const res = await registerApi(req);
    handleAuthSuccess(res);
  }, [handleAuthSuccess]);

  const logout = useCallback(() => {
    setStoredToken(null);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
    router.replace("/login");
  }, [router]);

  const refreshUser = useCallback(async () => {
    if (!token) return;
    try {
      const freshUser = await getMeApi(token);
      setUser(freshUser);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(freshUser));
    } catch {
      // ignore
    }
  }, [token]);

  const isAuthenticated = !!token && !!user;
  const isAdmin = user?.role === "ADMIN";

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isAdmin,
        isLoading,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
