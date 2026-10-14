"use client";

import React, { useEffect } from "react";
import Link from "next/link";
import { CustomerShell } from "@/components/layout/CustomerShell";

export default function RootError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Root error boundary caught error:", error);
  }, [error]);

  return (
    <CustomerShell className="py-12">
      <div className="max-w-xl mx-auto my-8 p-8 sm:p-10 bg-white border border-[#E7E5E4] rounded-2xl text-center shadow-sm">
        <div className="w-14 h-14 mx-auto mb-5 rounded-2xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600">
          <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>

        <h1 className="text-xl sm:text-2xl font-bold text-[#1C1917] mb-2 tracking-tight">
          Đã xảy ra sự cố không mong muốn
        </h1>

        <p className="text-sm text-[#78716C] leading-relaxed mb-6">
          Hệ thống gặp gián đoạn tạm thời khi kết nối hoặc tải dữ liệu. Bạn có thể nhấn thử lại hoặc quay về trang chủ.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => reset()}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#1C1917] hover:bg-[#292524] active:scale-95 text-white text-xs font-semibold rounded-lg transition-all cursor-pointer shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>Thử lại ngay</span>
          </button>

          <Link
            href="/"
            className="inline-flex items-center px-4 py-2.5 bg-white hover:bg-[#FAFAF9] active:scale-95 text-[#1C1917] border border-[#E7E5E4] text-xs font-semibold rounded-lg transition-all shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#D97706]"
          >
            Quay về trang chủ
          </Link>
        </div>
      </div>
    </CustomerShell>
  );
}
