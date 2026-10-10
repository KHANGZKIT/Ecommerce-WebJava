"use client";

import React, { Suspense, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
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
      {/* ================================================================ */}
      {/* 2. SHOP INTRO / PAGE HEADING                                     */}
      {/* ================================================================ */}
      {/* ================================================================ */}
      {/* 2. SHOP INTRO / PAGE HEADING                                     */}
      {/* ================================================================ */}
      <section className="pt-8 pb-6 border-b border-stone-200">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-stone-900 mb-2">
          Tất cả sản phẩm
        </h1>
        <p className="text-sm text-stone-600 leading-relaxed max-w-2xl">
          Khám phá trọn bộ thiết bị công nghệ chính hãng: bàn phím cơ, chuột, tai nghe, màn hình và phụ kiện cao cấp.
        </p>
      </section>

      {/* ================================================================ */}
      {/* 3. CATEGORY NAVIGATION (Segmented Rail)                          */}
      {/* ================================================================ */}
      <section className="py-4 border-b border-stone-200 overflow-x-auto scrollbar-none">
        <div className="flex items-center gap-2 min-w-max">
          <Link
            href="/shop"
            className="bg-stone-900 text-white px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-colors"
          >
            Tất cả ({totalElements})
          </Link>

          {displayCategories.map((cat) => (
            <Link
              key={cat.id}
              href={`/category/${cat.id}`}
              className="bg-stone-50 text-stone-700 border border-stone-200 hover:bg-stone-100 hover:text-stone-900 px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-colors"
            >
              {cat.name}
            </Link>
          ))}
        </div>
      </section>

      {/* ================================================================ */}
      {/* 4. CATALOG TOOLBAR                                               */}
      {/* ================================================================ */}
      <section className="py-4 flex flex-col sm:flex-row sm:items-center justify-between border-b border-stone-200 mb-8 gap-3">
        {/* Left: Product Counter */}
        <div className="text-xs font-medium text-stone-500">
          {isLoading ? (
            <span>Đang tải danh sách sản phẩm...</span>
          ) : totalElements > 0 ? (
            <span>
              Hiển thị {startItem}–{endItem} trên {totalElements} sản phẩm
            </span>
          ) : (
            <span>0 sản phẩm</span>
          )}
        </div>

        {/* Right: Sort Selector */}
        <div className="flex items-center gap-2">
          <label
            htmlFor="shop-sort"
            className="text-xs font-medium text-stone-600"
          >
            Sắp xếp:
          </label>
          <select
            id="shop-sort"
            value={sort}
            onChange={handleSortChange}
            className="h-9 px-3 bg-white border border-stone-200 rounded-lg text-xs text-stone-800 font-medium focus:border-stone-900 focus:outline-none cursor-pointer"
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
        /* Loading Skeleton (8 cards matching 4-column desktop grid) */
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6 mb-12">
          {Array.from({ length: 8 }).map((_, idx) => (
            <div
              key={idx}
              className="bg-white border border-stone-200 rounded-xl p-4 flex flex-col animate-pulse"
            >
              <div className="aspect-square bg-stone-100 rounded-lg mb-3" />
              <div className="h-3 w-16 bg-stone-200 rounded mb-2" />
              <div className="h-4 w-3/4 bg-stone-200 rounded mb-4" />
              <div className="mt-auto pt-3 border-t border-stone-100 flex justify-between">
                <div className="h-4 w-20 bg-stone-200 rounded" />
                <div className="h-3 w-14 bg-stone-200 rounded" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        /* Error State Banner */
        <div className="p-8 border border-rose-200 bg-rose-50 rounded-xl my-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="text-xs font-semibold text-rose-700 uppercase mb-1">
              Không thể tải dữ liệu sản phẩm
            </div>
            <p className="text-sm text-rose-800">{error}</p>
          </div>
          <button
            onClick={() => loadProducts(page, sort)}
            className="h-9 px-4 rounded-lg bg-stone-900 text-white text-xs font-medium hover:bg-stone-800 transition-colors self-start sm:self-auto cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      ) : pageData.content.length === 0 ? (
        /* Empty State */
        <div className="py-16 text-center border border-stone-200 rounded-xl bg-white my-8 p-6">
          <h3 className="text-lg font-bold text-stone-900 mb-2">
            Không tìm thấy sản phẩm nào
          </h3>
          <p className="text-sm text-stone-500 max-w-md mx-auto mb-6">
            Hiện chưa có sản phẩm nào trong danh mục này hoặc hệ thống đang cập nhật.
          </p>
          <button
            onClick={() => loadProducts(0, sort)}
            className="inline-flex h-10 px-5 rounded-lg bg-stone-900 text-white text-xs font-medium items-center justify-center hover:bg-stone-800 transition-colors cursor-pointer"
          >
            Làm mới danh sách
          </button>
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
