import { fetchApi } from "./client";
import { CheckoutRequest, CheckoutResponse } from "@/types/order";

export async function checkoutApi(
  req: CheckoutRequest,
  idempotencyKey?: string
): Promise<CheckoutResponse> {
  const headers: Record<string, string> = {};
  if (idempotencyKey) {
    headers["Idempotency-Key"] = idempotencyKey;
  }

  return fetchApi<CheckoutResponse>("/api/v1/orders/checkout", {
    method: "POST",
    headers,
    body: JSON.stringify(req),
  });
}
