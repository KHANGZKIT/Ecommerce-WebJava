import { fetchApi } from "./client";
import {
  CategoryDto,
  CreateCategoryRequest,
  CreateProductRequest,
  InventoryUpdateRequest,
  Page,
  ProductDto,
  ProductQueryParams,
  UpdateProductRequest,
} from "@/types/catalog";
import { OrderDto, UpdateOrderStatusRequest } from "@/types/order";
import { UserProfileDto } from "@/types/user";

// Admin Products
export async function createAdminProductApi(req: CreateProductRequest): Promise<ProductDto> {
  return fetchApi<ProductDto>("/api/v1/products", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export async function updateAdminProductApi(
  id: number | string,
  req: UpdateProductRequest
): Promise<ProductDto> {
  return fetchApi<ProductDto>(`/api/v1/products/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export async function updateAdminInventoryApi(
  id: number | string,
  req: InventoryUpdateRequest
): Promise<ProductDto> {
  return fetchApi<ProductDto>(`/api/v1/products/${id}/inventory`, {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}

export async function deleteAdminProductApi(id: number | string): Promise<void> {
  return fetchApi<void>(`/api/v1/products/${id}`, {
    method: "DELETE",
  });
}

// Admin Categories
export async function createAdminCategoryApi(req: CreateCategoryRequest): Promise<CategoryDto> {
  return fetchApi<CategoryDto>("/api/v1/categories", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export async function updateAdminCategoryApi(
  id: number | string,
  req: Partial<CategoryDto>
): Promise<CategoryDto> {
  return fetchApi<CategoryDto>(`/api/v1/categories/${id}`, {
    method: "PUT",
    body: JSON.stringify(req),
  });
}

export async function deleteAdminCategoryApi(id: number | string): Promise<void> {
  return fetchApi<void>(`/api/v1/categories/${id}`, {
    method: "DELETE",
  });
}

// Admin Users
export async function getAdminUsersApi(): Promise<UserProfileDto[]> {
  return fetchApi<UserProfileDto[]>("/api/v1/users");
}

export async function getAdminUserByIdApi(id: number | string): Promise<UserProfileDto> {
  return fetchApi<UserProfileDto>(`/api/v1/users/${id}`);
}

export async function deleteAdminUserApi(id: number | string): Promise<void> {
  return fetchApi<void>(`/api/v1/users/${id}`, {
    method: "DELETE",
  });
}

// Admin Orders
export async function getAdminOrderByIdApi(id: number | string): Promise<OrderDto> {
  return fetchApi<OrderDto>(`/api/v1/orders/${id}`);
}

export async function getAdminOrdersByUserApi(
  userId: number | string,
  page: number = 0,
  size: number = 10
): Promise<Page<OrderDto>> {
  return fetchApi<Page<OrderDto>>(`/api/v1/orders/user/${userId}`, {
    params: {
      page,
      size,
    },
  });
}

export async function updateAdminOrderStatusApi(
  id: number | string,
  req: UpdateOrderStatusRequest
): Promise<OrderDto> {
  return fetchApi<OrderDto>(`/api/v1/orders/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}
