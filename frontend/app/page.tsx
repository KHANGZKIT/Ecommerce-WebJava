"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";
import { ProductCard } from "@/components/product/ProductCard";
import { getCategoriesApi, getProductsApi } from "@/lib/api/catalog";
import { CategoryDto, ProductDto } from "@/types/catalog";

const CANONICAL_CATEGORIES = [
  { id: 1, name: "NOTEBOOK", img: "https://cdn.cody.mn/img/433524/500x0xwebp/NoteBookM.jpg" },
  { id: 2, name: "ĐIỆN THOẠI", img: "https://cdn.cody.mn/img/433525/500x0xwebp/PhoneM.jpg" },
  { id: 3, name: "MÁY TÍNH BẢNG", img: "https://cdn.cody.mn/img/433526/500x0xwebp/TabletM.jpg" },
  { id: 4, name: "ĐỒNG HỒ", img: "https://cdn.cody.mn/img/433527/500x0xwebp/watchM.jpg" },
  { id: 5, name: "ÂM THANH", img: "https://cdn.cody.mn/img/433528/500x0xwebp/HeadsetM.jpg" },
  { id: 6, name: "MÁY ẢNH", img: "https://cdn.cody.mn/img/433529/500x0xwebp/CameraM.jpg" },
  { id: 7, name: "MÁY IN", img: "https://cdn.cody.mn/img/433530/500x0xwebp/CopierM.jpg" },
  { id: 8, name: "NHÀ THÔNG MINH", img: "https://cdn.cody.mn/img/433531/500x0xwebp/HomeM.jpg" }
];

