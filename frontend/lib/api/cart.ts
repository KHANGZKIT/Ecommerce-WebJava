import { fetchApi } from "./client";
import { AddToCartRequest, CartDto, UpdateQuantityRequest } from "@/types/cart";

export async function getCartApi(): Promise<CartDto> {
  return fetchApi<CartDto>("/api/v1/cart");
}

export async function addToCartApi(req: AddToCartRequest): Promise<CartDto> {
  return fetchApi<CartDto>("/api/v1/cart/items", {
    method: "POST",
    body: JSON.stringify(req),
  });
}

export async function updateCartItemQuantityApi(
  itemId: number,
  req: UpdateQuantityRequest
): Promise<CartDto> {
  return fetchApi<CartDto>(`/api/v1/cart/items/${itemId}`, {
    method: "PATCH",
    body: JSON.stringify(req),
  });
}

export async function removeCartItemApi(itemId: number): Promise<CartDto> {
  return fetchApi<CartDto>(`/api/v1/cart/items/${itemId}`, {
    method: "DELETE",
  });
}

export async function clearCartApi(): Promise<void> {
  return fetchApi<void>("/api/v1/cart", {
    method: "DELETE",
  });
}
