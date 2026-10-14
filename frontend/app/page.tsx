import React from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { HeroBannerCarousel } from "@/components/home/HeroBannerCarousel";
import { CategoryStrip } from "@/components/home/CategoryStrip";
import { ProductCarousel } from "@/components/product/ProductCarousel";
import {
  getCategoriesApi,
  getProductsApi,
  getProductsByCategoryApi,
} from "@/lib/api/catalog";
import { CategoryDto, ProductDto } from "@/types/catalog";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  let categories: CategoryDto[] = [];
  let newestProducts: ProductDto[] = [];
  let laptopProducts: ProductDto[] = [];
  let phoneProducts: ProductDto[] = [];
  let audioProducts: ProductDto[] = [];
  let isNewestError = false;
  let isLaptopError = false;
  let isPhoneError = false;
  let isAudioError = false;

  try {
    const [catRes, newestRes, laptopRes, phoneRes, audioRes] = await Promise.allSettled([
      getCategoriesApi(),
      getProductsApi({ sort: "createdAt,desc", size: 12 }),
      getProductsByCategoryApi(1, { size: 12 }), // Laptop (ID 1)
      getProductsByCategoryApi(13, { size: 12 }), // Điện thoại (ID 13)
      getProductsByCategoryApi(10, { size: 12 }), // Tai nghe (ID 10)
    ]);

    if (catRes.status === "fulfilled") categories = catRes.value || [];
    if (newestRes.status === "fulfilled") {
      newestProducts = newestRes.value?.content || [];
    } else {
      isNewestError = true;
    }

    if (laptopRes.status === "fulfilled") {
      laptopProducts = laptopRes.value?.content || [];
    } else {
      isLaptopError = true;
    }

    if (phoneRes.status === "fulfilled") {
      phoneProducts = phoneRes.value?.content || [];
    } else {
      isPhoneError = true;
    }

    if (audioRes.status === "fulfilled") {
      audioProducts = audioRes.value?.content || [];
    } else {
      isAudioError = true;
    }
  } catch (error) {
    console.error("Failed to load catalog on homepage:", error);
  }

  return (
    <CustomerShell className="!px-0 !py-0 !max-w-full bg-[#FAFAF9]">
      {/* 
        Full-Width Responsive Gutter System:
        Desktop: 20px gutter (px-5 -> width: calc(100% - 40px))
        Tablet: 16px gutter (sm:px-4)
        Mobile: 14px gutter (px-3.5)
      */}
      <div className="w-full px-3.5 sm:px-4 lg:px-5 py-4 sm:py-6 space-y-6 sm:space-y-8">
        {/* 1. HERO BANNER CAROUSEL (Full Width Landscape & Editorial Showcase) */}
        <HeroBannerCarousel />

        {/* 2. CATEGORY STRIP (10 Real Categories Distributed Across Full Width) */}
        <CategoryStrip categories={categories} />

        {/* 3. PRODUCT CAROUSEL: SẢN PHẨM MỚI NHẤT */}
        <ProductCarousel
          title="SẢN PHẨM MỚI CẬP NHẬT"
          subtitle="Thiết bị công nghệ, smartphone và laptop thế hệ mới nhất vừa cập bến TECHGEAR"
          viewAllHref="/shop?sort=createdAt,desc"
          viewAllText="Xem tất cả →"
          products={newestProducts}
          isError={isNewestError}
          emptyMessage="Chưa có sản phẩm mới nào được cập nhật."
        />

        {/* 4. PROMOTIONAL WIDESCREEN BANNER: TRADE-IN THU CŨ ĐỔI MỚI */}
        <section className="w-full overflow-hidden rounded-2xl border border-[#E7E5E4] shadow-sm bg-white group">
          <Link href="/shop" className="block w-full cursor-pointer">
            <img
              src="/image/Trade-in.png"
              alt="Trade-in Thu cũ đổi mới trợ giá lên đến 5 triệu đồng"
              className="w-full h-auto max-h-[300px] object-cover sm:object-contain mx-auto transition-transform duration-500 group-hover:scale-101"
              loading="lazy"
            />
          </Link>
        </section>

        {/* 5. PRODUCT CAROUSEL: LAPTOP CHÍNH HÃNG */}
        <ProductCarousel
          title="LAPTOP & MÁY TÍNH XÁCH TAY"
          subtitle="Laptop văn phòng cao cấp, đồ họa kỹ thuật và máy tính chơi game hiệu năng cao"
          viewAllHref="/category/1"
          viewAllText="Xem tất cả laptop →"
          products={laptopProducts}
          isError={isLaptopError}
          emptyMessage="Chưa có sản phẩm laptop nào."
        />

        {/* 6. PRODUCT CAROUSEL: ĐIỆN THOẠI THÔNG MINH */}
        <ProductCarousel
          title="ĐIỆN THOẠI & THIẾT BỊ DI ĐỘNG"
          subtitle="Smartphone chính hãng, camera chuyên nghiệp và các dòng flagship thời thượng"
          viewAllHref="/category/13"
          viewAllText="Xem tất cả điện thoại →"
          products={phoneProducts}
          isError={isPhoneError}
          emptyMessage="Chưa có sản phẩm điện thoại nào."
        />

        {/* 7. PROMOTIONAL WIDESCREEN BANNER: SMART WATCH SHOWCASE */}
        <section className="w-full overflow-hidden rounded-2xl border border-[#E7E5E4] shadow-sm bg-[#0B0F19] group">
          <Link href="/category/15" className="block w-full cursor-pointer">
            <img
              src="https://cdn.cody.mn/img/487254/1920x0xwebp/Apple_watch_series_12.png"
              alt="Smart Watch Collection 2026"
              className="w-full h-auto max-h-[320px] object-cover sm:object-contain mx-auto transition-transform duration-500 group-hover:scale-101"
              loading="lazy"
            />
          </Link>
        </section>

        {/* 8. PRODUCT CAROUSEL: TAI NGHE & ÂM THANH */}
        <ProductCarousel
          title="TAI NGHE & THIẾT BỊ ÂM THANH"
          subtitle="Tai nghe chống ồn, gaming, không dây và có dây"
          viewAllHref="/category/10"
          viewAllText="Xem tất cả tai nghe →"
          products={audioProducts}
          isError={isAudioError}
          emptyMessage="Chưa có sản phẩm âm thanh nào."
        />

        {/* 9. TECHGEAR VALUES & COMMITMENTS */}
        <section className="bg-white rounded-2xl p-6 sm:p-8 border border-[#E7E5E4] shadow-sm">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 text-center sm:text-left">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center shrink-0 text-[#1C1917]">
                <svg className="w-6 h-6 text-[#D97706]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <div>
                <h4 className="text-sm font-bold text-[#1C1917] uppercase tracking-wide">100% Chính Hãng</h4>
                <p className="text-xs text-[#78716C] mt-0.5">Bảo hành chính hãng 12-24 tháng</p>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center shrink-0 text-[#1C1917]">
                <svg className="w-6 h-6 text-[#D97706]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <div>
                <h4 className="text-sm font-bold text-[#1C1917] uppercase tracking-wide">Giao Hàng Tốc Hành</h4>
                <p className="text-xs text-[#78716C] mt-0.5">Nội thành trong 2 giờ, toàn quốc 2-3 ngày</p>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center shrink-0 text-[#1C1917]">
                <svg className="w-6 h-6 text-[#D97706]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </div>
              <div>
                <h4 className="text-sm font-bold text-[#1C1917] uppercase tracking-wide">Đổi Trả Linh Hoạt</h4>
                <p className="text-xs text-[#78716C] mt-0.5">1 đổi 1 trong 30 ngày nếu có lỗi phần cứng</p>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center shrink-0 text-[#1C1917]">
                <svg className="w-6 h-6 text-[#D97706]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
              <div>
                <h4 className="text-sm font-bold text-[#1C1917] uppercase tracking-wide">Hỗ Trợ Kỹ Thuật 24/7</h4>
                <p className="text-xs text-[#78716C] mt-0.5">Tư vấn cấu hình & giải pháp chuyên sâu</p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </CustomerShell>
  );
}
