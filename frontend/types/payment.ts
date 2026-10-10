export type PaymentMethod =
  | "CREDIT_CARD"
  | "DEBIT_CARD"
  | "UPI"
  | "NET_BANKING"
  | "WALLET";

export type PaymentStatus =
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "REFUNDED";

export interface PaymentResponse {
  id: number;
  transactionId: string;
  orderId: number;
  userId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  message?: string | null;
  createdAt: string;
}

export interface RefundRequest {
  paymentId: number;
  reason: string;
}
