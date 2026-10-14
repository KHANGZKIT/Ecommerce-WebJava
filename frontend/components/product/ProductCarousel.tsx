"use client";

import React, { useRef, useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { ProductDto } from "@/types/catalog";
import { ProductCard } from "./ProductCard";

interface ProductCarouselProps {
  title: string;
  subtitle?: string;
  viewAllHref: string;
  viewAllText?: string;
  products: ProductDto[];
  isLoading?: boolean;
  isError?: boolean;
  emptyMessage?: string;
}

export function ProductCarousel({
  title,
  subtitle,
  viewAllHref,
  viewAllText = "Xem tất cả →",
  products,
  isLoading = false,
  isError = false,
  emptyMessage = "Chưa có sản phẩm nào trong mục này.",
}: ProductCarouselProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [canScrollLeft, setCanScrollLeft] = useState<boolean>(false);
  const [canScrollRight, setCanScrollRight] = useState<boolean>(false);

  const checkScroll = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;

    const { scrollLeft, scrollWidth, clientWidth } = el;
    // Allow small epsilon tolerance (2px)
    setCanScrollLeft(scrollLeft > 4);
    setCanScrollRight(scrollLeft + clientWidth < scrollWidth - 6);
  }, []);

  useEffect(() => {
    checkScroll();
    const el = scrollRef.current;
    if (!el) return;

    el.addEventListener("scroll", checkScroll, { passive: true });
    window.addEventListener("resize", checkScroll);

    return () => {
      el.removeEventListener("scroll", checkScroll);
      window.removeEventListener("resize", checkScroll);
    };
  }, [products, isLoading, checkScroll]);

  const handleScroll = (direction: "left" | "right") => {
    const el = scrollRef.current;
    if (!el) return;

    // Scroll by ~75% of visible container width
    const scrollAmount = Math.max(el.clientWidth * 0.75, 280);
    const targetLeft = direction === "left" ? el.scrollLeft - scrollAmount : el.scrollLeft + scrollAmount;

    el.scrollTo({
      left: targetLeft,
      behavior: "smooth",
    });
  };

  return (
    <section className="bg-white rounded-2xl p-4 sm:p-5 lg:p-6 border border-[#E7E5E4] shadow-sm">
      {/* Section Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5 border-b border-[#E7E5E4] pb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-1.5 h-4.5 bg-[#D97706] rounded-full inline-block" />
            <h2 className="text-base sm:text-lg lg:text-xl font-bold text-[#1C1917] tracking-tight uppercase">
              {title}
            </h2>
          </div>
          {subtitle && (
            <p className="text-xs text-[#78716C] mt-0.5 ml-3.5 font-medium">
              {subtitle}
            </p>
          )}
        </div>

        {/* Header Right: Navigation Links & Arrow Buttons */}
        <div className="flex items-center justify-between sm:justify-end gap-3 self-end sm:self-auto w-full sm:w-auto">
          <Link
            href={viewAllHref}
            className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors inline-flex items-center gap-1"
          >
            <span>{viewAllText}</span>
          </Link>

          {/* Carousel Arrow Buttons */}
          <div className="flex items-center gap-1.5 pl-2 border-l border-[#E7E5E4]">
            <button
              type="button"
              onClick={() => handleScroll("left")}
              disabled={!canScrollLeft}
              className={`w-8 h-8 rounded-lg flex items-center justify-center border transition-all cursor-pointer ${
                canScrollLeft
                  ? "bg-white text-[#1C1917] border-[#E7E5E4] hover:bg-[#FAFAF9] hover:border-[#1C1917] active:scale-95"
                  : "bg-[#FAFAF9] text-[#A8A29E] border-[#F5F5F4] opacity-40 cursor-not-allowed"
              }`}
              aria-label={`Cuộn sang trái danh sách ${title}`}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => handleScroll("right")}
              disabled={!canScrollRight}
              className={`w-8 h-8 rounded-lg flex items-center justify-center border transition-all cursor-pointer ${
                canScrollRight
                  ? "bg-white text-[#1C1917] border-[#E7E5E4] hover:bg-[#FAFAF9] hover:border-[#1C1917] active:scale-95"
                  : "bg-[#FAFAF9] text-[#A8A29E] border-[#F5F5F4] opacity-40 cursor-not-allowed"
              }`}
              aria-label={`Cuộn sang phải danh sách ${title}`}
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Content Area */}
      {isLoading ? (
        /* Loading Skeleton: 6 responsive cards */
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 sm:gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="h-68 bg-[#FAFAF9] animate-pulse rounded-xl border border-[#E7E5E4] p-4 flex flex-col justify-between"
            >
              <div className="aspect-square w-full bg-[#E7E5E4]/60 rounded-lg" />
              <div className="space-y-2 mt-3">
                <div className="h-3 w-1/2 bg-[#E7E5E4]/80 rounded" />
                <div className="h-4 w-4/5 bg-[#E7E5E4] rounded" />
                <div className="h-4 w-1/3 bg-[#E7E5E4] rounded mt-2" />
              </div>
            </div>
          ))}
        </div>
      ) : isError ? (
        <div className="p-8 bg-[#FAFAF9] rounded-2xl text-center border border-rose-200">
          <div className="w-10 h-10 mx-auto mb-2 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="text-sm font-semibold text-rose-700 mb-1">
            Không thể tải danh sách sản phẩm {title.toLowerCase()}
          </p>
          <p className="text-xs text-[#78716C] mb-4">
            Đang có sự cố kết nối máy chủ dịch vụ sản phẩm. Bạn có thể xem trên trang cửa hàng.
          </p>
          <Link
            href={viewAllHref}
            className="inline-flex items-center px-4 py-2 bg-white border border-[#E7E5E4] hover:border-[#1C1917] text-xs font-semibold rounded-lg text-[#1C1917] transition-all"
          >
            Đến trang sản phẩm →
          </Link>
        </div>
      ) : products.length === 0 ? (
        <div className="py-10 px-6 bg-[#FAFAF9] text-[#78716C] rounded-2xl text-center border border-[#E7E5E4]">
          <div className="w-10 h-10 mx-auto mb-2.5 rounded-xl bg-white border border-[#E7E5E4] flex items-center justify-center text-[#A8A29E]">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          <p className="text-xs sm:text-sm font-medium text-[#78716C] mb-3">
            {emptyMessage}
          </p>
          <Link
            href={viewAllHref}
            className="inline-flex items-center px-4 py-1.5 bg-white border border-[#E7E5E4] hover:border-[#1C1917] text-xs font-semibold rounded-lg text-[#1C1917] transition-all"
          >
            Khám phá tất cả →
          </Link>
        </div>
      ) : (
        /* Scrollable Product Strip: 5-6 cards on desktop, 3-4 on tablet, 2 on mobile */
        <div
          ref={scrollRef}
          className="flex gap-3 sm:gap-4 overflow-x-auto pb-2 pt-1 hide-scrollbar snap-x snap-mandatory scroll-smooth"
        >
          {products.map((product) => (
            <div
              key={product.id}
              className="shrink-0 snap-start w-[calc(50%-6px)] sm:w-[calc(33.333%-11px)] md:w-[calc(25%-12px)] lg:w-[calc(20%-13px)] xl:w-[calc(16.666%-14px)] min-w-[155px] sm:min-w-[180px] md:min-w-[200px]"
            >
              <ProductCard product={product} />
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
