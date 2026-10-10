import { fetchApi } from "./client";
import { PaymentResponse, RefundRequest } from "@/types/payment";

export async function getPaymentByIdApi(id: number | string): Promise<PaymentResponse> {
  return fetchApi<PaymentResponse>(`/api/v1/payments/${id}`);
}

export async function getPaymentsByOrderApi(orderId: number | string): Promise<PaymentResponse[]> {
  return fetchApi<PaymentResponse[]>(`/api/v1/payments/order/${orderId}`);
}

export async function getPaymentsByUserApi(userId: number | string): Promise<PaymentResponse[]> {
  return fetchApi<PaymentResponse[]>(`/api/v1/payments/user/${userId}`);
}

export async function refundPaymentApi(req: RefundRequest): Promise<PaymentResponse> {
  return fetchApi<PaymentResponse>("/api/v1/payments/refund", {
    method: "POST",
    body: JSON.stringify(req),
  });
}
