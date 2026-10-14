"use client";

import React, { useEffect, useState, useRef } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth/AuthContext";
import { getCartApi } from "@/lib/api/cart";

// Danh mục sản phẩm công nghệ chuẩn mực của hệ thống
const TECH_CATEGORIES = [
  { id: 1, name: "Laptop & Máy tính", shortName: "Laptop", href: "/category/1" },
  { id: 13, name: "Điện thoại thông minh", shortName: "Điện thoại", href: "/category/13" },
  { id: 10, name: "Tai nghe & Âm thanh", shortName: "Tai nghe", href: "/category/10" },
  { id: 14, name: "Máy tính bảng", shortName: "Tablet", href: "/category/14" },
  { id: 15, name: "Đồng hồ thông minh", shortName: "Đồng hồ", href: "/category/15" },
  { id: 11, name: "Màn hình máy tính", shortName: "Màn hình", href: "/category/11" },
  { id: 16, name: "Máy tính để bàn", shortName: "PC Desktop", href: "/category/16" },
  { id: 8, name: "Bàn phím cơ", shortName: "Bàn phím", href: "/category/8" },
  { id: 9, name: "Chuột máy tính", shortName: "Chuột", href: "/category/9" },
  { id: 12, name: "Phụ kiện công nghệ", shortName: "Phụ kiện", href: "/category/12" },
];

