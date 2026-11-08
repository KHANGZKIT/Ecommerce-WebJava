"use client";

import React, { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Box,
  Layers,
  ShoppingBag,
  CreditCard,
  Users,
  TicketPercent,
  Store,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  Cpu,
  LogOut,
  ExternalLink,
} from "lucide-react";
import { useAuth } from "@/features/auth/AuthContext";
import { cn } from "@/lib/utils";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";

interface NavItem {
  title: string;
  href: string;
  icon: React.ElementType;
  badge?: string;
  badgeVariant?: "default" | "secondary" | "destructive" | "outline" | "success" | "warning";
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: "TỔNG QUAN",
    items: [
      {
        title: "Dashboard",
        href: "/admin",
        icon: LayoutDashboard,
      },
    ],
  },
  {
    title: "QUẢN LÝ THIẾT BỊ",
    items: [
      {
        title: "Sản phẩm & Cấu hình",
        href: "/admin/products",
        icon: Box,
      },
      {
        title: "Danh mục thiết bị",
        href: "/admin/categories",
        icon: Layers,
        badge: "7 Nhóm",
        badgeVariant: "outline",
      },
    ],
  },
  {
    title: "BÁN HÀNG & GIAO DỊCH",
    items: [
      {
        title: "Đơn đặt hàng",
        href: "/admin/orders",
        icon: ShoppingBag,
      },
      {
        title: "Thanh toán & Giao dịch",
        href: "/admin/payments",
        icon: CreditCard,
      },
    ],
  },
  {
    title: "KHÁCH HÀNG & TIẾP THỊ",
    items: [
      {
        title: "Người dùng & Phân quyền",
        href: "/admin/users",
        icon: Users,
      },
      {
        title: "Vouchers & Khuyến mãi",
        href: "/admin/vouchers",
        icon: TicketPercent,
        badge: "Mới",
        badgeVariant: "warning",
      },
    ],
  },
];

interface AdminSidebarProps {
  collapsed: boolean;
  onToggleCollapse: () => void;
  mobileOpen?: boolean;
  onCloseMobile?: () => void;
}

