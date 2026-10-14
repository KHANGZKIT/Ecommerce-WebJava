import React from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";

export default function NotFound() {
  return (
    <CustomerShell className="py-16">
      <div className="max-w-lg mx-auto text-center p-8 sm:p-12 bg-white border border-[#E7E5E4] rounded-2xl shadow-sm">
        <div className="w-16 h-16 mx-auto mb-5 rounded-2xl bg-[#FAFAF9] border border-[#E7E5E4] flex items-center justify-center text-[#1C1917] font-black text-xl font-mono">
          404
        </div>

        <h1 className="text-2xl font-bold text-[#1C1917] mb-2 tracking-tight">
          Trang không tìm thấy
        </h1>

        <p className="text-sm text-[#78716C] leading-relaxed mb-6">
          Địa chỉ trang bạn đang tìm kiếm không tồn tại, đã bị đổi tên hoặc tạm thời gỡ bỏ khỏi hệ thống TECHGEAR.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/"
            className="inline-flex items-center px-5 py-2.5 bg-[#1C1917] hover:bg-[#292524] active:scale-95 text-white text-xs font-semibold rounded-lg transition-all shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            Về trang chủ
          </Link>
          <Link
            href="/shop"
            className="inline-flex items-center px-4 py-2.5 bg-white hover:bg-[#FAFAF9] active:scale-95 text-[#1C1917] border border-[#E7E5E4] text-xs font-semibold rounded-lg transition-all shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            Khám phá sản phẩm
          </Link>
        </div>
      </div>
    </CustomerShell>
  );
}