export default function HomePage() {
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isError, setIsError] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;

    async function loadHomeCatalog() {
      try {
        setIsError(false);
        const [categoryList, productPage] = await Promise.all([
          getCategoriesApi(),
          getProductsApi({ size: 20 }),
        ]);

        if (!isMounted) return;
        setCategories(categoryList || []);
        setProducts(productPage.content || []);
      } catch (error) {
        if (!isMounted) return;
        setIsError(true);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadHomeCatalog();

    return () => {
      isMounted = false;
    };
  }, []);

  const displayCategories = CANONICAL_CATEGORIES.map((canonical) => {
    const matched = categories.find(
      (c) => c.id === canonical.id || c.name.toLowerCase().includes(canonical.name.toLowerCase())
    );
    return {
      id: matched?.id || canonical.id,
      name: matched?.name || canonical.name,
      img: canonical.img,
    };
  });

  const featuredProducts = products.slice(0, 5);
  const appleProducts = products.slice(5, 10);
  const pcProducts = products.slice(10, 15);
  const watchProducts = products.slice(15, 20);

  return (
    <CustomerShell className="!px-0 !py-0 !max-w-full bg-[#FAFAF9]">
      {/* 1. HERO BANNER */}
      <section className="w-full bg-[#FAFAF9] mb-6 border-b border-[#E7E5E4]">
        <div className="w-full relative max-w-[1920px] mx-auto flex justify-center items-center">
          {/* Main Hero Banner */}
          <div className="w-full cursor-pointer flex justify-center items-center overflow-hidden h-[260px] sm:h-[340px] md:h-[420px] lg:h-[460px]">
            <img
              src="/image/Iphone.png"
              alt="iPhone 18 Pro Banner"
              className="h-full w-auto max-w-full object-contain mx-auto"
            />
          </div>
        </div>
      </section>

      <div className="w-full max-w-[1320px] mx-auto px-4 sm:px-6 pb-12 space-y-8">
        {/* 2. CATEGORY TILES */}
        <section className="pt-2">
          <div className="flex flex-nowrap overflow-x-auto md:grid md:grid-cols-8 gap-4 md:gap-4 pb-4 md:pb-0 hide-scrollbar snap-x">
            {displayCategories.map((cat) => (
              <Link href={`/category/${cat.id}`} key={cat.id} className="group flex flex-col items-center flex-shrink-0 snap-center w-[80px] md:w-auto">
                <div className="w-[72px] h-[72px] sm:w-[88px] sm:h-[88px] rounded-xl sm:rounded-2xl bg-white border border-[#E7E5E4] hover:border-[#1C1917] overflow-hidden transition-all duration-200">
                  <img src={cat.img} alt={cat.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" onError={(e) => { e.currentTarget.style.display = 'none'; }} />
                </div>
                <span className="mt-2.5 text-[11px] sm:text-xs font-semibold text-[#1C1917] text-center uppercase tracking-tight">
                  {cat.name}
                </span>
              </Link>
            ))}
          </div>
        </section>

        {/* 3. SẢN PHẨM NỔI BẬT */}
        <section className="bg-white rounded-xl p-4 sm:p-5 border border-[#E7E5E4]">
          <div className="flex items-center justify-between mb-4 border-b border-[#E7E5E4] pb-3">
            <h2 className="text-lg sm:text-xl font-bold text-[#1C1917] tracking-tight">SẢN PHẨM NỔI BẬT</h2>
            <Link href="/shop" className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors">Xem tất cả →</Link>
          </div>

          {isLoading ? (
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3 sm:gap-4">
              {[1, 2, 3, 4, 5].map(i => <div key={i} className="h-64 bg-[#FAFAF9] animate-pulse rounded-xl border border-[#E7E5E4]" />)}
            </div>
          ) : isError ? (
            <div className="p-4 bg-rose-50 text-rose-700 rounded-lg text-center text-sm border border-rose-200">
              Không thể tải danh sách sản phẩm lúc này. Vui lòng thử lại sau.
            </div>
          ) : featuredProducts.length === 0 ? (
            <div className="p-4 bg-[#FAFAF9] text-[#78716C] rounded-lg text-center text-sm border border-[#E7E5E4]">
              Chưa có sản phẩm nào.
            </div>
          ) : (
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
              {featuredProducts.map(product => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}
        </section>

        {/* 4. NEW APPLE IS HERE SHOWCASE */}
        <section className="pt-2">
          <div className="text-center mb-6">
            <h2 className="text-2xl sm:text-3xl font-black text-[#1C1917] tracking-tight uppercase">
              NEW APPLE IS HERE
            </h2>
          </div>

          <div className="flex md:grid md:grid-cols-4 gap-4 sm:gap-5 overflow-x-auto pb-4 md:pb-0 hide-scrollbar snap-x snap-mandatory">
            {[
              {
                id: "iphone",
                name: "iPhone 18 Pro Max",
                subtitle: "Titanium thiết kế mới & Chip A18 Pro",
                image: "https://cdn.cody.mn/img/487251/800x0xwebp/iPhone_18_Pro_Max.png",
                fallbackImage: "/image/Iphone.png",
                link: "/shop?search=iPhone",
              },
              {
                id: "airpods",
                name: "AirPods 5",
                subtitle: "Âm thanh sống động, chống ồn thông minh",
                image: "https://cdn.cody.mn/img/487253/800x0xwebp/Air_Pods_5.png",
                link: "/shop?search=AirPods",
              },
              {
                id: "watch-series",
                name: "WATCH",
                series: "SERIES 12",
                subtitle: "Màn hình sáng vượt bậc & Cảm biến tối tân",
                image: "https://cdn.cody.mn/img/487254/800x0xwebp/Apple_watch_series_12.png",
                link: "/shop?search=Watch",
              },
              {
                id: "watch-se",
                name: "WATCH",
                series: "SE 3",
                subtitle: "Kết nối trọn vẹn, phong cách năng động",
                image: "https://cdn.cody.mn/img/487255/800x0xwebp/Apple_watch_se_3.png",
                link: "/shop?search=Watch+SE",
              },
            ].map((item) => (
              <Link
                key={item.id}
                href={item.link}
                className="group relative flex-shrink-0 snap-center w-[260px] sm:w-[280px] md:w-auto h-[360px] sm:h-[400px] md:h-[420px] rounded-xl overflow-hidden transition-all duration-300 block focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706] bg-[#1C1917]"
              >
                {/* Background Image */}
                <img
                  src={item.image}
                  alt={item.name}
                  className="absolute inset-0 w-full h-full object-cover transition-all duration-500 ease-out group-hover:scale-105 brightness-90 group-hover:brightness-100"
                  onError={(e) => {
                    if (item.fallbackImage) {
                      e.currentTarget.src = item.fallbackImage;
                    }
                  }}
                />

                {/* Dark Gradient Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/40 to-black/20 transition-opacity duration-300 group-hover:opacity-75" />

                {/* Card Content */}
                <div className="relative h-full flex flex-col justify-between p-6 text-white text-center">
                  {/* Center Content: Product Title & Logo */}
                  <div className="my-auto transform transition-transform duration-300 ease-out group-hover:-translate-y-3">
                    <div className="flex items-center justify-center gap-1.5 text-xl sm:text-2xl font-bold tracking-tight">
                      <svg
                        className="w-5 h-5 sm:w-6 sm:h-6 fill-current shrink-0 -mt-0.5"
                        viewBox="0 0 170 170"
                        aria-hidden="true"
                      >
                        <path d="M150.37 130.25c-2.45 5.66-5.35 10.87-8.71 15.66-4.58 6.53-8.33 11.05-11.22 13.56-4.48 4.12-9.28 6.23-14.42 6.35-3.69 0-8.14-1.05-13.32-3.18-5.19-2.12-9.97-3.17-14.34-3.17-4.58 0-9.49 1.05-14.75 3.17-5.26 2.13-9.5 3.24-12.74 3.35-4.35.13-9.16-1.9-14.42-6.08-3.69-3.04-7.67-7.81-11.96-14.34-6.42-9.67-11.45-20.9-15.08-33.68-3.63-12.79-5.45-24.89-5.45-36.32 0-14.68 3.51-26.79 10.53-36.32 7.02-9.53 16.03-14.39 27.03-14.59 4.9 0 10.37 1.35 16.42 4.05 6.05 2.7 10.04 4.09 11.97 4.17 1.63 0 5.86-1.48 12.69-4.44 6.83-2.96 12.56-4.29 17.2-4 12.87.82 22.99 5.84 30.36 15.06-11.2 6.74-16.68 16.32-16.44 28.73.23 10.02 4.16 18.28 11.78 24.78 7.62 6.5 16.64 10.15 27.07 10.96-2.5 7.42-5.74 15.35-9.73 23.8zM119.22 33.64c0-7.23 2.64-13.92 7.93-20.08 5.29-6.15 11.78-9.87 19.46-11.16.82 7.02-1.61 13.84-7.29 20.46-5.68 6.62-12.38 10.53-20.1 10.78z" />
                      </svg>
                      <span>{item.name}</span>
                    </div>
                    {item.series && (
                      <div className="text-xs sm:text-sm font-semibold tracking-wider text-stone-300 mt-1 uppercase">
                        {item.series}
                      </div>
                    )}
                  </div>

                  {/* Hover Details: Subtitle & Explore Button */}
                  <div className="opacity-0 translate-y-3 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 ease-out flex flex-col items-center gap-3">
                    <p className="text-xs sm:text-sm text-stone-200 line-clamp-2 max-w-[220px]">
                      {item.subtitle}
                    </p>
                    <span className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-white text-[#1C1917] font-semibold text-xs sm:text-sm hover:bg-[#FAFAF9] transition-colors">
                      Khám phá
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </section>

        {/* 5. SẢN PHẨM APPLE */}
        <section className="bg-white rounded-xl p-4 sm:p-5 border border-[#E7E5E4]">
          <div className="flex items-center justify-between mb-4 border-b border-[#E7E5E4] pb-3">
            <h2 className="text-lg sm:text-xl font-bold text-[#1C1917] tracking-tight">THIẾT BỊ APPLE</h2>
            <Link href="/shop" className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors">Xem tất cả →</Link>
          </div>

          {!isLoading && !isError && appleProducts.length > 0 ? (
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
              {appleProducts.map(product => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : null}
        </section>

        {/* BANNER TRADE-IN */}
        <section className="w-full overflow-hidden rounded-xl border border-[#E7E5E4]">
          <img
            src="/image/Trade-in.png"
            alt="Trade-in - Trade in your old device"
            className="block w-full h-auto object-contain"
          />
        </section>

        {/* 7. SẢN PHẨM MÁY TÍNH */}
        <section className="bg-white rounded-xl p-4 sm:p-5 border border-[#E7E5E4] mb-8">
          <div className="flex items-center justify-between mb-4 border-b border-[#E7E5E4] pb-3">
            <h2 className="text-lg sm:text-xl font-bold text-[#1C1917] tracking-tight">MÁY TÍNH XÁCH TAY</h2>
            <Link href="/shop" className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors">Xem tất cả →</Link>
          </div>

          {!isLoading && !isError && pcProducts.length > 0 ? (
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
              {pcProducts.map(product => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : null}
        </section>

        {/* 8. BANNER XEN KẼ 3 - SMART WATCH */}
        <section className="overflow-hidden rounded-xl border border-[#E7E5E4] cursor-pointer transition-shadow">
          <img src="https://cdn.cody.mn/img/487254/1920x0xwebp/Apple_watch_series_12.png" alt="Smart Watch Banner" className="w-full object-cover hidden md:block" />
          <img src="https://cdn.cody.mn/img/487377/1920x0xwebp/Apple_watch_series_12_mobie_size.png" alt="Smart Watch Banner Mobile" className="w-full object-cover md:hidden aspect-[2/1]" />
        </section>

        {/* 9. SẢN PHẨM SMART WATCH */}
        <section className="bg-white rounded-xl p-4 sm:p-5 border border-[#E7E5E4] mb-8">
          <div className="flex items-center justify-between mb-4 border-b border-[#E7E5E4] pb-3">
            <h2 className="text-lg sm:text-xl font-bold text-[#1C1917] tracking-tight">THIẾT BỊ ĐEO THÔNG MINH</h2>
            <Link href="/shop" className="text-xs sm:text-sm font-semibold text-[#78716C] hover:text-[#1C1917] uppercase tracking-wider transition-colors">Xem tất cả →</Link>
          </div>

          {!isLoading && !isError && watchProducts.length > 0 ? (
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
              {watchProducts.map(product => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : null}
        </section>

      </div>
    </CustomerShell>
  );
}
