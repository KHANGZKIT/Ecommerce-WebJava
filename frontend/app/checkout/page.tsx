"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProtectedRoute } from "@/features/auth/AuthGuard";
import { formatVndPrice } from "@/components/product/ProductCard";
import { getCartApi } from "@/lib/api/cart";
import { checkoutApi } from "@/lib/api/checkout";
import { ApiError } from "@/lib/api/client";
import { CartDto } from "@/types/cart";
import { CheckoutRequest, CheckoutResponse } from "@/types/order";
import { PaymentMethod } from "@/types/payment";

const PAYMENT_METHODS: { id: PaymentMethod; label: string; desc: string }[] = [
  { id: "CREDIT_CARD", label: "Thẻ tín dụng", desc: "Visa, MasterCard, JCB" },
  { id: "DEBIT_CARD", label: "Thẻ ghi nợ / ATM", desc: "Thẻ ATM nội địa Napas" },
  { id: "UPI", label: "Chuyển khoản QR", desc: "Quét mã QR ngân hàng nhanh chóng" },
  { id: "NET_BANKING", label: "Internet Banking", desc: "Cổng thanh toán ngân hàng trực tuyến" },
  { id: "WALLET", label: "Ví điện tử", desc: "Thanh toán qua ví điện tử liên kết" },
];

