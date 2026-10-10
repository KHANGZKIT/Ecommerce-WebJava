"use client";

import React, { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProtectedRoute } from "@/features/auth/AuthGuard";
import { formatVndPrice } from "@/components/product/ProductCard";
import {
  getCartApi,
  updateCartItemQuantityApi,
  removeCartItemApi,
  clearCartApi,
} from "@/lib/api/cart";
import { ApiError } from "@/lib/api/client";
import { CartDto, CartItemDto } from "@/types/cart";

function CartContent() {
  const [cart, setCart] = useState<CartDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState<string | null>(null);

  // Track the item currently being updated or removed to prevent double-submits
  const [busyItemId, setBusyItemId] = useState<number | null>(null);
  const [isClearing, setIsClearing] = useState<boolean>(false);
  const [showClearConfirm, setShowClearConfirm] = useState<boolean>(false);

  // Load Cart from Redis via API Gateway
  const loadCart = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCartApi();
      setCart(data);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Unable to retrieve cart contents.";
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCart();
  }, [loadCart]);

  // Handle Quantity Change (PATCH /api/v1/cart/items/{itemId})
  const handleUpdateQuantity = async (item: CartItemDto, newQuantity: number) => {
    const itemId = item.id ?? item.productId;
    if (busyItemId !== null || isClearing) return;

    // Validate boundaries: min 0, max 9999
    if (newQuantity < 0 || newQuantity > 9999) return;

    setBusyItemId(itemId);
    setMutationError(null);

    try {
      const updatedCart = await updateCartItemQuantityApi(itemId, {
        quantity: newQuantity,
      });
      setCart(updatedCart);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Unable to update item quantity.";
      setMutationError(msg);
    } finally {
      setBusyItemId(null);
    }
  };

  // Handle Item Removal (DELETE /api/v1/cart/items/{itemId})
  const handleRemoveItem = async (item: CartItemDto) => {
    const itemId = item.id ?? item.productId;
    if (busyItemId !== null || isClearing) return;

    setBusyItemId(itemId);
    setMutationError(null);

    try {
      const updatedCart = await removeCartItemApi(itemId);
      setCart(updatedCart);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Unable to remove item from cart.";
      setMutationError(msg);
    } finally {
      setBusyItemId(null);
    }
  };

  // Handle Clear Cart (DELETE /api/v1/cart)
  const handleClearCart = async () => {
    if (isClearing || busyItemId !== null) return;

    setIsClearing(true);
    setMutationError(null);

    try {
      await clearCartApi();
      setCart({
        userId: cart?.userId || 0,
        items: [],
        totalPrice: 0,
        itemCount: 0,
      });
      setShowClearConfirm(false);
    } catch (err: unknown) {
      const msg =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Unable to clear shopping cart.";
      setMutationError(msg);
    } finally {
      setIsClearing(false);
    }
  };

  // =========================================================================
  // 1. LOADING SKELETON
  // =========================================================================
  if (isLoading) {
    return (
      <div className="py-8">
        <div className="h-8 w-44 bg-[#E7E5E4] rounded mb-8 animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          <div className="lg:col-span-8 space-y-4">
            {[1, 2].map((i) => (
              <div
                key={i}
                className="h-28 bg-white border border-[#E7E5E4] rounded-[6px] p-5 flex gap-4 animate-pulse"
              >
                <div className="w-20 h-20 bg-[#F5F5F4] rounded-[4px]" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-48 bg-[#E7E5E4] rounded" />
                  <div className="h-3 w-28 bg-[#E7E5E4] rounded" />
                  <div className="h-4 w-32 bg-[#E7E5E4] rounded" />
                </div>
              </div>
            ))}
          </div>
          <div className="lg:col-span-4">
            <div className="h-64 bg-white border border-[#E7E5E4] rounded-[6px] p-6 animate-pulse" />
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 2. ERROR STATE
  // =========================================================================
  if (error || !cart) {
    return (
      <div className="py-12">
        <div className="p-8 border border-rose-200 bg-rose-50 rounded-xl max-w-xl mx-auto my-12 text-center">
          <h2 className="text-xl font-bold text-rose-700 mb-2">
            Không thể tải giỏ hàng
          </h2>
          <p className="text-sm text-stone-600 mb-6">
            {error || "Đã xảy ra lỗi khi lấy thông tin giỏ hàng. Vui lòng thử lại."}
          </p>
          <button
            onClick={loadCart}
            className="h-10 px-6 rounded-lg bg-stone-900 text-white text-xs font-semibold hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      </div>
    );
  }

  const items = cart.items || [];
  const itemCount = items.length;
  const totalPrice = cart.totalPrice || 0;

  // =========================================================================
  // 3. EMPTY CART STATE
  // =========================================================================
  if (itemCount === 0) {
    return (
      <div className="py-12">
        <div className="py-20 px-8 max-w-xl mx-auto my-12 text-center bg-white border border-stone-200 rounded-xl">
          <div className="w-16 h-16 rounded-full bg-stone-100 flex items-center justify-center mx-auto mb-4 text-2xl text-stone-400">
            🛒
          </div>
          <h2 className="text-2xl font-bold text-stone-900 tracking-tight mb-2">
            Giỏ hàng của bạn đang trống
          </h2>
          <p className="text-sm text-stone-500 max-w-md mx-auto mb-8 leading-relaxed">
            Bạn chưa thêm sản phẩm nào vào giỏ hàng. Hãy khám phá bộ sưu tập thiết bị công nghệ chính hãng tại TECHGEAR.
          </p>
          <Link
            href="/shop"
            className="inline-flex h-11 px-8 rounded-lg bg-stone-900 text-white text-xs font-semibold items-center justify-center hover:bg-stone-800 transition-colors"
          >
            Khám phá sản phẩm
          </Link>
        </div>
      </div>
    );
  }

  // =========================================================================
  // 4. POPULATED CART (65% Items / 35% Summary Grid)
  // =========================================================================
  return (
    <div className="py-8">
      {/* Page Heading & Item Counter */}
      <div className="pt-2 pb-6 border-b border-stone-200 mb-8 flex flex-col sm:flex-row sm:items-baseline justify-between gap-4">
        <div className="flex items-baseline gap-3">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-stone-900">
            Giỏ hàng của bạn
          </h1>
          <span className="text-sm text-stone-500">
            ({itemCount} sản phẩm)
          </span>
        </div>

        {/* Clear Cart Action Button */}
        <div>
          {showClearConfirm ? (
            <div className="flex items-center gap-2 text-xs">
              <span className="text-rose-600 font-medium">Xóa tất cả?</span>
              <button
                type="button"
                onClick={handleClearCart}
                disabled={isClearing}
                className="bg-rose-600 text-white px-2.5 py-1 rounded font-medium hover:bg-rose-700 transition-colors cursor-pointer"
              >
                {isClearing ? "Đang xóa..." : "Xác nhận xóa"}
              </button>
              <button
                type="button"
                onClick={() => setShowClearConfirm(false)}
                disabled={isClearing}
                className="bg-stone-100 text-stone-700 px-2.5 py-1 rounded hover:bg-stone-200 transition-colors cursor-pointer"
              >
                Hủy
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setShowClearConfirm(true)}
              disabled={busyItemId !== null || isClearing}
              className="text-xs text-stone-500 hover:text-rose-600 transition-colors cursor-pointer"
            >
              Xóa toàn bộ giỏ hàng
            </button>
          )}
        </div>
      </div>

      {/* Mutation Error Notification (Inline) */}
      {mutationError && (
        <div className="mb-6 p-4 bg-rose-50 border border-rose-200 rounded-lg flex items-center justify-between text-xs text-rose-700">
          <span>✕ {mutationError}</span>
          <button
            type="button"
            onClick={() => setMutationError(null)}
            className="font-semibold underline hover:text-rose-900 cursor-pointer ml-3 shrink-0"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start mb-16">
        {/* ============================================================== */}
        {/* LEFT COLUMN: CART ITEMS LIST (~65%)                            */}
        {/* ============================================================== */}
        <div className="lg:col-span-8 space-y-4">
          {items.map((item) => {
            const itemId = item.id ?? item.productId;
            const isItemBusy = busyItemId === itemId || isClearing;

            return (
              <div
                key={itemId}
                className={`bg-white border border-stone-200 rounded-xl p-5 flex flex-col sm:flex-row gap-5 items-start sm:items-center justify-between transition-all ${
                  isItemBusy ? "opacity-50 pointer-events-none" : "opacity-100"
                }`}
              >
                {/* Thumbnail Bay */}
                <div className="w-20 h-20 bg-stone-50 border border-stone-100 rounded-lg p-1.5 flex items-center justify-center shrink-0 overflow-hidden">
                  {item.imageUrl ? (
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      className="w-full h-full object-cover rounded"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center text-center">
                      <svg
                        className="w-8 h-8 text-stone-300 stroke-[1.25]"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <rect x="2" y="3" width="20" height="14" rx="2" />
                        <line x1="8" y1="21" x2="16" y2="21" />
                        <line x1="12" y1="17" x2="12" y2="21" />
                      </svg>
                    </div>
                  )}
                </div>

                {/* Product Metadata Block */}
                <div className="flex-1 min-w-0">
                  <Link
                    href={`/products/${item.productId}`}
                    className="text-base font-semibold text-stone-900 hover:text-amber-600 transition-colors truncate block mb-1"
                  >
                    {item.productName}
                  </Link>

                  <div className="text-xs text-stone-400 mb-1">
                    Mã SP: {item.productSku || `TG-${item.productId}`}
                  </div>

                  <div className="text-sm font-medium text-stone-600">
                    {formatVndPrice(item.unitPrice)} <span className="text-xs text-stone-400 font-normal">/ sản phẩm</span>
                  </div>
                </div>

                {/* Quantity Stepper Control */}
                <div className="flex items-center border border-stone-200 rounded-lg bg-white h-9 shrink-0 overflow-hidden">
                  <button
                    type="button"
                    aria-label={`Giảm số lượng của ${item.productName}`}
                    onClick={() => handleUpdateQuantity(item, item.quantity - 1)}
                    disabled={isItemBusy || item.quantity <= 1}
                    className="w-9 h-full flex items-center justify-center text-base text-stone-600 hover:text-stone-900 hover:bg-stone-50 disabled:text-stone-300 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors"
                  >
                    −
                  </button>
                  <div className="w-10 h-full flex items-center justify-center text-sm font-semibold text-stone-900 border-x border-stone-200 select-none tabular-nums">
                    {item.quantity}
                  </div>
                  <button
                    type="button"
                    aria-label={`Tăng số lượng của ${item.productName}`}
                    onClick={() => handleUpdateQuantity(item, item.quantity + 1)}
                    disabled={isItemBusy || item.quantity >= 9999}
                    className="w-9 h-full flex items-center justify-center text-base text-stone-600 hover:text-stone-900 hover:bg-stone-50 disabled:text-stone-300 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors"
                  >
                    +
                  </button>
                </div>

                {/* Subtotal Block & Remove Action */}
                <div className="flex sm:flex-col items-center sm:items-end justify-between w-full sm:w-auto shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-stone-100 gap-1">
                  <div className="text-right">
                    <div className="text-[11px] text-stone-400 uppercase tracking-wider">
                      Thành tiền
                    </div>
                    <div className="text-base font-bold text-stone-900 tabular-nums">
                      {formatVndPrice(item.subtotal || item.unitPrice * item.quantity)}
                    </div>
                  </div>

                  <button
                    type="button"
                    aria-label={`Xóa ${item.productName} khỏi giỏ`}
                    onClick={() => handleRemoveItem(item)}
                    disabled={isItemBusy}
                    className="text-xs text-stone-400 hover:text-rose-600 hover:underline transition-colors cursor-pointer self-end mt-1"
                  >
                    Xóa
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* ============================================================== */}
        {/* RIGHT COLUMN: ORDER SUMMARY PANEL (~35%)                       */}
        {/* ============================================================== */}
        <div className="lg:col-span-4 sticky top-24">
          <div className="bg-white border border-stone-200 rounded-xl p-6 shadow-sm">
            <h2 className="text-lg font-bold text-stone-900 border-b border-stone-100 pb-4 mb-5">
              Tóm tắt đơn hàng
            </h2>

            {/* Line Items Breakdown */}
            <div className="space-y-3 font-sans">
              <div className="flex justify-between items-center text-sm">
                <span className="text-stone-600">Tạm tính</span>
                <span className="font-semibold text-stone-900 tabular-nums">
                  {formatVndPrice(totalPrice)}
                </span>
              </div>

              <div className="flex justify-between items-center text-sm">
                <span className="text-stone-600">Phí vận chuyển</span>
                <span className="text-xs font-semibold text-emerald-600 uppercase bg-emerald-50 px-2 py-0.5 rounded">
                  Miễn phí
                </span>
              </div>

              <div className="pt-3 border-t border-stone-100 flex justify-between items-baseline">
                <span className="text-base font-bold text-stone-900">
                  Tổng cộng
                </span>
                <span className="text-2xl font-extrabold text-amber-600 tabular-nums">
                  {formatVndPrice(totalPrice)}
                </span>
              </div>
            </div>

            {/* Truthful Context Notice */}
            <p className="text-xs text-stone-400 mt-4 mb-6 leading-relaxed">
              Địa chỉ nhận hàng và hình thức thanh toán sẽ được chọn ở bước tiếp theo.
            </p>

            {/* Primary Conversion CTA: Proceed to Checkout */}
            <Link
              href="/checkout"
              className="w-full h-12 rounded-lg bg-stone-900 hover:bg-stone-800 text-white font-semibold text-sm flex items-center justify-center transition-colors focus:ring-2 focus:ring-stone-900 shadow-sm"
            >
              Tiến hành thanh toán
            </Link>

            {/* Secondary Action: Continue Shopping */}
            <Link
              href="/shop"
              className="block text-center text-xs text-stone-500 hover:text-stone-900 transition-colors mt-4"
            >
              ← Tiếp tục mua sắm
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function CartPage() {
  return (
    <ProtectedRoute>
      <CustomerShell className="py-0">
        <CartContent />
      </CustomerShell>
    </ProtectedRoute>
  );
}
