import { fetchApi } from "./client";
import { OrderDto } from "@/types/order";
import { Page } from "@/types/catalog";

export async function getOrdersByUserApi(
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

export async function getOrderByIdApi(id: number | string): Promise<OrderDto> {
  return fetchApi<OrderDto>(`/api/v1/orders/${id}`);
}

export async function cancelOrderApi(id: number | string): Promise<OrderDto> {
  return fetchApi<OrderDto>(`/api/v1/orders/${id}/cancel`, {
    method: "POST",
  });
}

