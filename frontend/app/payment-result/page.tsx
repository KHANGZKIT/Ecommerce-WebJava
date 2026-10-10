"use client";

import React, { Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { formatVndPrice } from "@/components/product/ProductCard";

type SupportedCheckoutStatus =
  | "CONFIRMED"
  | "CONFIRMED_CART_CLEAR_PENDING"
  | "PAYMENT_FAILED"
  | "PAYMENT_UNKNOWN";

function isSupportedStatus(status: string | null): status is SupportedCheckoutStatus {
  return (
    status === "CONFIRMED" ||
    status === "CONFIRMED_CART_CLEAR_PENDING" ||
    status === "PAYMENT_FAILED" ||
    status === "PAYMENT_UNKNOWN"
  );
}

function PaymentResultContent() {
  const searchParams = useSearchParams();

  const rawStatus = searchParams.get("status")?.trim().toUpperCase() ?? null;
  const orderIdParam = searchParams.get("orderId")?.trim() ?? null;
  const totalAmountParam = searchParams.get("totalAmount")?.trim() ?? null;

  // Validate status against backend contract
  const isValidStatus = isSupportedStatus(rawStatus);

  // If status is confirmed/pending, orderId is required to construct authoritative order links
  const isSuccessStatus =
    rawStatus === "CONFIRMED" || rawStatus === "CONFIRMED_CART_CLEAR_PENDING";
  const isSuccessValid = isSuccessStatus && Boolean(orderIdParam);

  // Parse optional total amount for display formatting
  const parsedAmount = totalAmountParam ? Number(totalAmountParam) : null;
  const formattedAmount =
    parsedAmount !== null && !isNaN(parsedAmount) && parsedAmount > 0
      ? formatVndPrice(parsedAmount)
      : null;

  // =========================================================================
  // STATE 1: ORDER CONFIRMED (CONFIRMED / CONFIRMED_CART_CLEAR_PENDING)
  // =========================================================================
  if (isSuccessValid && orderIdParam) {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        {/* Status Indicator */}
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

        {/* Headline & Subtitle */}
        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Đặt hàng thành công!
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Cảm ơn bạn đã mua sắm tại TECHGEAR. Đơn hàng của bạn đã được ghi nhận.
        </p>

        {/* Order Details Inset */}
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
              Trạng thái
            </span>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full bg-emerald-50 border border-emerald-200 text-xs font-semibold text-emerald-700">
              Đã xác nhận
            </span>
          </div>

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

        {/* Factual Notice */}
        <p className="text-xs text-stone-400 mb-6 leading-relaxed">
          Thông tin chi tiết và tiến trình xử lý đơn hàng đã được lưu vào mục Đơn hàng trong tài khoản của bạn.
        </p>

        {/* Action CTAs */}
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

  // =========================================================================
  // STATE 2: PAYMENT FAILED (PAYMENT_FAILED)
  // =========================================================================
  if (isValidStatus && rawStatus === "PAYMENT_FAILED") {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        {/* Status Indicator */}
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

        {/* Headline & Subtitle */}
        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Thanh toán không thành công
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Giao dịch thanh toán chưa hoàn tất. Bạn có thể kiểm tra lại số dư hoặc chọn phương thức khác.
        </p>

        {/* Explanatory Inset */}
        <div className="bg-stone-50 border border-stone-100 rounded-xl p-5 text-left mb-6 space-y-2">
          {orderIdParam && (
            <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
              <span className="text-stone-500 font-semibold uppercase">
                Mã tham chiếu
              </span>
              <span className="font-mono text-xs font-bold text-stone-900">
                #{orderIdParam}
              </span>
            </div>
          )}
          <p className="text-xs text-stone-600 leading-relaxed">
            Hệ thống đã tự động giải phóng tồn kho đã giữ. Vui lòng thử lại quy trình thanh toán.
          </p>
        </div>

        {/* Action CTAs */}
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

  // =========================================================================
  // STATE 3: PAYMENT UNKNOWN (PAYMENT_UNKNOWN)
  // =========================================================================
  if (isValidStatus && rawStatus === "PAYMENT_UNKNOWN") {
    return (
      <div
        className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
        aria-live="polite"
      >
        {/* Status Indicator */}
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
              d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M12 21a9 9 0 100-18 9 9 0 000-18z"
            />
          </svg>
        </div>

        {/* Headline & Subtitle */}
        <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 mb-2">
          Đang xác nhận kết quả thanh toán
        </h1>
        <p className="text-sm text-stone-600 mb-6">
          Chúng tôi đang đồng bộ trạng thái giao dịch với cổng thanh toán.
        </p>

        {/* Advisory Inset */}
        <div className="bg-amber-50 border border-amber-100 rounded-xl p-5 text-left mb-6 space-y-2">
          {orderIdParam && (
            <div className="font-mono text-xs font-bold text-stone-900">
              Đơn hàng #{orderIdParam}
            </div>
          )}
          <p className="text-xs text-amber-800 leading-relaxed">
            Vui lòng kiểm tra lại danh sách Đơn hàng của bạn sau ít phút trước khi thực hiện giao dịch mới.
          </p>
        </div>

        {/* Action CTAs */}
        <div className="space-y-3">
          <Link
            href="/orders"
            className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
          >
            Kiểm tra đơn hàng của tôi
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

  // =========================================================================
  // STATE 4: INVALID / MISSING PARAMETERS FALLBACK
  // =========================================================================
  return (
    <div
      className="bg-white border border-stone-200 rounded-2xl p-6 sm:p-10 text-center max-w-xl mx-auto my-6 md:my-12 shadow-sm"
      aria-live="polite"
    >
      {/* Fallback Indicator */}
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

      {/* Headline & Subtitle */}
      <h1 className="text-2xl font-extrabold text-stone-900 mb-2">
        Thông tin giao dịch không hợp lệ
      </h1>
      <p className="text-sm text-stone-600 mb-6">
        Không tìm thấy mã đơn hàng hoặc trạng thái thanh toán hợp lệ.
      </p>

      {/* Inset Guidance */}
      <div className="bg-stone-50 border border-stone-100 rounded-xl p-5 text-left mb-6">
        <p className="text-xs text-stone-500 leading-relaxed">
          Bạn có thể truy cập mục Đơn hàng để kiểm tra các giao dịch gần nhất, hoặc quay lại trang danh mục sản phẩm.
        </p>
      </div>

      {/* Action CTAs */}
      <div className="space-y-3">
        <Link
          href="/orders"
          className="flex items-center justify-center w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white text-sm font-semibold transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-stone-900"
        >
          Xem đơn hàng của tôi
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
