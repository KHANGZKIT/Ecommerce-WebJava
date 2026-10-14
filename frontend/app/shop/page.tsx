"use client";

import React, { Suspense, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
import { Breadcrumbs } from "@/components/ui/Breadcrumbs";
import { EmptyState, ErrorState, ProductGridSkeleton } from "@/components/ui/FeedbackStates";
import { getCategoriesApi, getProductsApi } from "@/lib/api/catalog";
import { CategoryDto, Page, ProductDto } from "@/types/catalog";

// Approved 6 Canonical Categories (FE-01 / FE-03)
const CANONICAL_CATEGORIES = [
  { id: 1, name: "Audio" },
  { id: 2, name: "Keyboards & Mouse" },
  { id: 3, name: "Gaming" },
  { id: 4, name: "Accessories" },
  { id: 5, name: "Monitors" },
  { id: 6, name: "Laptop Accessories" },
];

const VALID_SORTS = [
  { value: "createdAt,desc", label: "Mới nhất" },
  { value: "name,asc", label: "Tên: A–Z" },
  { value: "price,asc", label: "Giá: Thấp đến cao" },
  { value: "price,desc", label: "Giá: Cao đến thấp" },
] as const;

type SortValue = (typeof VALID_SORTS)[number]["value"];

function ShopContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Read search params with strict validation
  const pageParam = parseInt(searchParams.get("page") || "0", 10);
  const page = isNaN(pageParam) || pageParam < 0 ? 0 : pageParam;

  const rawSort = searchParams.get("sort") || "createdAt,desc";
  const sort: SortValue = VALID_SORTS.some((s) => s.value === rawSort)
    ? (rawSort as SortValue)
    : "createdAt,desc";

  const size = 20;

  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [pageData, setPageData] = useState<Page<ProductDto>>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    size,
    number: 0,
    first: true,
    last: true,
    empty: true,
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Load Categories for the Navigation Rail
  useEffect(() => {
    let isMounted = true;
    getCategoriesApi()
      .then((cats) => {
        if (isMounted && Array.isArray(cats) && cats.length > 0) {
          setCategories(cats);
        }
      })
      .catch(() => {
        // Fall back gracefully to canonical category descriptors
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch Products based on URL query state
  const loadProducts = useCallback(
    async (targetPage: number, targetSort: string) => {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getProductsApi({
          page: targetPage,
          size,
          sort: targetSort,
        });
        setPageData(data);
      } catch (err: unknown) {
        const msg =
          err instanceof Error
            ? err.message
            : "Unable to retrieve the equipment catalog.";
        setError(msg);
      } finally {
        setIsLoading(false);
      }
    },
    [size]
  );

  useEffect(() => {
    loadProducts(page, sort);
  }, [page, sort, loadProducts]);

  // Handle Sort Change (pushes new URL and resets page to 0)
  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSort = e.target.value;
    const params = new URLSearchParams(searchParams.toString());
    params.set("sort", newSort);
    params.set("page", "0");
    router.push(`/shop?${params.toString()}`);
  };

  // Handle Page Change
  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(newPage));
    router.push(`/shop?${params.toString()}`);
  };

  // Merge loaded categories with canonical fallback
  const displayCategories =
    categories.length > 0
      ? categories
      : CANONICAL_CATEGORIES.map((c) => ({
          id: c.id,
          name: c.name,
          description: "",
          active: true,
          createdAt: "",
        }));

  // Result Counter Calculation (based strictly on API Page metadata)
  const totalElements = pageData.totalElements;
  const startItem = totalElements === 0 ? 0 : pageData.number * size + 1;
  const endItem = Math.min((pageData.number + 1) * size, totalElements);
  const totalPages = pageData.totalPages;

  return (
    <CustomerShell className="py-0">
      {/* 1. BREADCRUMBS */}
      <div className="pt-4">
        <Breadcrumbs items={[{ label: "Tất cả sản phẩm", href: "/shop", isCurrent: true }]} />
      </div>

      {/* 2. SHOP INTRO / PAGE HEADING */}
      <section className="pt-2 pb-6 border-b border-[#E7E5E4]">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#1C1917] mb-2">
          Tất cả sản phẩm
        </h1>
        <p className="text-sm text-[#78716C] leading-relaxed max-w-2xl">
          Khám phá trọn bộ thiết bị công nghệ chính hãng: laptop, điện thoại thông minh, bàn phím cơ, màn hình và phụ kiện cao cấp.
        </p>
      </section>

      {/* ================================================================ */}
      {/* 3. CATEGORY NAVIGATION (Segmented Rail)                          */}
      {/* ================================================================ */}
      <section className="py-4 border-b border-[#E7E5E4] overflow-x-auto hide-scrollbar">
        <div className="flex items-center gap-2 min-w-max">
          <Link
            href="/shop"
            className="bg-[#1C1917] text-white px-3.5 py-1.5 text-xs font-semibold rounded-full whitespace-nowrap transition-colors shadow-xs"
          >
            Tất cả ({totalElements})
          </Link>

          {displayCategories.map((cat) => (
            <Link
              key={cat.id}
              href={`/category/${cat.id}`}
              className="bg-white text-[#78716C] border border-[#E7E5E4] hover:bg-[#FAFAF9] hover:text-[#1C1917] hover:border-[#1C1917]/40 px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-all shadow-xs"
            >
              {cat.name}
            </Link>
          ))}
        </div>
      </section>

      {/* ================================================================ */}
      {/* 4. CATALOG TOOLBAR                                               */}
      {/* ================================================================ */}
      <section className="py-4 flex flex-col sm:flex-row sm:items-center justify-between border-b border-[#E7E5E4] mb-8 gap-3">
        {/* Left: Product Counter */}
        <div className="text-xs font-medium text-[#78716C]">
          {isLoading ? (
            <span>Đang tải danh sách sản phẩm...</span>
          ) : totalElements > 0 ? (
            <span>
              Hiển thị <span className="font-semibold text-[#1C1917]">{startItem}–{endItem}</span> trên{" "}
              <span className="font-semibold text-[#1C1917]">{totalElements}</span> sản phẩm
            </span>
          ) : (
            <span>0 sản phẩm</span>
          )}
        </div>

        {/* Right: Sort Selector */}
        <div className="flex items-center gap-2">
          <label
            htmlFor="shop-sort"
            className="text-xs font-medium text-[#78716C]"
          >
            Sắp xếp:
          </label>
          <select
            id="shop-sort"
            value={sort}
            onChange={handleSortChange}
            className="h-9 px-3 bg-white border border-[#E7E5E4] rounded-lg text-xs text-[#1C1917] font-semibold hover:border-[#1C1917]/40 focus:border-[#1C1917] focus:ring-2 focus:ring-[#1C1917]/10 focus:outline-none cursor-pointer transition-all shadow-xs"
          >
            {VALID_SORTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </div>
      </section>

      {/* ================================================================ */}
      {/* 5. PRODUCT GRID / STATES                                         */}
      {/* ================================================================ */}
      {isLoading ? (
        <div className="mb-12">
          <ProductGridSkeleton count={8} />
        </div>
      ) : error ? (
        <div className="my-8">
          <ErrorState
            title="Không thể tải dữ liệu sản phẩm"
            message={error}
            onRetry={() => loadProducts(page, sort)}
          />
        </div>
      ) : pageData.content.length === 0 ? (
        <div className="my-8">
          <EmptyState
            title="Không tìm thấy sản phẩm nào"
            message="Hiện chưa có sản phẩm nào trong danh mục này hoặc hệ thống đang cập nhật."
            actionLabel="Làm mới danh sách"
            onAction={() => loadProducts(0, sort)}
          />
        </div>
      ) : (
        /* 4-Column Responsive Retail Grid */
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6 mb-12">
          {pageData.content.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* ================================================================ */}
      {/* 6. PAGINATION BAR                                                */}
      {/* ================================================================ */}
      {!isLoading && !error && totalElements > 0 && (
        <section className="py-8 flex flex-col sm:flex-row items-center justify-between border-t border-stone-200 mt-8 gap-4">
          {/* Left: Page Range Indicator */}
          <div className="text-xs text-stone-500">
            Trang {page + 1} / {Math.max(totalPages, 1)} ({size} sản phẩm/trang)
          </div>

          {/* Right: Pagination Controls */}
          <div className="flex items-center gap-1.5 text-xs">
            {/* Previous Button */}
            <button
              onClick={() => handlePageChange(page - 1)}
              disabled={page === 0 || pageData.first}
              className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                page === 0 || pageData.first
                  ? "bg-stone-100 text-stone-400 border-stone-200 cursor-not-allowed"
                  : "bg-white text-stone-800 border-stone-200 hover:border-stone-400 cursor-pointer"
              }`}
            >
              ← Trước
            </button>

            {/* Page Number Buttons */}
            {Array.from({ length: totalPages }).map((_, idx) => {
              const isCurrent = idx === page;
              return (
                <button
                  key={idx}
                  onClick={() => handlePageChange(idx)}
                  className={`w-8 h-8 rounded-lg font-medium transition-colors cursor-pointer flex items-center justify-center ${
                    isCurrent
                      ? "bg-stone-900 text-white"
                      : "bg-white text-stone-700 border border-stone-200 hover:border-stone-400"
                  }`}
                >
                  {idx + 1}
                </button>
              );
            })}

            {/* Next Button */}
            <button
              onClick={() => handlePageChange(page + 1)}
              disabled={page >= totalPages - 1 || pageData.last}
              className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                page >= totalPages - 1 || pageData.last
                  ? "bg-stone-100 text-stone-400 border-stone-200 cursor-not-allowed"
                  : "bg-white text-stone-800 border-stone-200 hover:border-stone-400 cursor-pointer"
              }`}
            >
              Sau →
            </button>
          </div>
        </section>
      )}
    </CustomerShell>
  );
}

export default function ShopPage() {
  return (
    <Suspense
      fallback={
        <CustomerShell className="py-0">
          <div className="pt-10 pb-8 border-b border-[#E7E5E4] animate-pulse">
            <div className="h-3 w-32 bg-[#E7E5E4] rounded mb-2" />
            <div className="h-8 w-64 bg-[#E7E5E4] rounded mb-3" />
            <div className="h-4 w-96 bg-[#E7E5E4] rounded" />
          </div>
        </CustomerShell>
      }
    >
      <ShopContent />
    </Suspense>
  );
}
