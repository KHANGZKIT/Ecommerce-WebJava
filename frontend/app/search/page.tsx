"use client";

import React, { useState, useEffect, useCallback, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
import { Breadcrumbs } from "@/components/ui/Breadcrumbs";
import { EmptyState, ErrorState, ProductGridSkeleton } from "@/components/ui/FeedbackStates";
import { searchProductsApi } from "@/lib/api/catalog";
import { Page, ProductDto } from "@/types/catalog";
import { ApiError } from "@/lib/api/client";

const PAGE_SIZE = 20;

function SearchContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const rawKeyword = searchParams.get("keyword") || searchParams.get("q") || "";
  const keyword = rawKeyword.trim();

  const rawPage = searchParams.get("page");
  const pageParam = Math.max(1, parseInt(rawPage || "1", 10) || 1);
  const backendPage = pageParam - 1;

  const [inputVal, setInputVal] = useState<string>(rawKeyword);
  const [pageData, setPageData] = useState<Page<ProductDto> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Sync input value with URL query changes
  useEffect(() => {
    setInputVal(rawKeyword);
  }, [rawKeyword]);

  const executeSearch = useCallback(
    async (targetKeyword: string, targetPage: number) => {
      if (!targetKeyword) {
        setPageData(null);
        setIsLoading(false);
        setError(null);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const data = await searchProductsApi(targetKeyword, {
          page: targetPage,
          size: PAGE_SIZE,
        });
        setPageData(data);
      } catch (err: unknown) {
        const msg =
          err instanceof ApiError
            ? err.message
            : err instanceof Error
            ? err.message
            : "Unable to retrieve search results.";
        setError(msg);
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (keyword) {
      executeSearch(keyword, backendPage);
    } else {
      setPageData(null);
      setIsLoading(false);
      setError(null);
    }
  }, [keyword, backendPage, executeSearch]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = inputVal.trim();
    if (trimmed) {
      router.push(`/search?keyword=${encodeURIComponent(trimmed)}`);
    } else {
      router.push("/search");
    }
  };

  const handlePageChange = (newPage: number) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set("page", String(newPage));
    if (keyword) {
      params.set("keyword", keyword);
    }
    router.push(`/search?${params.toString()}`);
  };

  return (
    <div className="py-2">
      {/* Breadcrumbs */}
      <div className="mb-4">
        <Breadcrumbs items={[{ label: "Tìm kiếm sản phẩm", href: "/search", isCurrent: true }]} />
      </div>

      {/* Search Form */}
      <form onSubmit={handleSearchSubmit} className="mb-8">
        <div className="flex flex-col sm:flex-row gap-2.5 max-w-2xl">
          <div className="relative flex-1">
            <div className="absolute left-3.5 top-3 text-[#78716C] pointer-events-none">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder="Nhập tên sản phẩm, laptop, bàn phím cơ..."
              className="w-full h-11 pl-10 pr-10 bg-white border border-[#E7E5E4] hover:border-[#1C1917]/40 rounded-xl text-sm text-[#1C1917] placeholder-[#A8A29E] focus:outline-none focus:border-[#1C1917] focus:ring-2 focus:ring-[#1C1917]/10 transition-all shadow-xs"
            />
            {inputVal && (
              <button
                type="button"
                onClick={() => setInputVal("")}
                className="absolute right-3 top-3 text-[#A8A29E] hover:text-[#1C1917] transition-colors cursor-pointer"
                aria-label="Xóa từ khóa"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
          <button
            type="submit"
            className="h-11 px-6 bg-[#1C1917] hover:bg-[#292524] active:scale-95 text-white text-xs font-semibold rounded-xl transition-all cursor-pointer whitespace-nowrap shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            Tìm kiếm
          </button>
        </div>
      </form>

      {/* State 1: Initial / Missing Keyword */}
      {!keyword && (
        <div className="my-8">
          <EmptyState
            title="Tìm kiếm thiết bị công nghệ"
            message="Nhập tên sản phẩm, thương hiệu hoặc chọn danh mục để khám phá các sản phẩm chính hãng tại TECHGEAR."
            actionLabel="Khám phá tất cả sản phẩm"
            onAction={() => router.push("/shop")}
          />
        </div>
      )}

      {/* State 5: Error State */}
      {keyword && error && (
        <div className="my-8">
          <ErrorState
            title="Lỗi khi tìm kiếm sản phẩm"
            message={error}
            onRetry={() => executeSearch(keyword, backendPage)}
          />
        </div>
      )}

      {/* State 2: Loading State */}
      {keyword && !error && isLoading && (
        <div>
          <div className="pb-6 border-b border-[#E7E5E4] mb-8">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#1C1917] mb-2">
              Kết quả tìm kiếm cho &ldquo;{keyword}&rdquo;
            </h1>
            <div className="text-xs text-[#78716C] animate-pulse">
              Đang tìm kiếm trong hệ thống...
            </div>
          </div>
          <ProductGridSkeleton count={8} />
        </div>
      )}

      {/* State 4: No Results State */}
      {keyword &&
        !error &&
        !isLoading &&
        pageData &&
        (pageData.totalElements === 0 || pageData.empty) && (
          <div className="my-8">
            <EmptyState
              title={`Không tìm thấy sản phẩm cho "${keyword}"`}
              message="Vui lòng kiểm tra lại chính tả hoặc thử lại với từ khóa ngắn gọn, thông dụng hơn."
              actionLabel="Xem toàn bộ sản phẩm"
              onAction={() => router.push("/shop")}
            />
          </div>
        )}

      {/* State 3: Success with Results */}
      {keyword &&
        !error &&
        !isLoading &&
        pageData &&
        pageData.totalElements > 0 &&
        !pageData.empty && (
          <div>
            {/* Header & Meta Result Counter */}
            <div className="pb-6 border-b border-stone-200 mb-8 flex flex-col sm:flex-row sm:items-end justify-between gap-2">
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-stone-900 mb-1">
                  Kết quả tìm kiếm cho &ldquo;{keyword}&rdquo;
                </h1>
                <div className="text-xs text-stone-500">
                  Tìm thấy {pageData.totalElements} sản phẩm phù hợp
                </div>
              </div>
            </div>

            {/* Product Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6 mb-8">
              {pageData.content.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>

            {/* Pagination Controls */}
            {pageData.totalPages > 1 && (
              <div className="pt-8 pb-4 border-t border-stone-200 flex flex-col sm:flex-row justify-between items-center gap-4">
                <div className="text-xs text-stone-500">
                  Trang {pageData.number + 1} / {pageData.totalPages} (Tổng cộng {pageData.totalElements} sản phẩm)
                </div>

                <div className="flex items-center gap-2 text-xs">
                  <button
                    type="button"
                    onClick={() => handlePageChange(pageParam - 1)}
                    disabled={pageData.first || pageParam <= 1}
                    className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                      pageData.first || pageParam <= 1
                        ? "border-stone-200 bg-stone-100 text-stone-400 cursor-not-allowed"
                        : "border-stone-200 bg-white text-stone-800 hover:border-stone-400 cursor-pointer"
                    }`}
                  >
                    ← Trước
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
                          className={`w-8 h-8 flex items-center justify-center text-xs rounded-lg font-medium transition-colors ${
                            p === pageParam
                              ? "bg-stone-900 text-white font-bold"
                              : "bg-white border border-stone-200 text-stone-800 hover:border-stone-400 cursor-pointer"
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
                    className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                      pageData.last || pageParam >= pageData.totalPages
                        ? "border-stone-200 bg-stone-100 text-stone-400 cursor-not-allowed"
                        : "border-stone-200 bg-white text-stone-800 hover:border-stone-400 cursor-pointer"
                    }`}
                  >
                    Sau →
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
    </div>
  );
}

export default function SearchPage() {
  return (
    <CustomerShell>
      <Suspense fallback={<ProductGridSkeleton count={8} />}>
        <SearchContent />
      </Suspense>
    </CustomerShell>
  );
}
