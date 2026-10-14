import React from "react";
import Link from "next/link";

export function Footer() {
  return (
    <footer className="w-full bg-[#FAFAF9] text-[#1C1917] pt-14 pb-10 border-t border-[#E7E5E4] font-sans mt-auto">
      <div className="w-full px-3.5 sm:px-4 lg:px-5">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-12">
          {/* Brand Col */}
          <div className="space-y-4">
            <Link href="/" className="flex items-center gap-1.5 text-2xl font-black tracking-tight text-[#1C1917]">
              <span>TECHGEAR</span>
              <span className="w-2 h-2 rounded-full bg-[#D97706]" />
            </Link>
            <p className="text-sm text-[#78716C] leading-relaxed max-w-xs">
              Trải nghiệm công nghệ đỉnh cao. Chúng tôi cung cấp các thiết bị, linh kiện và phụ kiện máy tính chính hãng với tiêu chuẩn hoàn thiện chính xác.
            </p>
            <div className="flex items-center gap-2.5 pt-2">
              <a
                href="#"
                className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center text-[#78716C] hover:text-[#1C1917] hover:border-[#1C1917] transition-all"
                aria-label="Facebook TECHGEAR"
              >
                <svg className="w-4 h-4 fill-currentColor" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                </svg>
              </a>
              <a
                href="#"
                className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center text-[#78716C] hover:text-[#1C1917] hover:border-[#1C1917] transition-all"
                aria-label="Instagram TECHGEAR"
              >
                <svg className="w-4 h-4 fill-currentColor" viewBox="0 0 24 24">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
                </svg>
              </a>
              <a
                href="#"
                className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center text-[#78716C] hover:text-[#1C1917] hover:border-[#1C1917] transition-all"
                aria-label="X (Twitter) TECHGEAR"
              >
                <svg className="w-3.5 h-3.5 fill-currentColor" viewBox="0 0 24 24">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                </svg>
              </a>
              <a
                href="#"
                className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center text-[#78716C] hover:text-[#1C1917] hover:border-[#1C1917] transition-all"
                aria-label="YouTube TECHGEAR"
              >
                <svg className="w-4 h-4 fill-currentColor" viewBox="0 0 24 24">
                  <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
                </svg>
              </a>
            </div>
          </div>

          {/* Catalog Col */}
          <div className="space-y-4">
            <h4 className="font-bold text-[#1C1917] text-xs uppercase tracking-wider mb-2">
              Sản phẩm
            </h4>
            <ul className="space-y-2.5 text-[#78716C] text-sm font-medium">
              <li><Link href="/category/1" className="hover:text-[#1C1917] transition-colors">Laptop chính hãng</Link></li>
              <li><Link href="/category/13" className="hover:text-[#1C1917] transition-colors">Điện thoại thông minh</Link></li>
              <li><Link href="/category/10" className="hover:text-[#1C1917] transition-colors">Tai nghe & Âm thanh</Link></li>
              <li><Link href="/category/8" className="hover:text-[#1C1917] transition-colors">Bàn phím cơ</Link></li>
              <li><Link href="/category/9" className="hover:text-[#1C1917] transition-colors">Chuột máy tính</Link></li>
              <li><Link href="/category/11" className="hover:text-[#1C1917] transition-colors">Màn hình máy tính</Link></li>
            </ul>
          </div>

          {/* Account Col */}
          <div className="space-y-4">
            <h4 className="font-bold text-[#1C1917] text-xs uppercase tracking-wider mb-2">
              Khách hàng
            </h4>
            <ul className="space-y-2.5 text-[#78716C] text-sm font-medium">
              <li><Link href="/login" className="hover:text-[#1C1917] transition-colors">Đăng nhập / Đăng ký</Link></li>
              <li><Link href="/cart" className="hover:text-[#1C1917] transition-colors">Giỏ hàng</Link></li>
              <li><Link href="/orders" className="hover:text-[#1C1917] transition-colors">Tra cứu đơn hàng</Link></li>
              <li><Link href="#" className="hover:text-[#1C1917] transition-colors">Chính sách bảo hành</Link></li>
              <li><Link href="#" className="hover:text-[#1C1917] transition-colors">Hướng dẫn thanh toán</Link></li>
            </ul>
          </div>

          {/* Contact Col */}
          <div className="space-y-4">
            <h4 className="font-bold text-[#1C1917] text-xs uppercase tracking-wider mb-2">
              Liên hệ
            </h4>
            <ul className="space-y-3 text-[#78716C] text-sm font-medium">
              <li className="flex items-start gap-3">
                <svg className="w-4 h-4 text-[#78716C] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                <span>Tòa nhà Tech, Số 1 Đường Mới, Quận 1, TP.HCM</span>
              </li>
              <li className="flex items-center gap-3">
                <svg className="w-4 h-4 text-[#78716C] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                <span className="font-bold text-[#1C1917]">1900 1000</span>
              </li>
              <li className="flex items-center gap-3">
                <svg className="w-4 h-4 text-[#78716C] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
                <span>support@techgear.vn</span>
              </li>
            </ul>
          </div>
        </div>

        <div className="pt-6 border-t border-[#E7E5E4] flex flex-col sm:flex-row justify-between items-center text-xs text-[#78716C] gap-4">
          <div className="font-medium">© 2026 TECHGEAR VIỆT NAM. BẢO LƯU MỌI QUYỀN.</div>
          <div className="flex gap-4 items-center">
            <span className="text-[11px] text-[#A8A29E] tracking-wider uppercase">Industrial Precision</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
