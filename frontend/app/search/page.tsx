"use client";

import React, { useState, useEffect, useCallback, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
import { searchProductsApi } from "@/lib/api/catalog";
import { Page, ProductDto } from "@/types/catalog";
import { ApiError } from "@/lib/api/client";

const PAGE_SIZE = 20;

function SearchLoadingSkeleton() {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-6">
      {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
        <div
          key={i}
          className="bg-white border border-stone-200 rounded-xl p-4 animate-pulse flex flex-col"
        >
          <div className="aspect-square w-full bg-stone-100 rounded-lg mb-3" />
          <div className="h-3 w-20 bg-stone-200 rounded mb-2" />
          <div className="h-4 w-full bg-stone-200 rounded mb-4" />
          <div className="mt-auto pt-3 border-t border-stone-100 flex justify-between items-center">
            <div className="h-4 w-24 bg-stone-200 rounded" />
            <div className="h-3 w-16 bg-stone-200 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}

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
      <div className="text-xs text-stone-500 mb-4 flex items-center gap-1.5 font-medium">
        <Link href="/" className="hover:text-stone-900 transition-colors">
          Trang chủ
        </Link>
        <span>/</span>
        <span className="text-stone-900 font-semibold">Tìm kiếm</span>
      </div>

      {/* Search Form */}
      <form onSubmit={handleSearchSubmit} className="mb-8">
        <div className="flex flex-col sm:flex-row gap-2 max-w-2xl">
          <input
            type="text"
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value)}
            placeholder="Nhập tên sản phẩm hoặc từ khóa tìm kiếm..."
            className="w-full h-11 px-4 bg-white border border-stone-300 rounded-lg text-sm text-stone-900 placeholder-stone-400 focus:outline-none focus:border-stone-900 transition-colors"
          />
          <button
            type="submit"
            className="h-11 px-6 bg-stone-900 hover:bg-stone-800 text-white text-xs font-semibold rounded-lg transition-colors cursor-pointer whitespace-nowrap"
          >
            Tìm kiếm
          </button>
        </div>
      </form>

      {/* State 1: Initial / Missing Keyword */}
      {!keyword && (
        <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-stone-200 rounded-xl my-8">
          <div
            className="w-12 h-12 mx-auto mb-4 rounded-full bg-stone-100 flex items-center justify-center text-stone-600"
            aria-hidden="true"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.8}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-stone-900 mb-2">
            Tìm kiếm thiết bị công nghệ
          </h2>
          <p className="text-sm text-stone-500 mb-6">
            Nhập tên sản phẩm hoặc chọn xem danh mục để khám phá sản phẩm bạn cần.
          </p>
          <Link
            href="/shop"
            className="h-10 px-6 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors"
          >
            Xem tất cả sản phẩm
          </Link>
        </div>
      )}

      {/* State 5: Error State */}
      {keyword && error && (
        <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-rose-200 rounded-xl my-8">
          <div
            className="w-12 h-12 mx-auto mb-4 rounded-full bg-rose-50 flex items-center justify-center text-rose-600 font-bold"
            aria-hidden="true"
          >
            !
          </div>
          <h2 className="text-xl font-bold text-rose-700 mb-2">
            Lỗi khi tìm kiếm sản phẩm
          </h2>
          <p className="text-sm text-stone-600 mb-6">{error}</p>
          <button
            type="button"
            onClick={() => executeSearch(keyword, backendPage)}
            className="h-10 px-6 text-xs font-semibold text-stone-900 bg-white border border-stone-300 hover:bg-stone-100 rounded-lg inline-flex items-center justify-center transition-colors cursor-pointer"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* State 2: Loading State */}
      {keyword && !error && isLoading && (
        <div>
          <div className="pb-6 border-b border-stone-200 mb-8">
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-stone-900 mb-2">
              Kết quả tìm kiếm cho &ldquo;{keyword}&rdquo;
            </h1>
            <div className="text-xs text-stone-500 animate-pulse">
              Đang tìm kiếm trong hệ thống...
            </div>
          </div>
          <SearchLoadingSkeleton />
        </div>
      )}

      {/* State 4: No Results State */}
      {keyword &&
        !error &&
        !isLoading &&
        pageData &&
        (pageData.totalElements === 0 || pageData.empty) && (
          <div className="max-w-md mx-auto py-16 px-6 text-center bg-white border border-stone-200 rounded-xl my-8">
            <h2 className="text-xl font-bold text-stone-900 mb-2">
              Không tìm thấy sản phẩm cho &ldquo;{keyword}&rdquo;
            </h2>
            <p className="text-sm text-stone-500 mb-6">
              Vui lòng kiểm tra lại chính tả hoặc thử lại với từ khóa ngắn gọn hơn.
            </p>
            <Link
              href="/shop"
              className="h-10 px-6 text-xs font-semibold text-white bg-stone-900 hover:bg-stone-800 rounded-lg inline-flex items-center justify-center transition-colors"
            >
              Xem tất cả sản phẩm
            </Link>
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
      <Suspense fallback={<SearchLoadingSkeleton />}>
        <SearchContent />
      </Suspense>
    </CustomerShell>
  );
}
