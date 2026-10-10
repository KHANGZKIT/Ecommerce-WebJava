"use client";

import React, { useEffect, useState, useCallback, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProtectedRoute } from "@/features/auth/AuthGuard";
import { useAuth } from "@/features/auth/AuthContext";
import { formatVndPrice } from "@/components/product/ProductCard";
import { getOrdersByUserApi } from "@/lib/api/orders";
import { ApiError } from "@/lib/api/client";
import { OrderDto, OrderStatus, OrderItemDto, Page } from "@/types/order";

const PAGE_SIZE = 10;

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

function formatOrderItems(items: OrderItemDto[] | undefined): string {
  if (!items || items.length === 0) {
    return "0 sản phẩm";
  }
  const count = items.length;
  const countLabel = `${count} sản phẩm`;
  const firstProductName = items[0]?.productName;
  return firstProductName ? `${countLabel} • ${firstProductName}` : countLabel;
}

function OrdersLoadingSkeleton() {
  return (
    <div className="border border-stone-200 bg-white rounded-xl overflow-hidden mt-8 shadow-sm">
      <div className="bg-stone-50 border-b border-stone-100 px-6 py-3.5 h-11" />
      <div className="divide-y divide-stone-100 p-6 space-y-6">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex justify-between items-center py-2 animate-pulse">
            <div className="space-y-2">
              <div className="h-4 w-32 bg-stone-200 rounded" />
              <div className="h-3 w-48 bg-stone-100 rounded" />
            </div>
            <div className="h-8 w-24 bg-stone-100 border border-stone-200 rounded-lg" />
          </div>
        ))}
      </div>
    </div>
  );
}

