"use client";

import React, { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { formatVndPrice } from "@/components/product/ProductCard";
import { getOrderByIdApi } from "@/lib/api/orders";
import { fetchApi } from "@/lib/api/client";
import { OrderDto, OrderStatus } from "@/types/order";

function resolveOrderId(searchParams: URLSearchParams): string | null {
  const allOrderIds = searchParams.getAll("orderId");
  for (const id of allOrderIds) {
    const trimmed = id.trim();
    if (/^\d+$/.test(trimmed)) {
      return trimmed;
    }
    const match = trimmed.match(/^TG_ORD_(\d+)/i);
    if (match) {
      return match[1];
    }
  }
  const extraData = searchParams.get("extraData");
  if (extraData) {
    const match = extraData.match(/orderId=(\d+)/i);
    if (match) {
      return match[1];
    }
  }
  return null;
}

function PaymentResultContent() {
  const searchParams = useSearchParams();

  const orderIdParam = resolveOrderId(searchParams);
  const rawStatus = searchParams.get("status")?.trim().toUpperCase() ?? null;
  const totalAmountParam = searchParams.get("totalAmount")?.trim() ?? null;

  const [order, setOrder] = useState<OrderDto | null>(null);
  const [isVerifying, setIsVerifying] = useState<boolean>(Boolean(orderIdParam));
  const [verifyAttempts, setVerifyAttempts] = useState<number>(0);

  // Authoritative Backend Order Verification & Active MoMo Sync
  useEffect(() => {
    if (!orderIdParam) {
      setIsVerifying(false);
      return;
    }

    let isMounted = true;
    let timeoutId: NodeJS.Timeout | null = null;

    const checkOrderStatus = async (attempt: number) => {
      try {
        // Trigger active MoMo transaction sync on attempt 0 and 2
        // to cover cases where asynchronous IPN is delayed or unreachable
        if (attempt === 0 || attempt === 2) {
          try {
            await fetchApi(`/api/v1/payments/momo/sync/${orderIdParam}`, { method: "POST" });
          } catch {
            // Non-blocking; continue to authoritative backend check
          }
        }

        const orderData = await getOrderByIdApi(orderIdParam);
        if (!isMounted) return;

        setOrder(orderData);

        // If order is still PENDING (awaiting asynchronous MoMo IPN), poll up to 5 times (1.5s interval)
        if (orderData.status === "PENDING" && attempt < 5) {
          timeoutId = setTimeout(() => {
            if (isMounted) {
              setVerifyAttempts(attempt + 1);
              checkOrderStatus(attempt + 1);
            }
          }, 1500);
        } else {
          setIsVerifying(false);
        }
      } catch (err) {
        if (!isMounted) return;
        setIsVerifying(false);
      }
    };

    checkOrderStatus(0);

    return () => {
      isMounted = false;
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [orderIdParam]);

  // Derived Authoritative State: NEVER mark an order as paid based only on frontend redirects or client-supplied status.
  // Confirmation is strictly determined by the backend database order state.
  const effectiveStatus: OrderStatus | string = order
    ? order.status
    : rawStatus && rawStatus !== "CONFIRMED"
    ? rawStatus
    : "PAYMENT_UNKNOWN";

  const effectiveAmount = order
    ? order.totalAmount
    : totalAmountParam
    ? Number(totalAmountParam)
    : null;

  const formattedAmount =
    effectiveAmount !== null && !isNaN(effectiveAmount) && effectiveAmount > 0
      ? formatVndPrice(effectiveAmount)
      : null;

  // -------------------------------------------------------------------------
  // 0. VERIFYING IN PROGRESS (Polling backend database)
  // -------------------------------------------------------------------------
  if (isVerifying) {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-8 max-w-xl mx-auto my-6 md:my-12 text-center shadow-sm"
        aria-live="polite"
      >
        <div className="w-16 h-16 rounded-full bg-amber-50 border border-amber-100 flex items-center justify-center mx-auto mb-5 text-amber-600 animate-spin">
          <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24">
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-stone-900 mb-2">
          Đang xác thực giao dịch với TECHGEAR...
        </h1>
        <p className="text-xs text-stone-500 mb-4">
          Hệ thống đang đồng bộ kết quả thanh toán từ cổng MoMo vào cơ sở dữ liệu. Vui lòng chờ giây lát.
        </p>
        <div className="h-1.5 w-48 bg-stone-100 rounded-full mx-auto overflow-hidden">
          <div className="h-full bg-amber-500 rounded-full animate-pulse w-3/4" />
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // 1. ORDER CONFIRMED (Authoritative DB status is CONFIRMED)
  // -------------------------------------------------------------------------
  if (
    orderIdParam &&
    (effectiveStatus === "CONFIRMED" || effectiveStatus === "CONFIRMED_CART_CLEAR_PENDING")
  ) {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        <div
          className="w-16 h-16 rounded-full bg-emerald-50 border border-emerald-100 flex items-center justify-center mx-auto mb-5 text-emerald-600"
          aria-hidden="true"
        >
          <svg
            className="w-8 h-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2.5}
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Đặt hàng thành công!
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Giao dịch thanh toán đã hoàn tất và được hệ thống TECHGEAR xác nhận thành công.
        </p>

        <div className="bg-stone-50 border border-stone-100 rounded-xl p-5 text-left mb-6 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="text-stone-500 uppercase tracking-wider font-semibold">
              Mã đơn hàng
            </span>
            <span className="font-mono text-sm font-bold text-stone-900">
              #{orderIdParam}
            </span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-stone-500 uppercase tracking-wider font-semibold">
              Trạng thái đơn hàng
            </span>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-700">
              Đã xác nhận
            </span>
          </div>

          {order?.paymentTransactionId && (
            <div className="flex items-center justify-between text-xs">
              <span className="text-stone-500 uppercase tracking-wider font-semibold">
                Mã giao dịch
              </span>
              <span className="font-mono text-xs font-medium text-stone-600 truncate max-w-[220px]">
                {order.paymentTransactionId}
              </span>
            </div>
          )}

          {formattedAmount && (
            <>
              <div className="border-t border-stone-200/60 my-2" />
              <div className="flex items-baseline justify-between">
                <span className="text-sm font-bold text-stone-900">
                  Tổng thanh toán
                </span>
                <span className="text-xl font-extrabold text-amber-600 tabular-nums">
                  {formattedAmount}
                </span>
              </div>
            </>
          )}
        </div>

        <div className="space-y-3">
          <Link
            href={`/orders/${orderIdParam}`}
            className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Xem chi tiết đơn hàng
          </Link>
          <Link
            href="/shop"
            className="flex items-center justify-center w-full h-11 rounded-lg bg-white hover:bg-stone-50 border border-stone-200 text-stone-700 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // 2. PAYMENT FAILED / CANCELLED
  // -------------------------------------------------------------------------
  if (
    orderIdParam &&
    (effectiveStatus === "PAYMENT_FAILED" || effectiveStatus === "CANCELLED")
  ) {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        <div
          className="w-16 h-16 rounded-full bg-rose-50 border border-rose-100 flex items-center justify-center mx-auto mb-5 text-rose-600"
          aria-hidden="true"
        >
          <svg
            className="w-8 h-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2.5}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 9v4m0 4h.01M12 3a9 9 0 100 18 9 9 0 000-18z"
            />
          </svg>
        </div>

        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Thanh toán không thành công
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Giao dịch thanh toán chưa hoàn tất hoặc bị hủy trên cổng thanh toán MoMo.
        </p>

        <div className="bg-stone-50 border border-stone-100 rounded-xl p-5 text-left mb-6 space-y-2">
          <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
            <span className="text-stone-500 font-semibold uppercase">
              Mã tham chiếu đơn hàng
            </span>
            <span className="font-mono text-xs font-bold text-stone-900">
              #{orderIdParam}
            </span>
          </div>
          <p className="text-xs text-stone-600 leading-relaxed">
            Hệ thống đã an toàn giải phóng tồn kho đã giữ. Bạn có thể thử lại quy trình thanh toán bất cứ lúc nào.
          </p>
        </div>

        <div className="space-y-3">
          <Link
            href="/checkout"
            className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Thử lại thanh toán
          </Link>
          <Link
            href="/cart"
            className="flex items-center justify-center w-full h-11 rounded-lg bg-white hover:bg-stone-50 border border-stone-200 text-stone-700 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Quay lại giỏ hàng
          </Link>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // 3. PAYMENT PENDING (Awaiting user confirmation)
  // -------------------------------------------------------------------------
  if (orderIdParam && effectiveStatus === "PENDING") {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        <div
          className="w-16 h-16 rounded-full bg-amber-50 border border-amber-100 flex items-center justify-center mx-auto mb-5 text-amber-600"
          aria-hidden="true"
        >
          <svg
            className="w-8 h-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2.5}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>

        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Đơn hàng đang chờ thanh toán
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Chúng tôi đang chờ xác nhận thanh toán từ ứng dụng MoMo của bạn.
        </p>

        <div className="bg-amber-50 border border-amber-100 rounded-xl p-5 text-left mb-6 space-y-2">
          <div className="font-mono text-xs font-bold text-stone-900">
            Đơn hàng #{orderIdParam}
          </div>
          <p className="text-xs text-amber-800 leading-relaxed">
            Nếu bạn đã quét mã và thanh toán thành công trên ứng dụng MoMo, trạng thái sẽ tự động cập nhật trong danh sách Đơn hàng của bạn sau ít phút.
          </p>
        </div>

        <div className="space-y-3">
          <Link
            href={`/orders/${orderIdParam}`}
            className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Kiểm tra trạng thái đơn hàng
          </Link>
          <Link
            href="/shop"
            className="flex items-center justify-center w-full h-11 rounded-lg bg-white hover:bg-stone-50 border border-stone-200 text-stone-700 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // 4. INVALID / MISSING PARAMETERS FALLBACK
  // -------------------------------------------------------------------------
  return (
    <div
      className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
      aria-live="polite"
    >
      <div
        className="w-16 h-16 rounded-full bg-stone-100 border border-stone-200 flex items-center justify-center mx-auto mb-5 text-stone-400"
        aria-hidden="true"
      >
        <svg
          className="w-8 h-8"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>

      <h1 className="text-2xl font-extrabold text-stone-900 mb-2">
        Thông tin giao dịch không hợp lệ
      </h1>
      <p className="text-sm text-stone-600 mb-6">
        Không tìm thấy thông tin đơn hàng hợp lệ cần xác minh.
      </p>

      <div className="space-y-3">
        <Link
          href="/orders"
          className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
        >
          Xem danh sách đơn hàng
        </Link>
        <Link
          href="/shop"
          className="flex items-center justify-center w-full h-11 rounded-lg bg-white hover:bg-stone-50 border border-stone-200 text-stone-700 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-stone-900"
        >
          Quay lại cửa hàng
        </Link>
      </div>
    </div>
  );
}

function PaymentResultFallback() {
  return (
    <div className="bg-white border border-stone-200 rounded-2xl p-8 max-w-xl mx-auto my-6 md:my-12 text-center animate-pulse shadow-sm">
      <div className="w-16 h-16 rounded-full bg-stone-100 mx-auto mb-5" />
      <div className="h-6 w-48 bg-stone-200 rounded mx-auto mb-3" />
      <div className="h-4 w-64 bg-stone-100 rounded mx-auto mb-6" />
      <div className="h-28 bg-stone-50 rounded-xl mx-auto mb-6" />
      <div className="h-12 bg-stone-200 rounded-lg mx-auto mb-3" />
      <div className="h-11 bg-stone-100 rounded-lg mx-auto" />
    </div>
  );
}

export default function PaymentResultPage() {
  return (
    <CustomerShell>
      <Suspense fallback={<PaymentResultFallback />}>
        <PaymentResultContent />
      </Suspense>
    </CustomerShell>
  );
}
