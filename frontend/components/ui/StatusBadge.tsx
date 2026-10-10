import React from "react";
import { OrderStatus } from "@/types/order";
import { PaymentStatus } from "@/types/payment";

export type AnyStatus = OrderStatus | PaymentStatus | "IN_STOCK" | "OUT_OF_STOCK" | "ACTIVE" | "INACTIVE";

const STATUS_VI_LABELS: Record<string, string> = {
  CONFIRMED: "Đã xác nhận",
  DELIVERED: "Đã giao hàng",
  SUCCESS: "Thành công",
  IN_STOCK: "Còn hàng",
  ACTIVE: "Đang bán",
  PENDING: "Chờ xử lý",
  PROCESSING: "Đang xử lý",
  SHIPPED: "Đang giao",
  PAYMENT_FAILED: "Thanh toán thất bại",
  CANCELLED: "Đã hủy",
  FAILED: "Thất bại",
  OUT_OF_STOCK: "Hết hàng",
  INACTIVE: "Tạm ngừng",
  REFUNDED: "Đã hoàn tiền",
};

export function StatusBadge({ status }: { status: AnyStatus }) {
  const getBadgeStyle = () => {
    switch (status) {
      case "CONFIRMED":
      case "DELIVERED":
      case "SUCCESS":
      case "IN_STOCK":
      case "ACTIVE":
        return "text-emerald-700 bg-emerald-50 border-emerald-200";

      case "PENDING":
      case "PROCESSING":
      case "SHIPPED":
        return "text-sky-700 bg-sky-50 border-sky-200";

      case "PAYMENT_FAILED":
      case "CANCELLED":
      case "FAILED":
      case "OUT_OF_STOCK":
      case "INACTIVE":
        return "text-rose-700 bg-rose-50 border-rose-200";

      case "REFUNDED":
        return "text-purple-700 bg-purple-50 border-purple-200";

      default:
        return "text-stone-700 bg-stone-100 border-stone-200";
    }
  };

  const label = STATUS_VI_LABELS[status] || status;

  return (
    <span
      className={`inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-0.5 rounded-full border ${getBadgeStyle()}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-70" />
      <span>{label}</span>
    </span>
  );
}
