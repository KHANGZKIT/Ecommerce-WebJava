import React from "react";
import Link from "next/link";

export function Footer() {
  return (
    <footer className="w-full bg-[#FAFAF9] text-[#1C1917] pt-14 pb-10 border-t border-[#E7E5E4] font-sans mt-auto">
      <div className="max-w-[1320px] mx-auto px-4 sm:px-6">
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
            <div className="flex items-center gap-3 pt-2">
              <a href="#" className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center hover:border-[#1C1917] transition-colors" aria-label="Facebook">
                <img src="https://d1f6qhhrbg3j8a.cloudfront.net/img/181921/0x0/facebook.png" alt="FB" className="w-3.5 h-3.5 object-contain opacity-70" />
              </a>
              <a href="#" className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center hover:border-[#1C1917] transition-colors" aria-label="Instagram">
                <img src="https://d1f6qhhrbg3j8a.cloudfront.net/img/181922/0x0/ig2.png" alt="IG" className="w-3.5 h-3.5 object-contain opacity-70" />
              </a>
              <a href="#" className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center hover:border-[#1C1917] transition-colors" aria-label="Twitter">
                <img src="https://d1f6qhhrbg3j8a.cloudfront.net/img/181923/0x0/twitt.png" alt="TW" className="w-3.5 h-3.5 object-contain opacity-70" />
              </a>
              <a href="#" className="w-8 h-8 rounded-lg bg-white border border-[#E7E5E4] flex items-center justify-center hover:border-[#1C1917] transition-colors" aria-label="YouTube">
                <img src="https://d1f6qhhrbg3j8a.cloudfront.net/img/181924/0x0/yout.png" alt="YT" className="w-3.5 h-3.5 object-contain opacity-70" />
              </a>
            </div>
          </div>

          {/* Catalog Col */}
          <div className="space-y-4">
            <h4 className="font-bold text-[#1C1917] text-xs uppercase tracking-wider mb-2">
              Sản phẩm
            </h4>
            <ul className="space-y-2.5 text-[#78716C] text-sm font-medium">
              <li><Link href="/category/1" className="hover:text-[#1C1917] transition-colors">Âm thanh & Tai nghe</Link></li>
              <li><Link href="/category/2" className="hover:text-[#1C1917] transition-colors">Bàn phím cơ</Link></li>
              <li><Link href="/category/3" className="hover:text-[#1C1917] transition-colors">Chuột máy tính</Link></li>
              <li><Link href="/category/4" className="hover:text-[#1C1917] transition-colors">Phụ kiện & Cáp</Link></li>
              <li><Link href="/category/5" className="hover:text-[#1C1917] transition-colors">Màn hình</Link></li>
              <li><Link href="/category/6" className="hover:text-[#1C1917] transition-colors">Laptop</Link></li>
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
