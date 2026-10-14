"use client";

import React, { useState, useEffect, useRef, useCallback } from "react";
import Link from "next/link";

interface CampaignSlide {
  id: string;
  imageSrc: string;
  mobileImageSrc?: string;
  fallbackImageSrc?: string;
  imageAlt: string;
  link: string;
  bgClass: string;
}

const CAMPAIGN_SLIDES: CampaignSlide[] = [
  {
    id: "iphone-18-pro-campaign",
    imageSrc: "/image/Iphone.png",
    imageAlt: "Chương trình mở bán thế hệ iPhone 18 Pro — Thu cũ đổi mới, tặng AirPods 5, adapter sạc nhanh và bảo hành toàn diện SHIELD+",
    link: "/shop?search=iPhone",
    bgClass: "bg-[#EFF0F0]", // Màu nền tiệp hoàn hảo với màu viền thực tế của asset iPhone 18 Pro
  },
  {
    id: "trade-in-campaign",
    imageSrc: "/image/Trade-in.png",
    imageAlt: "Chương trình Trade-in — Thu cũ đổi mới lên đời trợ giá đến 5 triệu đồng tại TECHGEAR",
    link: "/shop",
    bgClass: "bg-white", // Màu nền trắng tiệp hoàn hảo với background thực tế của asset Trade-in
  },
  {
    id: "apple-watch-campaign",
    imageSrc: "https://cdn.cody.mn/img/487254/1920x0xwebp/Apple_watch_series_12.png",
    mobileImageSrc: "https://cdn.cody.mn/img/487377/1920x0xwebp/Apple_watch_series_12_mobie_size.png",
    fallbackImageSrc: "/img/433527/500x0xwebp/watchM.jpg",
    imageAlt: "Bộ sưu tập Apple Watch Series 12 chính hãng 2026 tại TECHGEAR",
    link: "/category/15",
    bgClass: "bg-[#0B0F19]", // Nền xanh đen sâu thẳm tiệp với dải màu Apple Watch
  },
];

export function HeroBannerCarousel() {
  const [currentSlide, setCurrentSlide] = useState<number>(0);
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const totalSlides = CAMPAIGN_SLIDES.length;

  const nextSlide = useCallback(() => {
    setCurrentSlide((prev) => (prev + 1) % totalSlides);
  }, [totalSlides]);

  const prevSlide = useCallback(() => {
    setCurrentSlide((prev) => (prev - 1 + totalSlides) % totalSlides);
  }, [totalSlides]);

  // Tự động chuyển slide sau mỗi 5.5 giây (tạm dừng khi hover hoặc focus)
  useEffect(() => {
    if (isPaused) return;

    timerRef.current = setInterval(() => {
      nextSlide();
    }, 5500);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPaused, nextSlide]);

  return (
    <div
      className="relative w-full rounded-2xl overflow-hidden border border-[#E7E5E4] shadow-xs select-none group"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocus={() => setIsPaused(true)}
      onBlur={() => setIsPaused(false)}
      role="region"
      aria-label="Khu vực Banner Chiến dịch Khuyến mãi TECHGEAR"
    >
      {/* Banner Slides Stage (Responsive height, preserving exact aspect ratio without cropping) */}
      <div className="relative w-full h-[220px] sm:h-[300px] md:h-[380px] lg:h-[450px] xl:h-[480px] overflow-hidden">
        {CAMPAIGN_SLIDES.map((slide, index) => {
          const isActive = index === currentSlide;

          return (
            <div
              key={slide.id}
              className={`absolute inset-0 w-full h-full transition-opacity duration-500 ease-in-out ${
                isActive ? "opacity-100 z-10" : "opacity-0 pointer-events-none z-0"
              }`}
            >
              <Link
                href={slide.link}
                className={`block w-full h-full relative cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-[#1C1917] ${slide.bgClass}`}
                tabIndex={isActive ? 0 : -1}
                aria-label={slide.imageAlt}
              >
                {/* Desktop / Default Image */}
                <img
                  src={slide.imageSrc}
                  alt={slide.imageAlt}
                  className={`w-full h-full object-contain mx-auto transition-transform duration-500 ease-out group-hover:scale-[1.008] ${
                    slide.mobileImageSrc ? "hidden sm:block" : "block"
                  }`}
                  loading={index === 0 ? "eager" : "lazy"}
                  onError={(e) => {
                    if (slide.fallbackImageSrc) {
                      e.currentTarget.src = slide.fallbackImageSrc;
                    }
                  }}
                />

                {/* Mobile Optimized Image (nếu có asset riêng) */}
                {slide.mobileImageSrc && (
                  <img
                    src={slide.mobileImageSrc}
                    alt={slide.imageAlt}
                    className="w-full h-full object-contain mx-auto sm:hidden"
                    loading="lazy"
                    onError={(e) => {
                      if (slide.fallbackImageSrc) {
                        e.currentTarget.src = slide.fallbackImageSrc;
                      }
                    }}
                  />
                )}
              </Link>
            </div>
          );
        })}
      </div>

      {/* Prev / Next Arrows (Tinh tế, phong cách e-commerce hiện đại) */}
      {totalSlides > 1 && (
        <>
          <button
            type="button"
            onClick={prevSlide}
            className="absolute left-2.5 sm:left-4 top-1/2 -translate-y-1/2 z-20 w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-white/90 hover:bg-white text-[#1C1917] border border-[#E7E5E4] shadow-sm hover:shadow-md transition-all flex items-center justify-center cursor-pointer active:scale-95 focus:outline-none focus:ring-2 focus:ring-[#1C1917]"
            aria-label="Xem banner chiến dịch trước"
          >
            <svg className="w-4 h-4 sm:w-4.5 sm:h-4.5 text-[#1C1917]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </button>

          <button
            type="button"
            onClick={nextSlide}
            className="absolute right-2.5 sm:right-4 top-1/2 -translate-y-1/2 z-20 w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-white/90 hover:bg-white text-[#1C1917] border border-[#E7E5E4] shadow-sm hover:shadow-md transition-all flex items-center justify-center cursor-pointer active:scale-95 focus:outline-none focus:ring-2 focus:ring-[#1C1917]"
            aria-label="Xem banner chiến dịch tiếp theo"
          >
            <svg className="w-4 h-4 sm:w-4.5 sm:h-4.5 text-[#1C1917]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </>
      )}

      {/* Slide Indicators (Dots thanh lịch, số chấm tương ứng chính xác số slide thực tế) */}
      {totalSlides > 1 && (
        <div className="absolute bottom-2.5 sm:bottom-3.5 left-1/2 -translate-x-1/2 z-20 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/85 backdrop-blur-sm border border-[#E7E5E4] shadow-xs">
          {CAMPAIGN_SLIDES.map((slide, idx) => (
            <button
              key={slide.id}
              type="button"
              onClick={() => setCurrentSlide(idx)}
              className={`transition-all duration-300 rounded-full cursor-pointer focus:outline-none ${
                idx === currentSlide
                  ? "w-5 sm:w-6 h-1.5 sm:h-2 bg-[#1C1917]"
                  : "w-1.5 sm:w-2 h-1.5 sm:h-2 bg-[#D6D3D1] hover:bg-[#A8A29E]"
              }`}
              aria-label={`Chuyển đến banner ${idx + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
