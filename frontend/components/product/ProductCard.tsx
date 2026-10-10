import React from "react";
import Link from "next/link";
import { ProductDto } from "@/types/catalog";

interface ProductCardProps {
  product: ProductDto;
}

export function formatVndPrice(price: number): string {
  return new Intl.NumberFormat("vi-VN").format(price) + " ₫";
}

export function ProductCard({ product }: ProductCardProps) {
  return (
    <Link
      href={`/products/${product.id}`}
      className="group flex flex-col bg-white rounded-xl overflow-hidden border border-[#E7E5E4] hover:border-[#1C1917] transition-all duration-200"
    >
      {/* Product Image Stage */}
      <div className="relative aspect-square w-full p-4 flex items-center justify-center overflow-hidden bg-white">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="max-w-full max-h-full object-contain transition-transform duration-300 group-hover:scale-105"
            loading="lazy"
            onError={(e) => { e.currentTarget.style.display='none'; }}
          />
        ) : (
          <div className="w-full h-full bg-[#FAFAF9] flex items-center justify-center">
            <span className="text-[#A8A29E] font-medium text-xs uppercase tracking-wider">No Image</span>
          </div>
        )}
      </div>

      {/* Product Info */}
      <div className="px-4 pb-4 pt-1 flex flex-col flex-1 border-t border-[#F5F5F4]">
        {/* Product Title */}
        <h3 className="text-[13px] sm:text-sm font-medium text-[#1C1917] line-clamp-2 min-h-[40px] leading-[20px] transition-colors mb-2">
          {product.name}
        </h3>

        {/* Price */}
        <div className="mt-auto pt-2 flex items-baseline justify-between">
          <div className="text-[15px] sm:text-[16px] font-bold text-[#1C1917] tabular-nums tracking-tight">
            {formatVndPrice(product.price)}
          </div>
        </div>
      </div>
    </Link>
  );
}
