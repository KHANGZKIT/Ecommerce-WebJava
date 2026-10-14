"use client";

import React, { use, Suspense, useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useSearchParams, useRouter } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
import { Breadcrumbs } from "@/components/ui/Breadcrumbs";
import { EmptyState, ErrorState, ProductGridSkeleton } from "@/components/ui/FeedbackStates";
import {
  getCategoriesApi,
  getCategoryByIdApi,
  getProductsByCategoryApi,
} from "@/lib/api/catalog";
import { ApiError } from "@/lib/api/client";
import { CategoryDto, Page, ProductDto } from "@/types/catalog";

const VALID_SORTS = [
  { value: "createdAt,desc", label: "Mới nhất" },
  { value: "price,asc", label: "Giá: Thấp đến cao" },
  { value: "price,desc", label: "Giá: Cao đến thấp" },
  { value: "name,asc", label: "Tên: A–Z" },
] as const;

type SortValue = (typeof VALID_SORTS)[number]["value"];

function CategoryLoadingSkeleton() {
  return (
    <div className="py-0">
      {/* Skeleton Header */}
      <section className="pt-10 pb-8 border-b border-[#E7E5E4]">
        <div className="h-3 w-48 bg-[#E7E5E4] rounded-[2px] mb-3 animate-pulse" />
        <div className="h-9 w-64 bg-[#E7E5E4] rounded-[3px] mb-3 animate-pulse" />
        <div className="h-5 w-96 bg-[#F5F5F4] rounded-[3px] animate-pulse" />
      </section>

      {/* Skeleton Category Rail */}
      <section className="py-4 border-b border-[#E7E5E4] overflow-x-auto scrollbar-none">
        <div className="flex items-center gap-2 min-w-max animate-pulse">
          <div className="h-8 w-28 bg-[#E7E5E4] rounded-[4px]" />
          <div className="h-8 w-20 bg-[#F5F5F4] rounded-[4px]" />
          <div className="h-8 w-32 bg-[#F5F5F4] rounded-[4px]" />
          <div className="h-8 w-24 bg-[#F5F5F4] rounded-[4px]" />
        </div>
      </section>

      {/* Skeleton Toolbar */}
      <section className="py-4 border-b border-[#E7E5E4] mb-8 flex justify-between items-center">
        <div className="h-3 w-36 bg-[#E7E5E4] rounded animate-pulse" />
        <div className="h-9 w-40 bg-[#E7E5E4] rounded-[4px] animate-pulse" />
      </section>

      {/* Skeleton Product Grid (8 cards) */}
      <div className="mb-12">
        <ProductGridSkeleton count={8} />
      </div>
    </div>
  );
}

