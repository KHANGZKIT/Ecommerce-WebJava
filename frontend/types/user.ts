export type UserRole = "CUSTOMER" | "ADMIN";

export interface UserProfileDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
  updatedAt?: string | null;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string | null;
}
