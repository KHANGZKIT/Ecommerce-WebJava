"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Menu,
  Search,
  Bell,
  Store,
  LogOut,
  User,
  Shield,
  ChevronRight,
  Package,
  Layers,
  ShoppingBag,
  CreditCard,
  Users,
  TicketPercent,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  X,
} from "lucide-react";
import { useAuth } from "@/features/auth/AuthContext";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface AdminHeaderProps {
  onToggleMobile: () => void;
}

const ROUTE_LABELS: Record<string, string> = {
  "/admin": "Tổng quan Dashboard",
  "/admin/products": "Quản lý Sản phẩm & Cấu hình",
  "/admin/categories": "Quản lý Danh mục",
  "/admin/orders": "Quản lý Đơn đặt hàng",
  "/admin/payments": "Quản lý Giao dịch & Thanh toán",
  "/admin/users": "Quản lý Người dùng & Phân quyền",
  "/admin/vouchers": "Khuyến mãi & Vouchers",
};

const COMMAND_NAV_ITEMS = [
  { title: "Tổng quan Dashboard", href: "/admin", icon: Shield, group: "Điều hướng" },
  { title: "Sản phẩm & Cấu hình phần cứng", href: "/admin/products", icon: Package, group: "Quản lý" },
  { title: "Danh mục thiết bị", href: "/admin/categories", icon: Layers, group: "Quản lý" },
  { title: "Đơn đặt hàng thiết bị", href: "/admin/orders", icon: ShoppingBag, group: "Giao dịch" },
  { title: "Thanh toán & Giao dịch", href: "/admin/payments", icon: CreditCard, group: "Giao dịch" },
  { title: "Người dùng & Khách hàng", href: "/admin/users", icon: Users, group: "Tài khoản" },
  { title: "Khuyến mãi & Vouchers", href: "/admin/vouchers", icon: TicketPercent, group: "Tiếp thị" },
  { title: "Cửa hàng TechGear (Storefront)", href: "/shop", icon: Store, group: "Cửa hàng" },
];

const RECENT_ALERTS = [
  {
    id: "alt-1",
    type: "warning",
    title: "Cảnh báo tồn kho thấp",
    desc: "MacBook Pro 16 M3 Max chỉ còn 3 chiếc trong kho.",
    time: "10 phút trước",
  },
  {
    id: "alt-2",
    type: "info",
    title: "Đơn hàng mới #ORD-9421",
    desc: "Khách hàng Mẫu vừa đặt đơn trị giá 82.500.000₫.",
    time: "25 phút trước",
  },
  {
    id: "alt-3",
    type: "success",
    title: "Thanh toán VNPay hoàn tất",
    desc: "Giao dịch TXN-9021-VNPAY đã được hạch toán thành công.",
    time: "1 giờ trước",
  },
];