function CategoryContent({
  params,
}: {
  params: Promise<{ categoryId: string }>;
}) {
  const { categoryId: rawCatId } = use(params);
  const router = useRouter();
  const searchParams = useSearchParams();

  const numericCatId = Number(rawCatId);
  const isValidCatId =
    !isNaN(numericCatId) && numericCatId > 0 && Number.isInteger(numericCatId);

  // Read search params with strict validation
  const pageParam = parseInt(searchParams.get("page") || "0", 10);
  const page = isNaN(pageParam) || pageParam < 0 ? 0 : pageParam;

  const rawSort = searchParams.get("sort") || "createdAt,desc";
  const sort: SortValue = VALID_SORTS.some((s) => s.value === rawSort)
    ? (rawSort as SortValue)
    : "createdAt,desc";

  const size = 20;

  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [category, setCategory] = useState<CategoryDto | null>(null);
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
  const [isNotFound, setIsNotFound] = useState<boolean>(false);
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
        // Navigation rail falls back to showing All Equipment if category list fails
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Fetch Category Details and Products
  const loadData = useCallback(
    async (catId: number, targetPage: number, targetSort: string) => {
      setIsLoading(true);
      setError(null);
      setIsNotFound(false);

      try {
        const [catRes, prodRes] = await Promise.all([
          getCategoryByIdApi(catId),
          getProductsByCategoryApi(catId, {
            page: targetPage,
            size,
            sort: targetSort,
          }),
        ]);

        setCategory(catRes);
        setPageData(prodRes);
      } catch (err: unknown) {
        if (err instanceof ApiError && err.status === 404) {
          setIsNotFound(true);
        } else {
          const msg =
            err instanceof ApiError
              ? err.message
              : err instanceof Error
              ? err.message
              : "Unable to retrieve the category equipment catalog.";
          setError(msg);
        }
      } finally {
        setIsLoading(false);
      }
    },
    [size]
  );

  useEffect(() => {
    if (!isValidCatId) {
      setIsNotFound(true);
      setIsLoading(false);
      return;
    }
    loadData(numericCatId, page, sort);
  }, [isValidCatId, numericCatId, page, sort, loadData]);

  // Handle Sort Change (updates URL query and resets page to 0)
  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSort = e.target.value;
    const currentParams = new URLSearchParams(searchParams.toString());
    currentParams.set("sort", newSort);
    currentParams.set("page", "0");
    router.push(`/category/${numericCatId}?${currentParams.toString()}`);
  };

  // Handle Page Change
  const handlePageChange = (newPage: number) => {
    const currentParams = new URLSearchParams(searchParams.toString());
    currentParams.set("page", String(newPage));
    router.push(`/category/${numericCatId}?${currentParams.toString()}`);
  };

  // State 4: Category Not Found (404 or invalid ID)
  if (isNotFound) {
    return (
      <div className="py-0">
        <div className="pt-4">
          <Breadcrumbs
            items={[
              { label: "Cửa hàng", href: "/shop" },
              { label: "Không tìm thấy danh mục", isCurrent: true },
            ]}
          />
        </div>

        {/* Category Navigation Rail */}
        <section className="py-4 border-b border-[#E7E5E4] overflow-x-auto hide-scrollbar">
          <div className="flex items-center gap-2 min-w-max">
            <Link
              href="/shop"
              className="bg-white text-[#78716C] border border-[#E7E5E4] hover:bg-[#FAFAF9] hover:text-[#1C1917] px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-colors"
            >
              Tất cả sản phẩm
            </Link>

            {categories.map((cat) => (
              <Link
                key={cat.id}
                href={`/category/${cat.id}`}
                className="bg-white text-[#78716C] border border-[#E7E5E4] hover:bg-[#FAFAF9] hover:text-[#1C1917] px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-colors"
              >
                {cat.name}
              </Link>
            ))}
          </div>
        </section>

        <div className="my-12">
          <EmptyState
            title="Không tìm thấy danh mục"
            message="Danh mục sản phẩm bạn yêu cầu không tồn tại hoặc đã ngừng kinh doanh."
            actionLabel="Quay lại cửa hàng"
            onAction={() => router.push("/shop")}
          />
        </div>
      </div>
    );
  }

  // State 5: API Error State
  if (error && !isLoading) {
    return (
      <div className="py-0">
        <div className="pt-4">
          <Breadcrumbs
            items={[
              { label: "Cửa hàng", href: "/shop" },
              { label: "Lỗi tải dữ liệu", isCurrent: true },
            ]}
          />
        </div>

        <div className="my-12">
          <ErrorState
            title="Lỗi tải dữ liệu danh mục"
            message={error}
            onRetry={() => loadData(numericCatId, page, sort)}
          />
        </div>
      </div>
    );
  }

  // Result Counter Calculation (based strictly on API Page metadata)
  const totalElements = pageData.totalElements;
  const startItem = totalElements === 0 ? 0 : pageData.number * size + 1;
  const endItem = Math.min((pageData.number + 1) * size, totalElements);
  const totalPages = pageData.totalPages;

  return (
    <div className="py-0">
      {/* ================================================================ */}
      {/* 1. BREADCRUMBS & CATEGORY EDITORIAL HEADER                       */}
      {/* ================================================================ */}
      <section className="pt-4 pb-6 border-b border-[#E7E5E4]">
        {/* Breadcrumb Navigation */}
        <Breadcrumbs
          items={[
            { label: "Cửa hàng", href: "/shop" },
            { label: category?.name || "Danh mục", isCurrent: true },
          ]}
        />

        {/* Category Heading & Description */}
        {isLoading ? (
          <div className="pt-2">
            <div className="h-8 w-64 bg-[#E7E5E4] rounded-lg mb-2 animate-pulse" />
            <div className="h-4 w-96 bg-[#F5F5F4] rounded animate-pulse" />
          </div>
        ) : (
          <div className="pt-2">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#1C1917] mb-2">
              {category?.name}
            </h1>
            {category?.description && category.description.trim() && (
              <p className="text-sm text-[#78716C] leading-relaxed max-w-2xl">
                {category.description}
              </p>
            )}
          </div>
        )}
      </section>

      {/* ================================================================ */}
      {/* 2. CATEGORY NAVIGATION (Segmented Rail)                          */}
      {/* ================================================================ */}
      <section className="py-4 border-b border-[#E7E5E4] overflow-x-auto hide-scrollbar">
        <div className="flex items-center gap-2 min-w-max">
          <Link
            href="/shop"
            className="bg-white text-[#78716C] border border-[#E7E5E4] hover:bg-[#FAFAF9] hover:text-[#1C1917] hover:border-[#1C1917]/40 px-3.5 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-all shadow-xs"
          >
            Tất cả sản phẩm
          </Link>

          {categories.map((cat) => {
            const isActive = cat.id === numericCatId;
            return (
              <Link
                key={cat.id}
                href={`/category/${cat.id}`}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-full whitespace-nowrap transition-all shadow-xs ${
                  isActive
                    ? "bg-[#1C1917] text-white"
                    : "bg-white text-[#78716C] border border-[#E7E5E4] hover:bg-[#FAFAF9] hover:text-[#1C1917] hover:border-[#1C1917]/40"
                }`}
              >
                {cat.name}
              </Link>
            );
          })}
        </div>
      </section>

      {/* ================================================================ */}
      {/* 3. CATALOG TOOLBAR                                               */}
      {/* ================================================================ */}
      <section className="py-4 flex flex-col sm:flex-row sm:items-center justify-between border-b border-[#E7E5E4] mb-8 gap-3">
        {/* Left: Product Counter */}
        <div className="text-xs font-medium text-[#78716C]">
          {isLoading ? (
            <span>Đang tải danh mục...</span>
          ) : totalElements > 0 ? (
            <span>
              {totalPages > 1
                ? `Hiển thị ${startItem}–${endItem} trên ${totalElements} sản phẩm`
                : `Hiển thị ${totalElements} sản phẩm`}
            </span>
          ) : (
            <span>0 sản phẩm trong danh mục</span>
          )}
        </div>

        {/* Right: Sort Selector */}
        <div className="flex items-center gap-2">
          <label
            htmlFor="category-sort"
            className="text-xs font-medium text-[#78716C]"
          >
            Sắp xếp:
          </label>
          <select
            id="category-sort"
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
      {/* 4. PRODUCT GRID / STATES                                         */}
      {/* ================================================================ */}
      {isLoading ? (
        <div className="mb-12">
          <ProductGridSkeleton count={8} />
        </div>
      ) : totalElements === 0 || pageData.empty ? (
        <div className="my-12">
          <EmptyState
            title="Chưa có sản phẩm trong danh mục này"
            message="Chúng tôi đang cập nhật các thiết bị công nghệ chính hãng mới nhất cho danh mục này. Bạn có thể xem các nhóm sản phẩm khác hoặc quay lại cửa hàng."
            actionLabel="Xem tất cả sản phẩm"
            onAction={() => router.push("/shop")}
          />
        </div>
      ) : (
        /* State 2: Populated Product Grid */
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6 mb-12">
          {pageData.content.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* ================================================================ */}
      {/* 5. PAGINATION                                                    */}
      {/* ================================================================ */}
      {!isLoading && totalPages > 1 && (
        <section className="py-6 border-t border-stone-200 mb-12 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-xs text-stone-500">
            Trang {pageData.number + 1} / {pageData.totalPages} (Tổng cộng {totalElements} sản phẩm)
          </div>

          <div className="flex items-center gap-2 text-xs">
            <button
              type="button"
              onClick={() => handlePageChange(page - 1)}
              disabled={pageData.first || page <= 0}
              className="px-3 py-1.5 rounded-lg border border-stone-200 bg-white text-stone-800 hover:border-stone-400 transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
            >
              ← Trước
            </button>

            <div className="flex items-center gap-1">
              {Array.from({ length: totalPages }, (_, idx) => idx)
                .filter((p) => {
                  if (totalPages <= 5) return true;
                  if (p === 0 || p === totalPages - 1) return true;
                  return Math.abs(p - page) <= 1;
                })
                .map((p) => (
                  <button
                    key={p}
                    type="button"
                    onClick={() => handlePageChange(p)}
                    className={`w-8 h-8 flex items-center justify-center text-xs rounded-lg font-medium transition-colors cursor-pointer ${
                      p === page
                        ? "bg-stone-900 text-white font-bold"
                        : "bg-white border border-stone-200 text-stone-800 hover:border-stone-400"
                    }`}
                  >
                    {p + 1}
                  </button>
                ))}
            </div>

            <button
              type="button"
              onClick={() => handlePageChange(page + 1)}
              disabled={pageData.last || page >= totalPages - 1}
              className="px-3 py-1.5 rounded-lg border border-stone-200 bg-white text-stone-800 hover:border-stone-400 transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
            >
              Sau →
            </button>
          </div>
        </section>
      )}
    </div>
  );
}

export default function CategoryPage({
  params,
}: {
  params: Promise<{ categoryId: string }>;
}) {
  return (
    <CustomerShell className="py-0">
      <Suspense fallback={<CategoryLoadingSkeleton />}>
        <CategoryContent params={params} />
      </Suspense>
    </CustomerShell>
  );
}
