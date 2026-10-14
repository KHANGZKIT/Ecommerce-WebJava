"use client";

import React from "react";
import Link from "next/link";
import { CategoryDto } from "@/types/catalog";

// Local image maps for guaranteed uptime and zero broken image boxes
const CATEGORY_LOCAL_ASSETS: Record<number, { img: string; iconFallback?: string }> = {
  1: { img: "/img/433524/500x0xwebp/NoteBookM.jpg" }, // Laptop
  13: { img: "/img/433525/500x0xwebp/PhoneM.jpg" }, // Điện thoại
  10: { img: "/img/433528/500x0xwebp/HeadsetM.jpg" }, // Tai nghe
  14: { img: "/img/433526/500x0xwebp/TabletM.jpg" }, // Máy tính bảng
  15: { img: "/img/433527/500x0xwebp/watchM.jpg" }, // Đồng hồ thông minh
  11: { img: "/img/433529/500x0xwebp/CameraM.jpg" }, // Màn hình
  16: { img: "/img/433530/500x0xwebp/CopierM.jpg" }, // Máy tính để bàn
  8: { img: "/img/433524/500x0xwebp/NoteBookM.jpg" }, // Bàn phím cơ
  9: { img: "/img/433525/500x0xwebp/PhoneM.jpg" }, // Chuột máy tính
  12: { img: "/img/433528/500x0xwebp/HeadsetM.jpg" }, // Phụ kiện
};

interface CategoryItem {
  id: number;
  name: string;
  imageUrl?: string | null;
  active?: boolean;
}

// 7 Approved Core System Categories (plus 3 accessories)
const DEFAULT_SYSTEM_CATEGORIES: CategoryItem[] = [
  { id: 1, name: "Laptop", imageUrl: "/img/433524/500x0xwebp/NoteBookM.jpg", active: true },
  { id: 13, name: "Điện thoại", imageUrl: "/img/433525/500x0xwebp/PhoneM.jpg", active: true },
  { id: 10, name: "Tai nghe", imageUrl: "/img/433528/500x0xwebp/HeadsetM.jpg", active: true },
  { id: 14, name: "Máy tính bảng", imageUrl: "/img/433526/500x0xwebp/TabletM.jpg", active: true },
  { id: 15, name: "Đồng hồ", imageUrl: "/img/433527/500x0xwebp/watchM.jpg", active: true },
  { id: 11, name: "Màn hình", imageUrl: "/img/433529/500x0xwebp/CameraM.jpg", active: true },
  { id: 16, name: "Máy tính để bàn", imageUrl: "/img/433530/500x0xwebp/CopierM.jpg", active: true },
  { id: 8, name: "Bàn phím cơ", imageUrl: "/img/433524/500x0xwebp/NoteBookM.jpg", active: true },
  { id: 9, name: "Chuột máy tính", imageUrl: "/img/433525/500x0xwebp/PhoneM.jpg", active: true },
  { id: 12, name: "Phụ kiện", imageUrl: "/img/433528/500x0xwebp/HeadsetM.jpg", active: true },
];

interface CategoryStripProps {
  categories: CategoryDto[];
}

export function CategoryStrip({ categories }: CategoryStripProps) {
  // Merge loaded categories with local asset map
  const activeItems: CategoryItem[] = (
    categories && categories.length > 0 ? categories : DEFAULT_SYSTEM_CATEGORIES
  ).filter((c) => c.active !== false);

  return (
    <section className="bg-white rounded-2xl p-4 sm:p-5 lg:p-6 border border-[#E7E5E4] shadow-sm">
      <div className="flex items-center justify-between mb-4 pb-3 border-b border-[#E7E5E4]">
        <div className="flex items-center gap-2">
          <span className="w-1.5 h-4.5 bg-[#D97706] rounded-full inline-block" />
          <h2 className="text-base sm:text-lg font-bold text-[#1C1917] tracking-tight uppercase">
            DANH MỤC THIẾT BỊ NỔI BẬT
          </h2>
        </div>
        <Link
          href="/shop"
          className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors"
        >
          Tất cả danh mục →
        </Link>
      </div>

      {/* Responsive Grid: Expands across entire width, 5-10 columns on desktop */}
      <div className="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-7 lg:grid-cols-10 gap-3 sm:gap-4 pt-1">
        {activeItems.map((cat) => {
          const imageSrc =
            CATEGORY_LOCAL_ASSETS[cat.id]?.img ||
            cat.imageUrl ||
            "/img/433524/500x0xwebp/NoteBookM.jpg";

          return (
            <Link
              key={cat.id}
              href={`/category/${cat.id}`}
              className="group flex flex-col items-center justify-start text-center p-1.5 sm:p-2 rounded-xl transition-all duration-200 hover:bg-[#FAFAF9] active:scale-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] rounded-xl"
            >
              {/* Category Icon Card */}
              <div className="w-13 h-13 sm:w-16 sm:h-16 md:w-18 md:h-18 lg:w-20 lg:h-20 rounded-2xl bg-[#FAFAF9] border border-[#E7E5E4] group-hover:border-[#1C1917] group-hover:shadow-md p-2 flex items-center justify-center overflow-hidden transition-all duration-300">
                <img
                  src={imageSrc}
                  alt={cat.name}
                  className="w-full h-full object-contain group-hover:scale-110 transition-transform duration-300"
                  loading="lazy"
                  onError={(e) => {
                    // Fallback to local notebook image
                    e.currentTarget.src = "/img/433524/500x0xwebp/NoteBookM.jpg";
                  }}
                />
              </div>

              {/* Category Label */}
              <span className="mt-2 text-[10.5px] sm:text-xs font-semibold text-[#1C1917] group-hover:text-[#D97706] tracking-tight leading-tight line-clamp-2 min-h-[26px] sm:min-h-0 break-words-vi transition-colors">
                {cat.name}
              </span>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
