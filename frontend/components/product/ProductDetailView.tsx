"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { formatVndPrice } from "@/components/product/ProductCard";
import { useAuth } from "@/features/auth/AuthContext";
import { getProductByIdApi } from "@/lib/api/catalog";
import { addToCartApi } from "@/lib/api/cart";
import { ApiError } from "@/lib/api/client";
import { ProductDto } from "@/types/catalog";

interface ProductDetailViewProps {
  id: string;
}

export function ProductDetailView({ id }: ProductDetailViewProps) {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  const [product, setProduct] = useState<ProductDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [notFound, setNotFound] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [quantity, setQuantity] = useState<number>(1);
  const [isAdding, setIsAdding] = useState<boolean>(false);
  const [addSuccess, setAddSuccess] = useState<boolean>(false);
  const [addError, setAddError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setNotFound(false);
    setError(null);

    getProductByIdApi(id)
      .then((data) => {
        if (!isMounted) return;
        if (!data || !data.id) {
          setNotFound(true);
        } else {
          setProduct(data);
          setQuantity(data.stockQuantity > 0 ? 1 : 0);
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        if (err instanceof ApiError && (err.status === 404 || err.status === 400)) {
          setNotFound(true);
        } else {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to retrieve equipment details."
          );
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [id]);

  const handleDecrement = () => {
    if (quantity > 1) {
      setQuantity((q) => q - 1);
    }
  };

  const handleIncrement = () => {
    if (product && quantity < product.stockQuantity) {
      setQuantity((q) => q + 1);
    }
  };

  const handleAddToCart = async () => {
    if (!product || product.stockQuantity <= 0 || isAdding) return;

    if (!isAuthenticated) {
      router.push(`/login?redirect=/products/${product.id}`);
      return;
    }

    setIsAdding(true);
    setAddSuccess(false);
    setAddError(null);

    try {
      await addToCartApi({
        productId: product.id,
        quantity,
      });
      setAddSuccess(true);
    } catch (err: unknown) {
      const message =
        err instanceof ApiError
          ? err.message
          : err instanceof Error
          ? err.message
          : "Unable to add this item to cart.";
      setAddError(message);
    } finally {
      setIsAdding(false);
    }
  };

  // =========================================================================
  // 1. LOADING STATE SKELETON
  // =========================================================================
  if (isLoading) {
    return (
      <CustomerShell className="py-6">
        <div className="h-4 w-36 bg-[#E7E5E4] rounded mb-8 animate-pulse" />
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-14 items-start">
          <div className="lg:col-span-7 aspect-[4/3] bg-[#F5F5F4] rounded-[6px] border border-[#E7E5E4] animate-pulse" />
          <div className="lg:col-span-5 space-y-4 animate-pulse">
            <div className="h-4 w-28 bg-[#E7E5E4] rounded" />
            <div className="h-9 w-4/5 bg-[#E7E5E4] rounded" />
            <div className="h-4 w-32 bg-[#E7E5E4] rounded" />
            <div className="h-8 w-40 bg-[#E7E5E4] rounded my-4" />
            <div className="h-24 w-full bg-[#E7E5E4] rounded" />
            <div className="h-11 w-full bg-[#E7E5E4] rounded" />
          </div>
        </div>
      </CustomerShell>
    );
  }

  // =========================================================================
  // 2. NOT FOUND STATE (404)
  // =========================================================================
  if (notFound) {
    return (
      <CustomerShell className="py-8">
        <div className="py-20 text-center border border-stone-200 rounded-xl bg-white my-8 p-8">
          <h2 className="text-2xl font-bold text-stone-900 tracking-tight mb-3">
            Không tìm thấy sản phẩm
          </h2>
          <p className="text-sm text-stone-500 max-w-md mx-auto mb-6 leading-relaxed">
            Sản phẩm bạn đang tìm kiếm (ID: #{id}) không tồn tại hoặc đã ngừng kinh doanh.
          </p>
          <Link
            href="/shop"
            className="inline-flex h-10 px-6 rounded-lg bg-stone-900 text-white text-xs font-semibold items-center justify-center hover:bg-stone-800 transition-colors"
          >
            ← Quay lại cửa hàng
          </Link>
        </div>
      </CustomerShell>
    );
  }

  // =========================================================================
  // 3. SERVICE ERROR STATE
  // =========================================================================
  if (error || !product) {
    return (
      <CustomerShell className="py-8">
        <div className="p-8 border border-rose-200 bg-rose-50 rounded-xl my-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="text-xs font-semibold text-rose-700 uppercase mb-1">
              Lỗi kết nối máy chủ
            </div>
            <p className="text-sm text-rose-800">
              {error || "Không thể tải thông tin sản phẩm."}
            </p>
          </div>
          <Link
            href="/shop"
            className="h-9 px-4 rounded-lg bg-stone-900 text-white text-xs font-semibold hover:bg-stone-800 transition-colors self-start sm:self-auto inline-flex items-center"
          >
            Quay lại cửa hàng
          </Link>
        </div>
      </CustomerShell>
    );
  }

  // =========================================================================
  // 4. MAIN PRODUCT DETAIL (55/45 DESKTOP SPLIT)
  // =========================================================================
  const isAvailable = product.stockQuantity > 0 && product.active;

  return (
    <CustomerShell className="py-6">
      {/* Back to Shop Navigation Link */}
      <div className="mb-6">
        <Link
          href="/shop"
          className="text-xs font-medium text-stone-500 hover:text-stone-900 transition-colors inline-flex items-center gap-1.5"
        >
          <span>←</span>
          <span>Quay lại danh mục sản phẩm</span>
        </Link>
      </div>

      {/* Main Product Hero (55% Visual / 45% Information Split) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-start mb-16">
        {/* ============================================================== */}
        {/* LEFT COLUMN: PRODUCT VISUAL (55%)                              */}
        {/* ============================================================== */}
        <div className="lg:col-span-7">
          <div className="relative aspect-[4/3] w-full bg-stone-50 border border-stone-200 rounded-2xl p-6 md:p-10 flex items-center justify-center overflow-hidden">
            {/* Product Image */}
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.name}
                className="max-h-[380px] w-full object-contain drop-shadow-sm hover:scale-105 transition-transform duration-300"
              />
            ) : (
              <div className="flex flex-col items-center justify-center text-center p-8 text-stone-400">
                <svg
                  className="w-16 h-16 mb-3 stroke-[1.25]"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <rect x="2" y="3" width="20" height="14" rx="2" />
                  <line x1="8" y1="21" x2="16" y2="21" />
                  <line x1="12" y1="17" x2="12" y2="21" />
                </svg>
                <div className="text-xs font-semibold text-stone-700 uppercase mb-1">
                  TECHGEAR
                </div>
              </div>
            )}
          </div>
        </div>

        {/* ============================================================== */}
        {/* RIGHT COLUMN: PRODUCT INFORMATION (45%)                       */}
        {/* ============================================================== */}
        <div className="lg:col-span-5 flex flex-col">
          {/* Category Eyebrow */}
          <div className="mb-2">
            {product.category ? (
              <Link
                href={`/category/${product.category.id}`}
                className="text-xs font-semibold text-amber-600 hover:text-amber-700 transition-colors"
              >
                {product.category.name}
              </Link>
            ) : (
              <span className="text-xs font-semibold text-amber-600">
                Thiết bị công nghệ
              </span>
            )}
          </div>

          {/* Product Name */}
          <h1 className="text-2xl sm:text-3xl font-extrabold text-stone-900 tracking-tight leading-snug mb-3">
            {product.name}
          </h1>

          {/* Price & Stock Availability Bar */}
          <div className="flex items-baseline justify-between gap-4 mb-5 pt-1">
            <div className="text-2xl lg:text-3xl font-bold text-stone-900">
              {formatVndPrice(product.price)}
            </div>

            <div className="flex items-center gap-1.5 text-xs">
              {isAvailable ? (
                <span className="inline-flex items-center gap-1.5 text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  Còn hàng ({product.stockQuantity} sản phẩm)
                </span>
              ) : (
                <span className="inline-flex items-center gap-1.5 text-rose-700 bg-rose-50 px-2.5 py-1 rounded-full border border-rose-200 font-medium">
                  <span className="w-2 h-2 rounded-full bg-rose-500" />
                  Hết hàng
                </span>
              )}
            </div>
          </div>

          {/* Product Description */}
          <div className="py-5 border-y border-stone-200 text-sm text-stone-600 leading-relaxed mb-6">
            {product.description ||
              "Sản phẩm công nghệ chính hãng được tuyển chọn và bảo hành theo tiêu chuẩn nhà sản xuất."}
          </div>

          {/* Quantity Stepper & Add to Cart Controls */}
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
              {/* Rectangular Quantity Stepper [ - ] [ 1 ] [ + ] */}
              <div className="flex items-center border border-stone-200 rounded-lg bg-white h-11 w-fit">
                <button
                  type="button"
                  aria-label="Giảm số lượng"
                  onClick={handleDecrement}
                  disabled={!isAvailable || quantity <= 1}
                  className="w-11 h-full flex items-center justify-center text-stone-800 hover:bg-stone-100 disabled:text-stone-300 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors"
                >
                  −
                </button>
                <div className="w-12 h-full flex items-center justify-center text-sm font-bold text-stone-900 border-x border-stone-200 select-none">
                  {quantity}
                </div>
                <button
                  type="button"
                  aria-label="Tăng số lượng"
                  onClick={handleIncrement}
                  disabled={!isAvailable || quantity >= product.stockQuantity}
                  className="w-11 h-full flex items-center justify-center text-stone-800 hover:bg-stone-100 disabled:text-stone-300 disabled:hover:bg-white disabled:cursor-not-allowed transition-colors"
                >
                  +
                </button>
              </div>

              {/* Add to Cart CTA */}
              <button
                type="button"
                onClick={handleAddToCart}
                disabled={!isAvailable || isAdding}
                className={`flex-1 h-11 px-6 rounded-lg text-sm font-semibold transition-colors inline-flex items-center justify-center ${
                  isAvailable
                    ? "bg-stone-900 text-white hover:bg-stone-800 cursor-pointer shadow-xs"
                    : "bg-stone-100 text-stone-400 border border-stone-200 cursor-not-allowed"
                }`}
              >
                {isAdding ? (
                  <span>ĐANG THÊM VÀO GIỎ...</span>
                ) : !isAvailable ? (
                  <span>HẾT HÀNG</span>
                ) : (
                  <span>THÊM VÀO GIỎ HÀNG</span>
                )}
              </button>
            </div>

            {/* In-situ Success Feedback */}
            {addSuccess && (
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg flex items-center justify-between text-xs text-emerald-800">
                <span>✓ Đã thêm {quantity} sản phẩm vào giỏ hàng thành công.</span>
                <Link
                  href="/cart"
                  className="underline hover:text-emerald-900 font-semibold"
                >
                  Xem giỏ hàng →
                </Link>
              </div>
            )}

            {/* In-situ Error Feedback */}
            {addError && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center justify-between text-xs text-rose-800">
                <span>✕ {addError}</span>
                <button
                  type="button"
                  onClick={handleAddToCart}
                  className="underline hover:text-rose-950 font-semibold cursor-pointer"
                >
                  Thử lại
                </button>
              </div>
            )}

            {/* Retail Trust Points */}
            <div className="pt-4 space-y-2 text-xs text-stone-500">
              <div className="flex items-center gap-2">
                <span className="text-emerald-600">✓</span> Cam kết 100% chính hãng nguyên seal
              </div>
              <div className="flex items-center gap-2">
                <span className="text-emerald-600">✓</span> Đổi mới trong 7 ngày nếu lỗi từ nhà sản xuất
              </div>
              <div className="flex items-center gap-2">
                <span className="text-emerald-600">✓</span> Hỗ trợ bảo hành nhanh chóng, tận tâm
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ================================================================ */}
      {/* 5. SUPPORTED CATALOG METADATA                                    */}
      {/* ================================================================ */}
      <section className="pt-8 pb-12 border-t border-stone-200">
        <h2 className="text-base font-bold text-stone-900 mb-4">
          Thông tin chi tiết sản phẩm
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 bg-white border border-stone-200 rounded-xl p-5">
          <div>
            <div className="text-xs text-stone-500 mb-1">
              Mã sản phẩm (SKU)
            </div>
            <div className="text-sm font-semibold text-stone-900">
              {product.sku || `#${product.id}`}
            </div>
          </div>

          <div>
            <div className="text-xs text-stone-500 mb-1">
              Danh mục
            </div>
            <div className="text-sm font-semibold text-stone-900">
              {product.category?.name || "Thiết bị công nghệ"}
            </div>
          </div>

          <div>
            <div className="text-xs text-stone-500 mb-1">
              Tồn kho hiện có
            </div>
            <div className="text-sm font-semibold text-stone-900">
              {product.stockQuantity} sản phẩm
            </div>
          </div>

          <div>
            <div className="text-xs text-stone-500 mb-1">
              Trạng thái kinh doanh
            </div>
            <div className="text-sm font-semibold text-stone-900">
              {product.active ? "Đang kinh doanh" : "Tạm ngừng"}
            </div>
          </div>
        </div>
      </section>
    </CustomerShell>
  );
}