function OrdersContent() {
  const { user } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  const rawPage = searchParams.get("page");
  const pageParam = Math.max(1, parseInt(rawPage || "1", 10) || 1);
  const backendPage = pageParam - 1;

  const [pageData, setPageData] = useState<Page<OrderDto> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isAccessDenied, setIsAccessDenied] = useState<boolean>(false);

  const fetchOrders = useCallback(
    async (targetBackendPage: number) => {
      if (!user?.id) return;
      setIsLoading(true);
      setError(null);
      setIsAccessDenied(false);

      try {
        const data = await getOrdersByUserApi(user.id, targetBackendPage, PAGE_SIZE);
        setPageData(data);
      } catch (err: unknown) {
        if (err instanceof ApiError && err.status === 403) {
          setIsAccessDenied(true);
          setError("Bạn không có quyền truy cập danh sách đơn hàng.");
        } else {
          const msg =
            err instanceof ApiError
              ? err.message
              : err instanceof Error
              ? err.message
              : "Không thể lấy danh sách đơn hàng. Vui lòng thử lại sau.";
          setError(msg);
        }
      } finally {
        setIsLoading(false);
      }
    },
    [user?.id]
  );

  useEffect(() => {
    if (user?.id) {
      fetchOrders(backendPage);
    }
  }, [user?.id, backendPage, fetchOrders]);

  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(newPage));
    router.push(`/orders?${params.toString()}`);
  };

  return (
    <div className="max-w-[1100px] mx-auto py-4 md:py-8">
      {/* Header & Breadcrumb */}
      <div className="pb-6 border-b border-stone-200 mb-8">
        <div className="text-xs text-stone-500 uppercase tracking-wider mb-1.5">
          Tài khoản // Lịch sử mua hàng
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-stone-900 mb-2">
          Đơn hàng của tôi
        </h1>
        <p className="text-sm text-stone-500">
          Theo dõi quá trình xử lý, trạng thái giao nhận và lịch sử các đơn hàng đã đặt.
        </p>
      </div>

      {/* Access Denied State (403) */}
      {isAccessDenied && (
        <div className="max-w-md mx-auto py-12 px-6 text-center bg-white border border-rose-200 rounded-xl shadow-sm">
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
            Bạn không có quyền xem thông tin danh sách đơn hàng này.
          </p>
          <Link
            href="/shop"
            className="h-10 px-6 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors"
          >
            Về cửa hàng
          </Link>
        </div>
      )}

      {/* Generic Error State (Non-403) */}
      {!isAccessDenied && error && (
        <div className="max-w-md mx-auto py-12 px-6 text-center bg-white border border-rose-200 rounded-xl shadow-sm">
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
            Không thể tải đơn hàng
          </h2>
          <p className="text-sm text-stone-600 mb-6">{error}</p>
          <button
            type="button"
            onClick={() => fetchOrders(backendPage)}
            className="h-10 px-6 text-xs font-semibold text-stone-800 bg-white border border-stone-200 hover:bg-stone-50 rounded-lg inline-flex items-center justify-center transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Loading State */}
      {!isAccessDenied && !error && isLoading && <OrdersLoadingSkeleton />}

      {/* Empty State */}
      {!isAccessDenied &&
        !error &&
        !isLoading &&
        (!pageData || pageData.totalElements === 0 || pageData.empty) && (
          <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-stone-200 rounded-xl shadow-sm">
            <div
              className="w-16 h-16 mx-auto mb-4 rounded-full bg-stone-100 flex items-center justify-center text-2xl text-stone-400"
              aria-hidden="true"
            >
              📦
            </div>
            <h2 className="text-xl font-bold text-stone-900 mb-2">
              Bạn chưa có đơn hàng nào
            </h2>
            <p className="text-sm text-stone-500 mb-6 leading-relaxed">
              Hãy khám phá bộ sưu tập thiết bị công nghệ chính hãng tại TECHGEAR.
            </p>
            <Link
              href="/shop"
              className="h-11 px-8 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors shadow-sm"
            >
              Khám phá sản phẩm ngay
            </Link>
          </div>
        )}

      {/* Orders Table Card */}
      {!isAccessDenied &&
        !error &&
        !isLoading &&
        pageData &&
        pageData.totalElements > 0 &&
        !pageData.empty && (
          <div className="border border-stone-200 bg-white rounded-xl overflow-hidden shadow-sm">
            {/* Desktop 12-Column Table Header */}
            <div className="hidden lg:grid grid-cols-12 gap-4 items-center bg-stone-50 border-b border-stone-200 px-6 py-3.5 text-xs font-semibold text-stone-500 uppercase tracking-wider">
              <div className="col-span-2">MÃ ĐƠN HÀNG</div>
              <div className="col-span-2">NGÀY ĐẶT</div>
              <div className="col-span-2">TRẠNG THÁI</div>
              <div className="col-span-3">SẢN PHẨM</div>
              <div className="col-span-2">TỔNG TIỀN</div>
              <div className="col-span-1 text-right">THAO TÁC</div>
            </div>

            {/* Desktop Rows */}
            <div className="hidden lg:block divide-y divide-stone-100">
              {pageData.content.map((order) => (
                <div
                  key={order.id}
                  className="grid grid-cols-12 gap-4 items-center px-6 py-4 hover:bg-stone-50/60 transition-colors"
                >
                  <div className="col-span-2 font-mono text-sm font-bold text-stone-900">
                    #{order.id}
                  </div>
                  <div className="col-span-2 text-xs text-stone-500">
                    {formatOrderDate(order.createdAt)}
                  </div>
                  <div className="col-span-2">
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <div
                    className="col-span-3 text-sm text-stone-700 truncate"
                    title={formatOrderItems(order.items)}
                  >
                    {formatOrderItems(order.items)}
                  </div>
                  <div className="col-span-2 font-mono text-sm font-bold text-amber-600 tabular-nums">
                    {formatVndPrice(order.totalAmount)}
                  </div>
                  <div className="col-span-1 flex justify-end">
                    <Link
                      href={`/orders/${order.id}`}
                      className="h-8 px-3 text-xs font-semibold text-stone-700 bg-white border border-stone-200 rounded-lg hover:bg-stone-50 hover:border-stone-400 transition-all flex items-center justify-center whitespace-nowrap"
                    >
                      Chi tiết
                    </Link>
                  </div>
                </div>
              ))}
            </div>

            {/* Mobile Stacked Order Cards (< 1024px) */}
            <div className="lg:hidden divide-y divide-stone-100">
              {pageData.content.map((order) => (
                <div key={order.id} className="p-5 space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="font-mono text-sm font-bold text-stone-900">
                      Đơn hàng #{order.id}
                    </span>
                    <OrderStatusBadge status={order.status} />
                  </div>
                  <div className="text-xs text-stone-400">
                    {formatOrderDate(order.createdAt)}
                  </div>
                  <div className="text-sm text-stone-700">
                    {formatOrderItems(order.items)}
                  </div>
                  <div className="flex justify-between items-baseline pt-2 border-t border-stone-100">
                    <span className="text-xs text-stone-500">
                      Tổng tiền
                    </span>
                    <span className="font-mono text-base font-bold text-amber-600 tabular-nums">
                      {formatVndPrice(order.totalAmount)}
                    </span>
                  </div>
                  <Link
                    href={`/orders/${order.id}`}
                    className="w-full h-10 text-xs font-semibold text-stone-700 bg-white border border-stone-200 rounded-lg hover:bg-stone-50 transition-all flex items-center justify-center"
                  >
                    Xem chi tiết đơn hàng
                  </Link>
                </div>
              ))}
            </div>

            {/* Pagination Controls */}
            {pageData.totalPages > 0 && (
              <div className="px-6 py-4 bg-stone-50/50 border-t border-stone-100 flex flex-col sm:flex-row justify-between items-center gap-3">
                <div className="text-xs text-stone-500">
                  Hiển thị{" "}
                  {pageData.totalElements === 0 ? 0 : pageData.number * PAGE_SIZE + 1} -{" "}
                  {Math.min((pageData.number + 1) * PAGE_SIZE, pageData.totalElements)} trong số{" "}
                  {pageData.totalElements} đơn hàng
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handlePageChange(pageParam - 1)}
                    disabled={pageData.first || pageParam <= 1}
                    className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors ${
                      pageData.first || pageParam <= 1
                        ? "border-stone-200 bg-white text-stone-300 cursor-not-allowed"
                        : "border-stone-200 bg-white text-stone-700 hover:bg-stone-50 cursor-pointer"
                    }`}
                  >
                    Trước
                  </button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: pageData.totalPages }, (_, idx) => idx + 1)
                      .filter((p) => {
                        const total = pageData.totalPages;
                        if (total <= 5) return true;
                        if (p === 1 || p === total) return true;
                        return Math.abs(p - pageParam) <= 1;
                      })
                      .map((p) => (
                        <button
                          key={p}
                          type="button"
                          onClick={() => handlePageChange(p)}
                          className={`w-8 h-8 flex items-center justify-center text-xs rounded-lg transition-colors ${
                            p === pageParam
                              ? "bg-stone-900 text-white font-bold"
                              : "bg-white border border-stone-200 text-stone-700 hover:bg-stone-50 cursor-pointer"
                          }`}
                        >
                          {p}
                        </button>
                      ))}
                  </div>

                  <button
                    type="button"
                    onClick={() => handlePageChange(pageParam + 1)}
                    disabled={pageData.last || pageParam >= pageData.totalPages}
                    className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors ${
                      pageData.last || pageParam >= pageData.totalPages
                        ? "border-stone-200 bg-white text-stone-300 cursor-not-allowed"
                        : "border-stone-200 bg-white text-stone-700 hover:bg-stone-50 cursor-pointer"
                    }`}
                  >
                    Sau
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
    </div>
  );
}

export default function MyOrdersPage() {
  return (
    <ProtectedRoute>
      <CustomerShell>
        <Suspense fallback={<OrdersLoadingSkeleton />}>
          <OrdersContent />
        </Suspense>
      </CustomerShell>
    </ProtectedRoute>
  );
}
