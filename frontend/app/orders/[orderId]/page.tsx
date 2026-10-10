"use client";

import React, { use, useEffect, useState, useCallback, Suspense } from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProtectedRoute } from "@/features/auth/AuthGuard";
import { formatVndPrice } from "@/components/product/ProductCard";
import { getOrderByIdApi, cancelOrderApi } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import { OrderDto, OrderStatus } from "@/types/order";

const STATUS_CONFIG: Record<
  OrderStatus,
  { label: string; fg: string; bg: string; border: string }
> = {
  CONFIRMED: {
    label: "Đã xác nhận",
    fg: "#15803D",
    bg: "#F0FDF4",
    border: "#BBF7D0",
  },
  PROCESSING: {
    label: "Đang xử lý",
    fg: "#0369A1",
    bg: "#F0F9FF",
    border: "#BAE6FD",
  },
  SHIPPED: {
    label: "Đang giao",
    fg: "#0284C7",
    bg: "#F0F9FF",
    border: "#BAE6FD",
  },
  DELIVERED: {
    label: "Đã giao hàng",
    fg: "#1C1917",
    bg: "#F5F5F4",
    border: "#E7E5E4",
  },
  PENDING: {
    label: "Chờ xử lý",
    fg: "#B45309",
    bg: "#FFFBEB",
    border: "#FDE68A",
  },
  PAYMENT_FAILED: {
    label: "Thanh toán lỗi",
    fg: "#DC2626",
    bg: "#FEF2F2",
    border: "#FECACA",
  },
  PAYMENT_UNKNOWN: {
    label: "Chưa rõ",
    fg: "#B45309",
    bg: "#FFFBEB",
    border: "#FDE68A",
  },
  CANCELLED: {
    label: "Đã hủy",
    fg: "#DC2626",
    bg: "#FEF2F2",
    border: "#FECACA",
  },
  REFUNDED: {
    label: "Đã hoàn tiền",
    fg: "#78716C",
    bg: "#F5F5F4",
    border: "#E7E5E4",
  },
};

function OrderStatusBadge({ status }: { status: OrderStatus }) {
  const config = STATUS_CONFIG[status] || {
    label: status,
    fg: "#78716C",
    bg: "#F5F5F4",
    border: "#E7E5E4",
  };

  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full border text-xs font-semibold"
      style={{
        color: config.fg,
        backgroundColor: config.bg,
        borderColor: config.border,
      }}
    >
      {config.label}
    </span>
  );
}

function formatOrderDate(dateString: string): string {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return new Intl.DateTimeFormat("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(date);
  } catch {
    return dateString;
  }
}

function OrderDetailSkeleton() {
  return (
    <div className="max-w-[1100px] mx-auto py-4 md:py-10 px-4 md:px-8 animate-pulse">
      <div className="h-4 w-32 bg-stone-200 rounded mb-4" />
      <div className="pb-6 border-b border-stone-200 mb-8 flex justify-between items-center">
        <div className="space-y-2">
          <div className="h-8 w-56 bg-stone-200 rounded" />
          <div className="h-4 w-40 bg-stone-100 rounded" />
        </div>
        <div className="h-7 w-24 bg-stone-200 rounded" />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div className="lg:col-span-8 bg-white border border-stone-200 rounded-xl p-6 h-64" />
        <div className="lg:col-span-4 space-y-6">
          <div className="bg-white border border-stone-200 rounded-xl p-6 h-28" />
          <div className="bg-white border border-stone-200 rounded-xl p-6 h-36" />
          <div className="bg-white border border-stone-200 rounded-xl p-6 h-24" />
        </div>
      </div>
    </div>
  );
}

