"use client";

import React, { useState } from "react";
import Link from "next/link";
import { ProductDto } from "@/types/catalog";

interface ProductCardProps {
  product: ProductDto;
}

export function formatVndPrice(price: number): string {
  return new Intl.NumberFormat("vi-VN").format(price) + " ₫";
}

export function ProductCard({ product }: ProductCardProps) {
  const [imgFailed, setImgFailed] = useState<boolean>(false);

  const hasValidImage = Boolean(product.imageUrl && !imgFailed);
  const inStock = product.stockQuantity > 0;

  return (
    <Link
      href={`/products/${product.id}`}
      className="group flex flex-col bg-white rounded-2xl overflow-hidden border border-[#E7E5E4] hover:border-[#1C1917] hover:shadow-md transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] focus-visible:ring-offset-2 active:scale-[0.99] select-none"
    >
      {/* Product Image Stage (Aspect 1:1, centered, neutral padding) */}
      <div className="relative aspect-square w-full p-4 flex items-center justify-center overflow-hidden bg-white">
        {hasValidImage ? (
          <img
            src={product.imageUrl!}
            alt={product.name}
            className="w-full h-full object-contain transition-transform duration-300 group-hover:scale-105"
            loading="lazy"
            onError={() => setImgFailed(true)}
          />
        ) : (
          <div className="w-full h-full bg-[#FAFAF9] rounded-xl border border-[#F5F5F4] flex flex-col items-center justify-center p-3 text-center">
            <svg
              className="w-8 h-8 text-[#A8A29E] mb-1.5 opacity-60 group-hover:opacity-100 transition-opacity"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
              />
            </svg>
            <span className="text-[10px] uppercase font-bold tracking-wider text-[#A8A29E]">
              {product.category?.name || "TECHGEAR"}
            </span>
          </div>
        )}

        {/* In-Stock or Low Stock Badge */}
        <div className="absolute top-2.5 left-2.5">
          {inStock ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200/80 shadow-xs">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              <span>Còn hàng</span>
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-stone-100 text-stone-600 border border-stone-200 shadow-xs">
              <span>Hết hàng</span>
            </span>
          )}
        </div>
      </div>

      {/* Product Info */}
      <div className="px-3.5 sm:px-4 pb-3.5 sm:pb-4 pt-2 flex flex-col flex-1 border-t border-[#F5F5F4]">
        {/* Category / SKU Tag */}
        <div className="flex items-center justify-between text-[11px] text-[#78716C] mb-1 font-mono">
          <span className="truncate max-w-[120px]">{product.category?.name || "Công nghệ"}</span>
          <span className="text-[10px] text-[#A8A29E] shrink-0">Chính hãng</span>
        </div>

        {/* Product Title */}
        <h3 className="text-[13px] sm:text-sm font-medium text-[#1C1917] line-clamp-2 min-h-[38px] sm:min-h-[40px] leading-[19px] sm:leading-[20px] transition-colors mb-2 break-words-vi group-hover:text-[#D97706]">
          {product.name}
        </h3>

        {/* Price & Action */}
        <div className="mt-auto pt-2 flex items-baseline justify-between border-t border-dashed border-[#F5F5F4]">
          <div className="text-[15px] sm:text-[16px] font-bold text-[#1C1917] tabular-nums tracking-tight">
            {formatVndPrice(product.price)}
          </div>
          <span className="text-[11px] font-medium text-[#78716C] group-hover:text-[#1C1917] transition-colors hidden sm:inline">
            Chi tiết →
          </span>
        </div>
      </div>
    </Link>
  );
}