function CheckoutContent() {
  const router = useRouter();

  const [cart, setCart] = useState<CartDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [cartError, setCartError] = useState<string | null>(null);

  // Form State
  const [shippingAddress, setShippingAddress] = useState<string>("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("CREDIT_CARD");
  const [notes, setNotes] = useState<string>("");

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Load Real Cart from API
  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setCartError(null);

    getCartApi()
      .then((data) => {
        if (!isMounted) return;
        setCart(data);
      })
      .catch((err: unknown) => {
        if (!isMounted) return;
        const msg =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : "Không thể lấy thông tin giỏ hàng để thanh toán.";
        setCartError(msg);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Form Submission
  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();

    // Prevent double-submit
    if (isSubmitting) return;

    // Reset previous errors
    setValidationError(null);
    setSubmitError(null);

    // Validate cart presence
    if (!cart || !cart.items || cart.items.length === 0) {
      setValidationError("Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi đặt hàng.");
      return;
    }

    // Validate delivery address (per backend @Size(min=10, max=500))
    const trimmedAddress = shippingAddress.trim();
    if (!trimmedAddress || trimmedAddress.length < 10) {
      setValidationError("Địa chỉ giao hàng phải có ít nhất 10 ký tự.");
      return;
    }
    if (trimmedAddress.length > 500) {
      setValidationError("Địa chỉ giao hàng không được vượt quá 500 ký tự.");
      return;
    }

    // Validate notes (max 500 chars)
    const trimmedNotes = notes.trim();
    if (trimmedNotes.length > 500) {
      setValidationError("Ghi chú đơn hàng không được vượt quá 500 ký tự.");
      return;
    }

    setIsSubmitting(true);

    // Generate explicit idempotency key at submission layer (per backend OrderController requirement)
    const idempotencyKey =
      typeof crypto !== "undefined" && crypto.randomUUID
        ? crypto.randomUUID()
        : `order-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    try {
      const payload: CheckoutRequest = {
        shippingAddress: trimmedAddress,
        paymentMethod,
        notes: trimmedNotes || undefined,
      };

      const res: CheckoutResponse = await checkoutApi(payload, idempotencyKey);

      // Navigate to /payment-result with backend response metadata
      const orderIdParam = res.orderId ? `&orderId=${res.orderId}` : "";
      const statusParam = `status=${encodeURIComponent(res.status || "CONFIRMED")}`;
      const amountParam = res.totalAmount
        ? `&totalAmount=${res.totalAmount}`
        : cart.totalPrice
        ? `&totalAmount=${cart.totalPrice}`
        : "";

      router.push(`/payment-result?${statusParam}${orderIdParam}${amountParam}`);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Không thể xử lý đơn hàng. Vui lòng kiểm tra lại thông tin và thử lại.";
      setSubmitError(msg);
      setIsSubmitting(false);
    }
  };

  // =========================================================================
  // 1. LOADING SKELETON
  // =========================================================================
  if (isLoading) {
    return (
      <div className="py-8">
        <div className="h-4 w-32 bg-stone-200 rounded mb-2 animate-pulse" />
        <div className="h-8 w-48 bg-stone-200 rounded mb-8 animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-8 space-y-6">
            <div className="h-48 bg-white border border-stone-200 rounded-xl p-6 animate-pulse" />
            <div className="h-44 bg-white border border-stone-200 rounded-xl p-6 animate-pulse" />
            <div className="h-36 bg-white border border-stone-200 rounded-xl p-6 animate-pulse" />
          </div>
          <div className="lg:col-span-4">
            <div className="h-64 bg-white border border-stone-200 rounded-xl p-6 animate-pulse" />
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 2. ERROR LOADING CART
  // =========================================================================
  if (cartError || !cart) {
    return (
      <div className="py-12">
        <div className="p-8 border border-rose-200 bg-rose-50 rounded-xl max-w-xl mx-auto my-12 text-center">
          <div className="text-xs font-semibold text-rose-600 uppercase tracking-wider mb-2">
            Lỗi tải dữ liệu thanh toán
          </div>
          <h2 className="text-xl font-bold text-stone-900 mb-2">
            Không thể tải thông tin đơn hàng
          </h2>
          <p className="text-sm text-rose-800 mb-6">
            {cartError || "Đã xảy ra lỗi mạng trong quá trình truy xuất giỏ hàng của bạn."}
          </p>
          <Link
            href="/cart"
            className="inline-flex h-10 px-6 rounded-lg bg-stone-900 text-white text-xs font-semibold items-center justify-center hover:bg-stone-800 transition-colors"
          >
            ← Quay lại giỏ hàng
          </Link>
        </div>
      </div>
    );
  }

  const items = cart.items || [];
  const itemCount = items.length;
  const totalPrice = cart.totalPrice || 0;

  // =========================================================================
  // 3. EMPTY CART GUARD
  // =========================================================================
  if (itemCount === 0) {
    return (
      <div className="py-12">
        <div className="py-16 px-8 max-w-xl mx-auto my-12 text-center bg-white border border-stone-200 rounded-xl shadow-sm">
          <div className="text-3xl mb-3">🛒</div>
          <h2 className="text-2xl font-bold text-stone-900 tracking-tight mb-2">
            Chưa có sản phẩm nào để thanh toán
          </h2>
          <p className="text-sm text-stone-500 max-w-md mx-auto mb-6 leading-relaxed">
            Giỏ hàng của bạn hiện đang trống. Vui lòng chọn sản phẩm trước khi tiến hành thanh toán đơn hàng.
          </p>
          <Link
            href="/shop"
            className="inline-flex h-10 px-6 rounded-lg bg-stone-900 text-white text-xs font-semibold items-center justify-center hover:bg-stone-800 transition-colors"
          >
            Khám phá danh mục sản phẩm →
          </Link>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 4. CHECKOUT WORKFLOW (65% Inputs & Review / 35% Summary Grid)
  // =========================================================================
  return (
    <div className="py-8">
      {/* Page Heading Area */}
      <div className="pt-2 pb-6 border-b border-stone-200 mb-8">
        <div className="text-xs text-stone-500 uppercase tracking-wider mb-1">
          Giỏ hàng → Thanh toán
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-stone-900">
          Xác nhận đặt hàng
        </h1>
      </div>

      {/* Global Validation & Submission Alerts */}
      {validationError && (
        <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-lg flex items-center justify-between text-xs text-rose-700">
          <span>✕ {validationError}</span>
          <button
            type="button"
            onClick={() => setValidationError(null)}
            className="font-semibold underline hover:text-rose-900 cursor-pointer ml-3 shrink-0"
          >
            Đóng
          </button>
        </div>
      )}

      {submitError && (
        <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-lg flex items-center justify-between text-xs text-rose-700">
          <span>✕ {submitError}</span>
          <button
            type="button"
            onClick={() => setSubmitError(null)}
            className="font-semibold underline hover:text-rose-900 cursor-pointer ml-3 shrink-0"
          >
            Đóng
          </button>
        </div>
      )}

      <form onSubmit={handlePlaceOrder}>
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start mb-16">
          {/* ============================================================ */}
          {/* LEFT COLUMN: INPUTS & REVIEW (~65%)                          */}
          {/* ============================================================ */}
          <div
            className={`lg:col-span-8 space-y-6 ${
              isSubmitting ? "pointer-events-none opacity-80" : ""
            }`}
          >
            {/* Card 1: Shipping Destination */}
            <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
              <h2 className="text-lg font-bold text-stone-900 mb-1">
                Địa chỉ giao hàng
              </h2>
              <p className="text-xs text-stone-500 mb-4">
                Vui lòng nhập địa chỉ nhận hàng chi tiết và chính xác tại Việt Nam.
              </p>

              <div className="space-y-4">
                <div>
                  <label
                    htmlFor="shippingAddress"
                    className="block text-xs font-semibold text-stone-700 uppercase mb-1.5"
                  >
                    ĐỊA CHỈ NHẬN HÀNG *
                  </label>
                  <textarea
                    id="shippingAddress"
                    required
                    rows={3}
                    value={shippingAddress}
                    onChange={(e) => setShippingAddress(e.target.value)}
                    placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố"
                    className="w-full p-3 bg-stone-50 border border-stone-200 rounded-lg text-sm text-stone-900 placeholder:text-stone-400 focus:bg-white focus:border-stone-900 focus:outline-none transition-colors"
                  />
                  <div className="flex justify-between items-center mt-1 text-[11px] text-stone-400">
                    <span>Tối thiểu 10 ký tự</span>
                    <span>{shippingAddress.length}/500</span>
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="notes"
                    className="block text-xs font-semibold text-stone-700 uppercase mb-1.5"
                  >
                    GHI CHÚ ĐƠN HÀNG (TÙY CHỌN)
                  </label>
                  <input
                    id="notes"
                    type="text"
                    maxLength={500}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Chỉ dẫn giao hàng (ví dụ: giao giờ hành chính, gọi trước khi giao...)"
                    className="w-full h-11 px-3 bg-stone-50 border border-stone-200 rounded-lg text-sm text-stone-900 placeholder:text-stone-400 focus:bg-white focus:border-stone-900 focus:outline-none transition-colors"
                  />
                </div>
              </div>
            </div>

            {/* Card 2: Payment Method */}
            <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
              <h2 className="text-lg font-bold text-stone-900 mb-1">
                Phương thức thanh toán
              </h2>
              <p className="text-xs text-stone-500 mb-4">
                Chọn hình thức thanh toán thuận tiện nhất cho bạn.
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                {PAYMENT_METHODS.map((method) => {
                  const isSelected = paymentMethod === method.id;
                  return (
                    <button
                      key={method.id}
                      type="button"
                      onClick={() => setPaymentMethod(method.id)}
                      className={`p-4 rounded-xl text-left transition-all cursor-pointer flex flex-col justify-between ${
                        isSelected
                          ? "border-2 border-stone-900 bg-stone-50 shadow-sm"
                          : "border border-stone-200 bg-white hover:bg-stone-50/50"
                      }`}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-bold text-stone-900">
                          {method.label}
                        </span>
                        <span
                          className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                            isSelected
                              ? "border-stone-900 bg-stone-900"
                              : "border-stone-300 bg-white"
                          }`}
                        >
                          {isSelected && (
                            <span className="w-1.5 h-1.5 rounded-full bg-white" />
                          )}
                        </span>
                      </div>
                      <span className="text-xs text-stone-500 leading-relaxed">
                        {method.desc}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Card 3: Review Items */}
            <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
              <div className="flex items-baseline justify-between mb-4 pb-3 border-b border-stone-100">
                <h2 className="text-lg font-bold text-stone-900">
                  Kiểm tra sản phẩm ({itemCount})
                </h2>
                <Link
                  href="/cart"
                  className="text-xs font-semibold text-amber-600 hover:text-amber-700 hover:underline"
                >
                  Chỉnh sửa trong giỏ
                </Link>
              </div>

              <div className="divide-y divide-stone-100">
                {items.map((item) => {
                  const itemId = item.id ?? item.productId;
                  return (
                    <div
                      key={itemId}
                      className="py-3.5 flex items-center justify-between gap-4 first:pt-0 last:pb-0"
                    >
                      <div className="flex items-center gap-3.5 min-w-0">
                        <div className="w-14 h-14 bg-stone-50 border border-stone-100 rounded-lg p-1 flex items-center justify-center shrink-0 overflow-hidden">
                          {item.imageUrl ? (
                            <img
                              src={item.imageUrl}
                              alt={item.productName}
                              className="w-full h-full object-cover rounded"
                            />
                          ) : (
                            <div className="w-5 h-5 text-stone-300">◻</div>
                          )}
                        </div>

                        <div className="min-w-0">
                          <div className="text-sm font-semibold text-stone-900 truncate">
                            {item.productName}
                          </div>
                          <div className="text-xs text-stone-400">
                            Mã SP: {item.productSku || `TG-${item.productId}`} • Số lượng:{" "}
                            {item.quantity}
                          </div>
                        </div>
                      </div>

                      <div className="text-sm font-bold text-stone-900 tabular-nums shrink-0">
                        {formatVndPrice(item.subtotal || item.unitPrice * item.quantity)}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* ============================================================ */}
          {/* RIGHT COLUMN: STICKY ORDER SUMMARY (~35%)                    */}
          {/* ============================================================ */}
          <div className="lg:col-span-4 sticky top-24">
            <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
              <h2 className="text-lg font-bold text-stone-900 border-b border-stone-100 pb-3 mb-4">
                Tóm tắt đơn hàng
              </h2>

              <div className="space-y-3 font-sans">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-stone-600">Tạm tính</span>
                  <span className="font-semibold text-stone-900 tabular-nums">
                    {formatVndPrice(totalPrice)}
                  </span>
                </div>

                <div className="flex justify-between items-center text-sm">
                  <span className="text-stone-600">Vận chuyển</span>
                  <span className="text-xs font-semibold text-emerald-600 uppercase bg-emerald-50 px-2 py-0.5 rounded">
                    Miễn phí
                  </span>
                </div>

                <div className="pt-3 border-t border-stone-100 flex justify-between items-baseline">
                  <span className="text-base font-bold text-stone-900">
                    Tổng thanh toán
                  </span>
                  <span className="text-2xl font-extrabold text-amber-600 tabular-nums">
                    {formatVndPrice(totalPrice)}
                  </span>
                </div>
              </div>

              <p className="text-xs text-stone-400 mt-4 mb-6 leading-relaxed">
                Đơn hàng sẽ được xử lý và gửi xác nhận qua hệ thống tài khoản ngay khi bạn bấm đặt hàng.
              </p>

              {/* Primary Conversion CTA: PLACE ORDER */}
              <button
                type="submit"
                disabled={isSubmitting || itemCount === 0}
                className={`w-full h-12 rounded-lg font-semibold text-sm transition-colors flex items-center justify-center ${
                  isSubmitting || itemCount === 0
                    ? "bg-stone-200 text-stone-400 cursor-not-allowed"
                    : "bg-stone-900 hover:bg-stone-800 text-white cursor-pointer shadow-sm focus:ring-2 focus:ring-stone-900"
                }`}
              >
                {isSubmitting ? "Đang xử lý đơn hàng..." : "Đặt hàng ngay"}
              </button>

              <Link
                href="/cart"
                className="block text-center text-xs text-stone-500 hover:text-stone-900 transition-colors mt-3"
              >
                ← Quay lại giỏ hàng
              </Link>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <ProtectedRoute>
      <CustomerShell className="py-0">
        <CheckoutContent />
      </CustomerShell>
    </ProtectedRoute>
  );
}