export function Header() {
  const pathname = usePathname();
  const { isAuthenticated, isAdmin, logout } = useAuth();
  const [cartCount, setCartCount] = useState<number>(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [categoryMenuOpen, setCategoryMenuOpen] = useState(false);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);

  const categoryDropdownRef = useRef<HTMLDivElement>(null);
  const accountDropdownRef = useRef<HTMLDivElement>(null);

  const isCurrent = (path: string) => {
    if (path === "/" && pathname === "/") return true;
    if (path !== "/" && pathname.startsWith(path)) return true;
    return false;
  };

  // Đồng bộ giỏ hàng
  useEffect(() => {
    if (!isAuthenticated) {
      setCartCount(0);
      return;
    }

    let isMounted = true;
    getCartApi()
      .then((cart) => {
        if (isMounted && cart?.items) {
          const count = cart.items.reduce((acc, item) => acc + item.quantity, 0);
          setCartCount(count);
        }
      })
      .catch(() => {
        // Bỏ qua lỗi kết nối dịch vụ giỏ hàng tạm thời
      });

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, pathname]);

  // Đóng menu và mở khóa scroll khi đổi trang
  useEffect(() => {
    setMobileMenuOpen(false);
    setMobileSearchOpen(false);
    setCategoryMenuOpen(false);
    setAccountMenuOpen(false);
  }, [pathname]);

  // Khóa cuộn trang khi drawer mobile đang mở
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileMenuOpen]);

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        categoryDropdownRef.current &&
        !categoryDropdownRef.current.contains(e.target as Node)
      ) {
        setCategoryMenuOpen(false);
      }
      if (
        accountDropdownRef.current &&
        !accountDropdownRef.current.contains(e.target as Node)
      ) {
        setAccountMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      window.location.href = `/search?q=${encodeURIComponent(searchQuery.trim())}`;
    }
  };

  return (
    <header className="w-full bg-[#FAFAF9] sticky top-0 z-50 border-b border-[#E7E5E4] font-sans">
      {/* ============================================================== */}
      {/* 1. DESKTOP & MOBILE TOP BAR                                    */}
      {/* ============================================================== */}
      <div className="w-full px-3.5 sm:px-4 lg:px-5 h-16 flex items-center justify-between gap-3 sm:gap-6">
        {/* Left: Mobile Hamburger Toggle + TECHGEAR Logo */}
        <div className="flex items-center gap-3 shrink-0">
          {/* Mobile Hamburger Button */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(true)}
            className="md:hidden inline-flex items-center justify-center w-10 h-10 rounded-lg text-[#1C1917] hover:bg-[#E7E5E4]/50 transition-colors focus:outline-none focus:ring-2 focus:ring-[#D97706]/40 cursor-pointer"
            aria-label="Mở menu danh mục"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          {/* Logo - Swiss Precision Wordmark */}
          <Link
            href="/"
            className="flex items-center gap-1.5 group select-none py-1"
          >
            <span className="text-xl sm:text-2xl font-black tracking-tight text-[#1C1917] transition-colors">
              TECHGEAR
            </span>
            <span className="w-2 h-2 rounded-full bg-[#D97706] transition-transform group-hover:scale-125" />
          </Link>
        </div>

        {/* Center: Desktop Search Bar (Expands flexibly to fill available width) */}
        <div className="hidden md:flex flex-1 max-w-xl lg:max-w-2xl xl:max-w-3xl mx-3 lg:mx-8">
          <form onSubmit={handleSearch} className="relative w-full">
            <div className="relative flex items-center">
              <div className="absolute left-3.5 pointer-events-none text-[#78716C]">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <input
                type="text"
                placeholder="Tìm laptop, điện thoại, bàn phím cơ, màn hình..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-10 pl-10 pr-24 bg-white hover:bg-white focus:bg-white text-[#1C1917] placeholder:text-[#78716C] text-sm rounded-lg border border-[#E7E5E4] hover:border-[#1C1917]/40 focus:border-[#1C1917] focus:ring-2 focus:ring-[#1C1917]/10 outline-none transition-all"
              />
              <div className="absolute right-1.5 flex items-center gap-1">
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery("")}
                    className="w-6 h-6 rounded-full flex items-center justify-center text-[#A8A29E] hover:text-[#1C1917] hover:bg-[#F5F5F4] transition-colors cursor-pointer"
                    aria-label="Xóa nội dung tìm kiếm"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                )}
                <button
                  type="submit"
                  className="h-7 px-3 bg-[#1C1917] hover:bg-[#292524] active:scale-95 text-white text-xs font-semibold rounded-md transition-all flex items-center gap-1 cursor-pointer focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[#D97706]"
                >
                  <span>Tìm</span>
                </button>
              </div>
            </div>
          </form>
        </div>

        {/* Right: Actions (Mobile Search Button, Cart, Auth) */}
        <div className="flex items-center gap-1.5 sm:gap-2.5 shrink-0">
          {/* Mobile Search Toggle Icon */}
          <button
            type="button"
            onClick={() => setMobileSearchOpen(!mobileSearchOpen)}
            className="md:hidden inline-flex items-center justify-center w-10 h-10 rounded-lg text-[#1C1917] hover:bg-[#E7E5E4]/50 transition-colors cursor-pointer"
            aria-label="Tìm kiếm sản phẩm"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>

          {/* Cart Button */}
          <Link
            href="/cart"
            className="relative flex items-center gap-2 px-2.5 sm:px-3 h-10 rounded-lg text-[#1C1917] hover:bg-[#E7E5E4]/50 transition-colors"
            aria-label="Giỏ hàng"
          >
            <div className="relative flex items-center justify-center">
              <svg className="w-5 h-5 text-[#1C1917] transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.8} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
              </svg>
              {cartCount > 0 && (
                <span className="absolute -top-1.5 -right-2 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-[#D97706] rounded-full border border-white">
                  {cartCount}
                </span>
              )}
            </div>
            <span className="hidden lg:inline text-xs font-semibold text-[#1C1917]">
              Giỏ hàng
            </span>
          </Link>

          {/* User Account Section */}
          {isAuthenticated ? (
            <div className="relative" ref={accountDropdownRef}>
              <button
                type="button"
                onClick={() => setAccountMenuOpen(!accountMenuOpen)}
                className="flex items-center gap-2 h-10 px-2 sm:px-3 rounded-lg border border-[#E7E5E4] hover:border-[#1C1917] hover:bg-white transition-colors cursor-pointer"
              >
                <div className="w-6 h-6 rounded-full bg-[#E7E5E4] text-[#1C1917] flex items-center justify-center font-bold text-xs shrink-0">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </div>
                <div className="hidden lg:flex flex-col text-left leading-tight">
                  <span className="text-[11px] text-[#78716C] font-normal">Tài khoản</span>
                  <span className="text-xs font-semibold text-[#1C1917] truncate max-w-[80px]">Thành viên</span>
                </div>
                <svg className={`w-3.5 h-3.5 text-[#78716C] transition-transform ${accountMenuOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {/* Account Dropdown */}
              {accountMenuOpen && (
                <div className="absolute right-0 top-full mt-1.5 w-52 bg-white border border-[#E7E5E4] rounded-lg shadow-sm py-1.5 z-50">
                  <div className="px-3.5 py-2 border-b border-[#E7E5E4]">
                    <p className="text-xs text-[#78716C]">Đã đăng nhập</p>
                    <p className="text-sm font-semibold text-[#1C1917]">Khách hàng TECHGEAR</p>
                  </div>
                  <Link
                    href="/orders"
                    className="flex items-center gap-2.5 px-3.5 py-2 text-xs font-medium text-[#1C1917] hover:bg-[#FAFAF9] transition-colors"
                  >
                    <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    <span>Đơn hàng của tôi</span>
                  </Link>
                  <Link
                    href="/profile"
                    className="flex items-center gap-2.5 px-3.5 py-2 text-xs font-medium text-[#1C1917] hover:bg-[#FAFAF9] transition-colors"
                  >
                    <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span>Thông tin tài khoản</span>
                  </Link>
                  {isAdmin && (
                    <Link
                      href="/admin"
                      className="flex items-center gap-2.5 px-3.5 py-2 text-xs font-semibold text-[#1C1917] bg-[#FAFAF9] hover:bg-[#E7E5E4] transition-colors"
                    >
                      <svg className="w-4 h-4 text-[#D97706]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span>Quản trị hệ thống</span>
                    </Link>
                  )}
                  <div className="h-px bg-[#E7E5E4] my-1"></div>
                  <button
                    onClick={() => {
                      setAccountMenuOpen(false);
                      logout();
                    }}
                    className="flex items-center gap-2.5 w-full text-left px-3.5 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 transition-colors cursor-pointer"
                  >
                    <svg className="w-4 h-4 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    <span>Đăng xuất</span>
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="flex items-center gap-1.5">
              <Link
                href="/login"
                className="inline-flex items-center justify-center px-3 h-8.5 text-xs font-semibold text-[#1C1917] hover:bg-[#E7E5E4]/50 rounded-lg transition-colors"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="hidden sm:inline-flex items-center justify-center px-3.5 h-8.5 text-xs font-semibold text-white bg-[#1C1917] hover:bg-[#292524] active:bg-black rounded-lg transition-colors"
              >
                Đăng ký
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Mobile Expandable Search Row */}
      {mobileSearchOpen && (
        <div className="md:hidden px-4 pb-3 pt-1 border-t border-[#E7E5E4] bg-[#FAFAF9]">
          <form onSubmit={handleSearch} className="relative w-full">
            <input
              type="text"
              placeholder="Tìm laptop, điện thoại, bàn phím..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
              className="w-full h-10 pl-4 pr-20 bg-white border border-[#E7E5E4] rounded-lg text-sm text-[#1C1917] placeholder:text-[#78716C] focus:border-[#1C1917] focus:ring-2 focus:ring-[#1C1917]/10 outline-none"
            />
            <div className="absolute right-1.5 top-1.5 flex items-center gap-1">
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery("")}
                  className="w-6 h-6 rounded-full flex items-center justify-center text-[#A8A29E] hover:text-[#1C1917] transition-colors cursor-pointer"
                  aria-label="Xóa nội dung tìm kiếm"
                >
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
              <button
                type="submit"
                className="h-7 px-2.5 bg-[#1C1917] text-white rounded text-xs font-semibold cursor-pointer active:scale-95 transition-all"
              >
                Tìm
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ============================================================== */}
      {/* 2. DESKTOP NAVIGATION TIER (Row 2)                              */}
      {/* ============================================================== */}
      <div className="hidden md:block bg-[#FAFAF9] border-t border-[#E7E5E4]">
        <div className="w-full px-3.5 sm:px-4 lg:px-5 h-11 flex items-center justify-between gap-4 text-xs font-medium">
          {/* Left: Category Dropdown Trigger + Main Nav Links */}
          <div className="flex items-center gap-1 lg:gap-2 shrink-0">
            {/* Categories Dropdown */}
            <div className="relative" ref={categoryDropdownRef}>
              <button
                type="button"
                onClick={() => setCategoryMenuOpen(!categoryMenuOpen)}
                className={`flex items-center gap-2 px-3 py-1.5 rounded-md font-semibold text-xs tracking-tight transition-all cursor-pointer ${
                  categoryMenuOpen
                    ? "bg-[#1C1917] text-white"
                    : "bg-white text-[#1C1917] border border-[#E7E5E4] hover:border-[#1C1917]"
                }`}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h7" />
                </svg>
                <span>DANH MỤC SẢN PHẨM</span>
                <svg className={`w-3.5 h-3.5 transition-transform ${categoryMenuOpen ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {/* Categories Flyout Menu */}
              {categoryMenuOpen && (
                <div className="absolute left-0 top-full mt-1.5 w-64 bg-white border border-[#E7E5E4] rounded-lg shadow-sm py-2 z-50">
                  <div className="px-3.5 py-1.5 text-[11px] font-bold text-[#78716C] uppercase tracking-wider">
                    Phân loại thiết bị
                  </div>
                  {TECH_CATEGORIES.map((cat) => (
                    <Link
                      key={cat.id}
                      href={cat.href}
                      className="flex items-center justify-between px-3.5 py-2 text-xs font-medium text-[#1C1917] hover:bg-[#FAFAF9] hover:text-[#D97706] transition-colors"
                    >
                      <span>{cat.name}</span>
                      <svg className="w-3.5 h-3.5 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </Link>
                  ))}
                  <div className="h-px bg-[#E7E5E4] my-1.5" />
                  <Link
                    href="/shop"
                    className="flex items-center justify-between px-3.5 py-2 text-xs font-semibold text-[#1C1917] hover:text-[#D97706] hover:bg-[#FAFAF9] transition-colors"
                  >
                    <span>Xem toàn bộ danh mục</span>
                    <span className="text-[11px]">→</span>
                  </Link>
                </div>
              )}
            </div>

            <div className="h-4 w-px bg-[#E7E5E4] mx-2" />

            {/* Direct Page Links */}
            <Link
              href="/"
              className={`px-3 py-1.5 rounded-md transition-colors ${
                isCurrent("/") && pathname === "/"
                  ? "text-[#1C1917] font-bold bg-[#E7E5E4]/60"
                  : "text-[#78716C] hover:text-[#1C1917] hover:bg-[#E7E5E4]/40"
              }`}
            >
              Trang chủ
            </Link>
            <Link
              href="/shop"
              className={`px-3 py-1.5 rounded-md transition-colors ${
                isCurrent("/shop")
                  ? "text-[#1C1917] font-bold bg-[#E7E5E4]/60"
                  : "text-[#78716C] hover:text-[#1C1917] hover:bg-[#E7E5E4]/40"
              }`}
            >
              Tất cả sản phẩm
            </Link>
            <Link
              href="/search"
              className={`px-3 py-1.5 rounded-md transition-colors ${
                isCurrent("/search")
                  ? "text-[#1C1917] font-bold bg-[#E7E5E4]/60"
                  : "text-[#78716C] hover:text-[#1C1917] hover:bg-[#E7E5E4]/40"
              }`}
            >
              Tìm kiếm nâng cao
            </Link>
          </div>

          {/* Right: Quick Category Shortcuts (Flex across available space, scroll smoothly if tight) */}
          <div className="hidden lg:flex items-center gap-1 xl:gap-2 text-[#78716C] overflow-x-auto hide-scrollbar">
            {TECH_CATEGORIES.slice(0, 8).map((cat) => (
              <Link
                key={cat.id}
                href={cat.href}
                className={`px-2.5 py-1 rounded-md text-xs whitespace-nowrap transition-colors shrink-0 ${
                  isCurrent(cat.href)
                    ? "text-[#1C1917] font-semibold bg-[#E7E5E4]/60"
                    : "hover:text-[#1C1917] hover:bg-[#E7E5E4]/40"
                }`}
              >
                {cat.shortName}
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* ============================================================== */}
      {/* 3. MOBILE NAVIGATION DRAWER (Slideout Menu)                    */}
      {/* ============================================================== */}
      {mobileMenuOpen && (
        <div className="fixed inset-0 z-50 md:hidden flex">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/40 transition-opacity"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden="true"
          />

          {/* Slideout Panel */}
          <div className="relative w-4/5 max-w-sm bg-[#FAFAF9] h-full flex flex-col shadow-xl z-10 animate-in slide-in-from-left duration-200">
            {/* Drawer Header */}
            <div className="px-5 py-4 border-b border-[#E7E5E4] flex items-center justify-between">
              <Link
                href="/"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-1.5"
              >
                <span className="text-xl font-black tracking-tight text-[#1C1917]">
                  TECHGEAR
                </span>
                <span className="w-1.5 h-1.5 rounded-full bg-[#D97706]" />
              </Link>
              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                className="w-9 h-9 rounded-lg flex items-center justify-center text-[#78716C] hover:bg-[#E7E5E4]/50 hover:text-[#1C1917] transition-colors cursor-pointer"
                aria-label="Đóng menu"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Drawer Body - Scrollable */}
            <div className="flex-1 overflow-y-auto px-5 py-4 space-y-6">
              {/* Mobile Search Inside Drawer */}
              <form onSubmit={handleSearch} className="relative">
                <input
                  type="text"
                  placeholder="Tìm sản phẩm..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full h-10 pl-9 pr-14 bg-white border border-[#E7E5E4] rounded-lg text-sm text-[#1C1917] placeholder:text-[#78716C] focus:border-[#1C1917] outline-none"
                />
                <div className="absolute left-3 top-3 text-[#78716C] pointer-events-none">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <button
                  type="submit"
                  className="absolute right-1.5 top-1.5 h-7 px-2.5 bg-[#1C1917] text-white rounded text-xs font-semibold cursor-pointer"
                >
                  Tìm
                </button>
              </form>

              {/* Main Pages */}
              <div>
                <p className="text-[11px] font-bold text-[#78716C] uppercase tracking-wider mb-2">
                  Trang chính
                </p>
                <div className="space-y-1">
                  <Link
                    href="/"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isCurrent("/") && pathname === "/"
                        ? "bg-[#E7E5E4] text-[#1C1917] font-semibold"
                        : "text-[#1C1917] hover:bg-[#E7E5E4]/50"
                    }`}
                  >
                    <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                    </svg>
                    <span>Trang chủ</span>
                  </Link>
                  <Link
                    href="/shop"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isCurrent("/shop")
                        ? "bg-[#E7E5E4] text-[#1C1917] font-semibold"
                        : "text-[#1C1917] hover:bg-[#E7E5E4]/50"
                    }`}
                  >
                    <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
                    </svg>
                    <span>Tất cả sản phẩm</span>
                  </Link>
                  <Link
                    href="/search"
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isCurrent("/search")
                        ? "bg-[#E7E5E4] text-[#1C1917] font-semibold"
                        : "text-[#1C1917] hover:bg-[#E7E5E4]/50"
                    }`}
                  >
                    <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    <span>Tìm kiếm & Bộ lọc</span>
                  </Link>
                </div>
              </div>

              {/* Categories */}
              <div>
                <p className="text-[11px] font-bold text-[#78716C] uppercase tracking-wider mb-2">
                  Danh mục sản phẩm
                </p>
                <div className="space-y-1">
                  {TECH_CATEGORIES.map((cat) => (
                    <Link
                      key={cat.id}
                      href={cat.href}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                        isCurrent(cat.href)
                          ? "bg-[#E7E5E4] text-[#1C1917] font-semibold"
                          : "text-[#1C1917] hover:bg-[#E7E5E4]/50"
                      }`}
                    >
                      <span>{cat.name}</span>
                      <svg className="w-4 h-4 text-[#78716C]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </Link>
                  ))}
                </div>
              </div>
            </div>

            {/* Drawer Footer - User Action */}
            <div className="p-4 border-t border-[#E7E5E4] bg-white">
              {isAuthenticated ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-2.5 px-2 py-1">
                    <div className="w-8 h-8 rounded-full bg-[#1C1917] text-white flex items-center justify-center font-bold text-xs">
                      TG
                    </div>
                    <div>
                      <p className="text-xs text-[#78716C]">Xin chào</p>
                      <p className="text-sm font-semibold text-[#1C1917]">Thành viên TECHGEAR</p>
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-2 pt-1">
                    <Link
                      href="/orders"
                      onClick={() => setMobileMenuOpen(false)}
                      className="text-center py-2 text-xs font-semibold text-[#1C1917] bg-[#FAFAF9] border border-[#E7E5E4] rounded-lg hover:border-[#1C1917]"
                    >
                      Đơn hàng
                    </Link>
                    <Link
                      href="/profile"
                      onClick={() => setMobileMenuOpen(false)}
                      className="text-center py-2 text-xs font-semibold text-[#1C1917] bg-[#FAFAF9] border border-[#E7E5E4] rounded-lg hover:border-[#1C1917]"
                    >
                      Tài khoản
                    </Link>
                  </div>
                  {isAdmin && (
                    <Link
                      href="/admin"
                      onClick={() => setMobileMenuOpen(false)}
                      className="block text-center py-2 text-xs font-semibold text-[#1C1917] bg-[#FAFAF9] border border-[#E7E5E4] rounded-lg hover:border-[#1C1917]"
                    >
                      Quản trị hệ thống
                    </Link>
                  )}
                  <button
                    onClick={() => {
                      setMobileMenuOpen(false);
                      logout();
                    }}
                    className="w-full py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
                  >
                    Đăng xuất
                  </button>
                </div>
              ) : (
                <div className="space-y-2">
                  <div className="grid grid-cols-2 gap-2">
                    <Link
                      href="/login"
                      onClick={() => setMobileMenuOpen(false)}
                      className="text-center py-2.5 text-xs font-semibold text-[#1C1917] bg-[#FAFAF9] border border-[#E7E5E4] rounded-lg hover:bg-white"
                    >
                      Đăng nhập
                    </Link>
                    <Link
                      href="/register"
                      onClick={() => setMobileMenuOpen(false)}
                      className="text-center py-2.5 text-xs font-semibold text-white bg-[#1C1917] rounded-lg hover:bg-[#292524]"
                    >
                      Đăng ký
                    </Link>
                  </div>
                  <p className="text-[11px] text-center text-[#78716C]">
                    Đăng nhập để nhận ưu đãi và quản lý đơn hàng
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
