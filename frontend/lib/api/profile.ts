import { fetchApi } from "./client";
import { UpdateUserProfileRequest, UserProfileDto } from "@/types/user";

export async function getProfileApi(): Promise<UserProfileDto> {
  return fetchApi<UserProfileDto>("/api/v1/users/me");
}

export async function updateProfileApi(req: UpdateUserProfileRequest): Promise<UserProfileDto> {
  return fetchApi<UserProfileDto>("/api/v1/users/me", {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}