export function AdminHeader({ onToggleMobile }: AdminHeaderProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();

  const [commandOpen, setCommandOpen] = useState(false);
  const [commandQuery, setCommandQuery] = useState("");
  const [alerts, setAlerts] = useState(RECENT_ALERTS);

  // Keyboard shortcut Cmd+K / Ctrl+K
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setCommandOpen((prev) => !prev);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const filteredCommands = COMMAND_NAV_ITEMS.filter((item) =>
    item.title.toLowerCase().includes(commandQuery.toLowerCase())
  );

  const handleSelectCommand = (href: string) => {
    setCommandOpen(false);
    setCommandQuery("");
    if (href.startsWith("/shop")) {
      window.open(href, "_blank");
    } else {
      router.push(href);
    }
  };

  const currentLabel = ROUTE_LABELS[pathname] || "Khu vực Quản trị";

  return (
    <>
      <header className="sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border bg-background/95 px-4 md:px-6 backdrop-blur-md">
        {/* Left: Mobile Toggle & Breadcrumbs */}
        <div className="flex items-center gap-3">
          <button
            onClick={onToggleMobile}
            className="flex md:hidden size-8 items-center justify-center rounded-md border border-border text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
            aria-label="Mở menu"
          >
            <Menu className="size-4" />
          </button>

          <nav className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Link
              href="/admin"
              className="hover:text-foreground transition-colors font-medium"
            >
              Admin
            </Link>
            <ChevronRight className="size-3.5 opacity-40 shrink-0" />
            <span className="font-semibold text-foreground truncate max-w-[160px] sm:max-w-none">
              {currentLabel}
            </span>
          </nav>
        </div>

        {/* Right: Search Command Trigger, Notifications & User Avatar */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Command Search Trigger */}
          <button
            onClick={() => setCommandOpen(true)}
            className="flex items-center gap-2 rounded-md border border-border bg-muted/40 px-2.5 py-1.5 text-xs text-muted-foreground hover:border-border hover:bg-muted hover:text-foreground transition-colors w-36 sm:w-56 lg:w-64"
          >
            <Search className="size-3.5 shrink-0" />
            <span className="flex-1 text-left truncate text-xs">Tìm kiếm nhanh...</span>
            <kbd className="pointer-events-none hidden sm:inline-flex h-4.5 select-none items-center gap-0.5 rounded border border-border bg-background px-1.5 font-mono text-[10px] font-medium text-muted-foreground">
              <span className="text-[11px]">⌘</span>K
            </kbd>
          </button>

          {/* Notification Popover */}
          <Popover>
            <PopoverTrigger asChild>
              <button
                className="relative size-8 flex items-center justify-center rounded-md border border-border text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
                title="Thông báo hệ thống"
              >
                <Bell className="size-4" />
                {alerts.length > 0 && (
                  <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-amber-500 ring-2 ring-background" />
                )}
              </button>
            </PopoverTrigger>
            <PopoverContent align="end" className="w-80 p-0 shadow-lg">
              <div className="flex items-center justify-between border-b border-border px-4 py-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-foreground">
                    Thông báo hệ thống
                  </span>
                  <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-4">
                    {alerts.length} mới
                  </Badge>
                </div>
                {alerts.length > 0 && (
                  <button
                    onClick={() => setAlerts([])}
                    className="text-[11px] text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Đã đọc
                  </button>
                )}
              </div>

              <div className="max-h-72 overflow-y-auto divide-y divide-border">
                {alerts.length === 0 ? (
                  <div className="py-8 text-center text-xs text-muted-foreground">
                    Không có thông báo mới nào
                  </div>
                ) : (
                  alerts.map((alt) => (
                    <div key={alt.id} className="p-3 hover:bg-muted/50 transition-colors">
                      <div className="flex items-start gap-2.5">
                        {alt.type === "warning" && (
                          <AlertTriangle className="size-4 text-amber-500 shrink-0 mt-0.5" />
                        )}
                        {alt.type === "info" && (
                          <ShoppingBag className="size-4 text-blue-500 shrink-0 mt-0.5" />
                        )}
                        {alt.type === "success" && (
                          <CheckCircle2 className="size-4 text-emerald-500 shrink-0 mt-0.5" />
                        )}
                        <div className="flex-1 min-w-0">
                          <p className="text-xs font-semibold text-foreground">
                            {alt.title}
                          </p>
                          <p className="text-[11px] text-muted-foreground mt-0.5 leading-snug">
                            {alt.desc}
                          </p>
                          <span className="text-[10px] text-muted-foreground mt-1 block">
                            {alt.time}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </PopoverContent>
          </Popover>

          {/* User Avatar Dropdown */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex items-center gap-2 rounded-full p-0.5 outline-hidden ring-offset-background hover:ring-2 hover:ring-muted-foreground/30 transition-all cursor-pointer">
                <Avatar className="size-8">
                  <AvatarFallback className="bg-primary text-primary-foreground text-xs font-bold">
                    {user?.firstName ? user.firstName[0].toUpperCase() : "A"}
                  </AvatarFallback>
                </Avatar>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-56">
              <DropdownMenuLabel className="font-normal">
                <div className="flex flex-col space-y-1">
                  <p className="text-xs font-bold leading-none text-foreground">
                    {user?.firstName ? `${user.firstName} ${user.lastName || ""}` : "Quản Trị Viên"}
                  </p>
                  <p className="text-[11px] leading-none text-muted-foreground">
                    {user?.email || "admin@techgear.com"}
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href="/admin" className="cursor-pointer">
                  <Shield className="size-4 mr-2 text-amber-500" />
                  <span>Hồ sơ & Cài đặt</span>
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <Link href="/shop" target="_blank" className="cursor-pointer">
                  <Store className="size-4 mr-2" />
                  <span>Về trang Cửa hàng</span>
                </Link>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => logout()}
                variant="destructive"
                className="cursor-pointer"
              >
                <LogOut className="size-4 mr-2" />
                <span>Đăng xuất</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      {/* Command Palette Modal */}
      <Dialog open={commandOpen} onOpenChange={setCommandOpen}>
        <DialogContent className="p-0 max-w-lg overflow-hidden border border-border shadow-2xl">
          <div className="flex items-center border-b border-border px-3 py-2.5">
            <Search className="size-4 text-muted-foreground mr-2 shrink-0" />
            <input
              type="text"
              placeholder="Nhập tên trang hoặc chức năng cần tìm..."
              value={commandQuery}
              onChange={(e) => setCommandQuery(e.target.value)}
              className="flex-1 bg-transparent text-xs text-foreground placeholder:text-muted-foreground outline-none"
              autoFocus
            />
            {commandQuery && (
              <button
                onClick={() => setCommandQuery("")}
                className="text-muted-foreground hover:text-foreground p-0.5"
              >
                <X className="size-3.5" />
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto p-2 space-y-1">
            {filteredCommands.length === 0 ? (
              <div className="py-6 text-center text-xs text-muted-foreground">
                Không tìm thấy kết quả phù hợp cho &quot;{commandQuery}&quot;
              </div>
            ) : (
              filteredCommands.map((cmd) => {
                const Icon = cmd.icon;
                return (
                  <button
                    key={cmd.href}
                    onClick={() => handleSelectCommand(cmd.href)}
                    className="flex w-full items-center justify-between rounded-md px-3 py-2 text-xs font-medium text-foreground hover:bg-muted transition-colors text-left"
                  >
                    <div className="flex items-center gap-2.5">
                      <Icon className="size-4 text-muted-foreground shrink-0" />
                      <span>{cmd.title}</span>
                    </div>
                    <span className="text-[10px] font-mono text-muted-foreground uppercase">
                      {cmd.group}
                    </span>
                  </button>
                );
              })
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