export function AdminSidebar({
  collapsed,
  onToggleCollapse,
  mobileOpen,
  onCloseMobile,
}: AdminSidebarProps) {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <>
      {/* Mobile Backdrop */}
      {mobileOpen && (
        <div
          onClick={onCloseMobile}
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs md:hidden"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={cn(
          "fixed top-0 bottom-0 left-0 z-50 flex flex-col border-r border-border bg-card transition-all duration-300 ease-in-out md:static",
          collapsed ? "w-18" : "w-64",
          mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        )}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center justify-between px-4 border-b border-border shrink-0">
          <Link
            href="/admin"
            className="flex items-center gap-2.5 overflow-hidden font-bold tracking-tight text-foreground"
          >
            <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground shadow-xs">
              <Cpu className="size-5 text-amber-500" />
            </div>
            {!collapsed && (
              <div className="flex flex-col truncate">
                <span className="text-base font-extrabold tracking-wider text-foreground">
                  TECH<span className="text-amber-600 dark:text-amber-500">GEAR</span>
                </span>
                <span className="text-[10px] font-mono tracking-widest text-muted-foreground uppercase">
                  ADMIN CONSOLE
                </span>
              </div>
            )}
          </Link>

          {/* Desktop Collapse Toggle */}
          <button
            onClick={onToggleCollapse}
            className="hidden md:flex size-7 items-center justify-center rounded-md border border-border text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            title={collapsed ? "Mở rộng thanh điều hướng" : "Thu gọn thanh điều hướng"}
          >
            {collapsed ? <ChevronRight className="size-4" /> : <ChevronLeft className="size-4" />}
          </button>
        </div>

        {/* Navigation Items */}
        <div className="flex-1 overflow-y-auto px-3 py-4 space-y-6">
          {NAV_SECTIONS.map((section) => (
            <div key={section.title} className="space-y-1">
              {!collapsed && (
                <div className="px-3 mb-2 text-[10px] font-bold tracking-wider text-muted-foreground uppercase">
                  {section.title}
                </div>
              )}
              {section.items.map((item) => {
                const isActive =
                  item.href === "/admin"
                    ? pathname === "/admin"
                    : pathname.startsWith(item.href);
                const Icon = item.icon;

                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => {
                      if (onCloseMobile) onCloseMobile();
                    }}
                    className={cn(
                      "flex items-center gap-2.5 rounded-md px-3 py-2 text-xs font-medium transition-all group relative",
                      isActive
                        ? "bg-zinc-900 text-zinc-50 dark:bg-zinc-100 dark:text-zinc-900 font-semibold shadow-xs"
                        : "text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800/60 hover:text-zinc-900 dark:hover:text-zinc-100",
                      collapsed && "justify-center px-0 py-2.5"
                    )}
                    title={collapsed ? item.title : undefined}
                  >
                    <Icon className={cn("size-4 shrink-0", isActive ? "text-amber-500 dark:text-amber-600" : "text-zinc-500 group-hover:text-zinc-900 dark:group-hover:text-zinc-100")} />
                    {!collapsed && (
                      <span className="flex-1 truncate">{item.title}</span>
                    )}
                    {!collapsed && item.badge && (
                      <Badge
                        variant={item.badgeVariant || "outline"}
                        className={cn(
                          "ml-auto text-[10px] px-1.5 py-0 h-4 rounded-md",
                          isActive && "bg-zinc-800 dark:bg-zinc-200 text-zinc-100 dark:text-zinc-900 border-transparent"
                        )}
                      >
                        {item.badge}
                      </Badge>
                    )}
                  </Link>
                );
              })}
            </div>
          ))}
        </div>

        {/* Storefront Link & User Info Footer */}
        <div className="border-t border-zinc-200 dark:border-zinc-800 p-3 space-y-2 shrink-0 bg-zinc-50/50 dark:bg-zinc-900/30">
          {/* Quick link to Storefront */}
          <Link
            href="/shop"
            target="_blank"
            className={cn(
              "flex items-center gap-2.5 rounded-md px-3 py-2 text-xs font-medium text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 hover:text-zinc-900 dark:hover:text-zinc-100 transition-colors border border-dashed border-zinc-200 dark:border-zinc-800",
              collapsed && "justify-center px-0"
            )}
            title="Mở Cửa Hàng TechGear"
          >
            <Store className="size-4 shrink-0 text-amber-600 dark:text-amber-500" />
            {!collapsed && (
              <>
                <span className="flex-1 truncate">Xem Cửa Hàng</span>
                <ExternalLink className="size-3 shrink-0 opacity-50" />
              </>
            )}
          </Link>

          {/* User Account Bar */}
          <div
            className={cn(
              "flex items-center gap-2.5 rounded-md p-2 bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800",
              collapsed && "justify-center p-1.5"
            )}
          >
            <Avatar className="size-7 shrink-0">
              <AvatarFallback className="bg-amber-500/10 text-amber-600 dark:text-amber-400 text-xs font-bold">
                AD
              </AvatarFallback>
            </Avatar>
            {!collapsed && (
              <div className="flex-1 min-w-0">
                <p className="text-xs font-bold truncate text-zinc-900 dark:text-zinc-100">
                  {user?.firstName ? `${user.firstName} ${user.lastName || ""}` : "Quản Trị Viên"}
                </p>
                <p className="text-[10px] text-zinc-500 truncate">
                  {user?.email || "admin@techgear.com"}
                </p>
              </div>
            )}
            {!collapsed && (
              <button
                onClick={() => logout()}
                className="text-zinc-400 hover:text-red-500 p-1 rounded-md transition-colors cursor-pointer"
                title="Đăng xuất"
              >
                <LogOut className="size-3.5" />
              </button>
            )}
          </div>
        </div>
      </aside>
    </>
  );
}
