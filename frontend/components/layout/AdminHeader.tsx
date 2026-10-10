"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";

export function AdminHeader() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const isCurrent = (path: string) => {
    if (path === "/admin" && pathname === "/admin") return true;
    if (path !== "/admin" && pathname.startsWith(path)) return true;
    return false;
  };

  const navItems = [
    { label: "Tổng quan", href: "/admin" },
    { label: "Sản phẩm", href: "/admin/products" },
    { label: "Danh mục", href: "/admin/categories" },
    { label: "Người dùng", href: "/admin/users" },
    { label: "Đơn hàng", href: "/admin/orders" },
    { label: "Thanh toán", href: "/admin/payments" },
  ];

  return (
    <header className="sticky top-0 z-40 w-full h-16 bg-white border-b border-stone-200">
      <div className="flex justify-between items-center h-full w-full max-w-[1440px] mx-auto px-6">
        {/* Brand & Nav */}
        <div className="flex items-center space-x-8">
          <Link
            href="/admin"
            className="flex items-center text-sm font-bold tracking-tight text-stone-900"
          >
            <span>TECHGEAR</span>
            <span className="text-xs text-stone-500 ml-1.5 font-medium">
              — Quản trị
            </span>
          </Link>

          <nav className="hidden md:flex items-center space-x-6 text-xs font-semibold">
            {navItems.map((item) => {
              const active = isCurrent(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`py-5 border-b-2 transition-colors ${
                    active
                      ? "border-stone-900 text-stone-900 font-bold"
                      : "border-transparent text-stone-500 hover:text-stone-900"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* Right Rail: Admin Session & Actions */}
        <div className="flex items-center space-x-4">
          {user && (
            <div className="hidden sm:flex items-center space-x-2 bg-stone-50 border border-stone-200 px-2.5 py-1 rounded-lg">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600" />
              <span className="text-xs text-stone-700">
                {user.email} [{user.role === "ADMIN" ? "Quản trị viên" : user.role}]
              </span>
            </div>
          )}

          <Link
            href="/shop"
            className="text-xs font-medium text-stone-600 hover:text-stone-900 transition-colors"
          >
            Xem cửa hàng →
          </Link>

          <button
            onClick={logout}
            className="text-xs font-medium text-stone-500 hover:text-rose-600 transition-colors cursor-pointer"
            type="button"
          >
            Đăng xuất
          </button>
        </div>
      </div>
    </header>
  );
}
