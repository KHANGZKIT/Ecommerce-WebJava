"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "./AuthContext";
import Link from "next/link";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center py-24">
        <div className="text-xs text-stone-500 font-medium animate-pulse">
          Đang tải phiên đăng nhập...
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}

export function AdminGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center py-24">
        <div className="text-xs text-stone-500 font-medium animate-pulse">
          Đang xác thực quyền quản trị viên...
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  if (!isAdmin) {
    return (
      <div className="flex-1 flex items-center justify-center py-24 px-6">
        <div className="max-w-[480px] w-full bg-white border border-rose-200 rounded-xl p-8 text-center shadow-sm">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-rose-50 border border-rose-200 text-rose-600 text-xl mb-4 font-bold">
            !
          </div>
          <h2 className="font-bold text-xl text-stone-900 mb-2">Quyền truy cập bị hạn chế</h2>
          <p className="text-sm text-stone-600 mb-6">
            Khu vực này yêu cầu tài khoản quản trị viên. Phiên đăng nhập khách hàng của bạn vẫn được giữ nguyên.
          </p>
          <div className="flex justify-center gap-3">
            <Link
              href="/shop"
              className="inline-flex items-center justify-center text-xs font-semibold px-5 py-2.5 rounded-lg bg-stone-900 text-white hover:bg-stone-800 transition-colors shadow-sm"
            >
              Quay lại cửa hàng
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
