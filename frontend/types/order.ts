import { PaymentMethod } from "./payment";
export type { Page } from "./catalog";

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PAYMENT_FAILED"
  | "PAYMENT_UNKNOWN"
  | "PROCESSING"
  | "SHIPPED"
  | "DELIVERED"
  | "CANCELLED"
  | "REFUNDED";

export interface OrderItemDto {
  id?: number;
  productId: number;
  productName: string;
  productSku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderDto {
  id: number;
  userId: number;
  items: OrderItemDto[];
  totalAmount: number;
  status: OrderStatus;
  shippingAddress: string;
  paymentTransactionId?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CheckoutRequest {
  shippingAddress: string;
  paymentMethod: PaymentMethod;
  notes?: string;
}

export interface CheckoutResponse {
  orderId: number;
  orderNumber?: string;
  status: OrderStatus;
  paymentStatus?: string;
  totalAmount: number;
  paymentTransactionId?: string;
  message?: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
}
