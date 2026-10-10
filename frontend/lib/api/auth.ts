import { fetchApi } from "./client";
import { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";
import { UserProfileDto } from "@/types/user";

export async function loginApi(req: LoginRequest): Promise<AuthResponse> {
  return fetchApi<AuthResponse>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export async function registerApi(req: RegisterRequest): Promise<AuthResponse> {
  return fetchApi<AuthResponse>("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export async function getMeApi(token?: string): Promise<UserProfileDto> {
  return fetchApi<UserProfileDto>("/api/v1/users/me", {
    token,
  });
}