function OrderDetailContent({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  const { orderId } = use(params);

  const [order, setOrder] = useState<OrderDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isAccessDenied, setIsAccessDenied] = useState<boolean>(false);
  const [isNotFound, setIsNotFound] = useState<boolean>(false);

  // Cancellation State
  const [showCancelModal, setShowCancelModal] = useState<boolean>(false);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  const numericOrderId = Number(orderId);
  const isValidOrderId = !isNaN(numericOrderId) && numericOrderId > 0;

  const fetchOrder = useCallback(async () => {
    if (!isValidOrderId) {
      setIsNotFound(true);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    setIsAccessDenied(false);
    setIsNotFound(false);

    try {
      const data = await getOrderByIdApi(numericOrderId);
      setOrder(data);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        if (err.status === 404) {
          setIsNotFound(true);
        } else if (err.status === 403) {
          setIsAccessDenied(true);
        } else {
          setError(err.message || "Không thể tải thông tin đơn hàng.");
        }
      } else {
        setError(err instanceof Error ? err.message : "Không thể tải thông tin đơn hàng.");
      }
    } finally {
      setIsLoading(false);
    }
  }, [isValidOrderId, numericOrderId]);

  useEffect(() => {
    fetchOrder();
  }, [fetchOrder]);

  const handleConfirmCancel = async () => {
    if (!order || isCancelling) return;
    setIsCancelling(true);
    setCancelError(null);

    try {
      const updated = await cancelOrderApi(order.id);
      setOrder(updated);
      setShowCancelModal(false);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Không thể hủy đơn hàng. Vui lòng thử lại sau.";
      setCancelError(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  // 1. Loading State
  if (isLoading) {
    return <OrderDetailSkeleton />;
  }

  // 2. 404 Not Found State
  if (isNotFound) {
    return (
      <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-stone-200 rounded-xl shadow-sm my-12">
        <div
          className="w-16 h-16 mx-auto mb-4 rounded-full bg-stone-100 flex items-center justify-center text-2xl text-stone-400 font-bold"
          aria-hidden="true"
        >
          ?
        </div>
        <h2 className="text-xl font-bold text-stone-900 mb-2">
          Không tìm thấy đơn hàng
        </h2>
        <p className="text-sm text-stone-500 mb-6 leading-relaxed">
          Đơn hàng bạn yêu cầu không tồn tại hoặc đã được lưu trữ trên hệ thống.
        </p>
        <Link
          href="/orders"
          className="h-10 px-6 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors"
        >
          Quay lại danh sách đơn hàng
        </Link>
      </div>
    );
  }

  // 3. 403 Access Restricted State
  if (isAccessDenied) {
    return (
      <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-rose-200 rounded-xl shadow-sm my-12">
        <div
          className="w-12 h-12 mx-auto mb-4 rounded-full border border-rose-200 bg-rose-50 flex items-center justify-center text-rose-600 text-xl font-bold"
          aria-hidden="true"
        >
          !
        </div>
        <h2 className="text-xl font-bold text-stone-900 mb-2">
          Không có quyền truy cập
        </h2>
        <p className="text-sm text-stone-600 mb-6">
          Bạn không có quyền xem thông tin chi tiết đơn hàng này.
        </p>
        <Link
          href="/orders"
          className="h-10 px-6 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors"
        >
          Quay lại danh sách đơn hàng
        </Link>
      </div>
    );
  }

  // 4. Server / Network Error State
  if (error || !order) {
    return (
      <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-rose-200 rounded-xl shadow-sm my-12">
        <div
          className="w-12 h-12 mx-auto mb-4 rounded-full border border-rose-200 bg-rose-50 flex items-center justify-center text-rose-600"
          aria-hidden="true"
        >
          <svg
            className="w-6 h-6"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-stone-900 mb-2">
          Không thể tải chi tiết đơn hàng
        </h2>
        <p className="text-sm text-stone-600 mb-6">{error || "Đã xảy ra sự cố kết nối."}</p>
        <button
          type="button"
          onClick={fetchOrder}
          className="h-10 px-6 text-xs font-semibold text-stone-800 bg-white border border-stone-200 hover:bg-stone-50 rounded-lg inline-flex items-center justify-center transition-colors cursor-pointer"
        >
          Thử lại
        </button>
      </div>
    );
  }

  const items = order.items || [];
  const canCancel = order.status === "PENDING" || order.status === "CONFIRMED";

  // 5. Successful Order Detail View
  return (
    <div className="max-w-[1100px] mx-auto py-4 md:py-10 px-4 md:px-8">
      {/* Breadcrumb Navigation */}
      <div className="text-xs text-stone-500 mb-4 flex items-center gap-1.5">
        <Link href="/orders" className="hover:text-stone-900 transition-colors">
          Đơn hàng
        </Link>
        <span>/</span>
        <span className="text-stone-900 font-semibold">Đơn hàng #{order.id}</span>
      </div>

      {/* Order Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 pb-6 border-b border-stone-200 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-stone-900">
            Đơn hàng #{order.id}
          </h1>
          <p className="text-xs text-stone-500 mt-1">
            Đặt lúc {formatOrderDate(order.createdAt)}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <OrderStatusBadge status={order.status} />

          {canCancel && (
            <button
              type="button"
              onClick={() => setShowCancelModal(true)}
              className="h-9 px-4 text-xs font-semibold text-rose-600 border border-rose-200 bg-white hover:bg-rose-50 rounded-lg transition-colors cursor-pointer whitespace-nowrap"
            >
              Hủy đơn hàng
            </button>
          )}
        </div>
      </div>

      {/* Order Content Grid (8-col Items / 4-col Specifications) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column (col-span-8): Ordered Items List */}
        <div className="lg:col-span-8">
          <div className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-3">
            Sản phẩm đã đặt ({items.length})
          </div>

          <div className="bg-white border border-stone-200 rounded-xl divide-y divide-stone-100 overflow-hidden shadow-sm">
            {items.map((item, idx) => (
              <div
                key={item.id ?? idx}
                className="p-6 flex flex-col sm:flex-row sm:items-start justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="text-base font-semibold text-stone-900">
                    {item.productId ? (
                      <Link
                        href={`/products/${item.productId}`}
                        className="hover:underline hover:text-amber-600 transition-colors"
                      >
                        {item.productName}
                      </Link>
                    ) : (
                      item.productName
                    )}
                  </div>
                  <div className="text-xs text-stone-400">
                    Mã SP: {item.productSku} • Số lượng: {item.quantity}
                  </div>
                  <div className="text-xs text-stone-500">
                    Đơn giá: {formatVndPrice(item.unitPrice)}
                  </div>
                </div>

                <div className="text-base font-bold text-stone-900 sm:text-right tabular-nums whitespace-nowrap">
                  {formatVndPrice(item.subtotal)}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Column (col-span-4): Specifications & Details */}
        <div className="lg:col-span-4 space-y-6">
          {/* Order Total Card */}
          <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
            <div className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-2">
              Tổng tiền đơn hàng
            </div>
            <div className="text-2xl font-extrabold text-amber-600 tabular-nums">
              {formatVndPrice(order.totalAmount)}
            </div>
          </div>

          {/* Shipping Address & Delivery Notes */}
          <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm space-y-4">
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-2">
                Địa chỉ giao hàng
              </div>
              <p className="text-sm text-stone-800 leading-relaxed whitespace-pre-line">
                {order.shippingAddress || "Chưa có địa chỉ giao hàng."}
              </p>
            </div>

            {order.notes && order.notes.trim() && (
              <div className="pt-3 border-t border-stone-100">
                <div className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-1">
                  Ghi chú giao hàng
                </div>
                <p className="text-sm text-stone-600 italic leading-relaxed whitespace-pre-line">
                  {order.notes}
                </p>
              </div>
            )}
          </div>

          {/* Payment Card */}
          <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
            <div className="text-xs font-semibold uppercase tracking-wider text-stone-500 mb-3">
              Thanh toán
            </div>
            {order.paymentTransactionId ? (
              <div>
                <div className="text-[11px] uppercase tracking-wider text-stone-400 mb-0.5">
                  Mã giao dịch
                </div>
                <div className="font-mono text-xs font-bold text-stone-800 break-all">
                  {order.paymentTransactionId}
                </div>
              </div>
            ) : (
              <div className="text-xs text-stone-500">
                Thanh toán tiêu chuẩn
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Cancellation Confirmation Dialog */}
      {showCancelModal && (
        <div
          className="fixed inset-0 z-50 bg-stone-900/40 backdrop-blur-xs flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="cancel-dialog-title"
        >
          <div className="bg-white border border-stone-200 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <h3
              id="cancel-dialog-title"
              className="text-lg font-bold text-stone-900"
            >
              Xác nhận hủy đơn hàng
            </h3>
            <p className="text-sm text-stone-600 leading-relaxed">
              Bạn có chắc chắn muốn hủy đơn hàng này không? Sản phẩm đang giữ trong đơn hàng sẽ được hoàn lại kho.
            </p>

            {cancelError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-700">
                {cancelError}
              </div>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  if (!isCancelling) {
                    setShowCancelModal(false);
                    setCancelError(null);
                  }
                }}
                disabled={isCancelling}
                className="h-10 px-4 text-xs font-semibold bg-white border border-stone-200 text-stone-700 rounded-lg hover:bg-stone-50 transition-colors cursor-pointer"
              >
                Giữ đơn hàng
              </button>
              <button
                type="button"
                onClick={handleConfirmCancel}
                disabled={isCancelling}
                className="h-10 px-4 text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white rounded-lg transition-colors disabled:opacity-50 cursor-pointer shadow-sm"
              >
                {isCancelling ? "Đang hủy..." : "Xác nhận hủy đơn"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function OrderDetailPage({
  params,
}: {
  params: Promise<{ orderId: string }>;
}) {
  return (
    <ProtectedRoute>
      <CustomerShell>
        <Suspense fallback={<OrderDetailSkeleton />}>
          <OrderDetailContent params={params} />
        </Suspense>
      </CustomerShell>
    </ProtectedRoute>
  );
}
