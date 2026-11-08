"use client";

import React, { useState, useMemo } from "react";
import {
  TicketPercent,
  Plus,
  Search,
  Edit,
  Trash2,
  Calendar,
  CheckCircle2,
  Tag,
  Percent,
  DollarSign,
  Copy,
} from "lucide-react";
import { formatVND } from "@/lib/utils";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";

interface Voucher {
  id: string;
  code: string;
  description: string;
  discountType: "PERCENT" | "FIXED";
  discountValue: number;
  minSpend: number;
  usageLimit: number;
  usedCount: number;
  expiresAt: string;
  active: boolean;
}

const INITIAL_VOUCHERS: Voucher[] = [
  {
    id: "V-1",
    code: "TECHGEAR10",
    description: "Giảm 10% tối đa 1.000.000₫ cho đơn hàng Laptop và Gaming Gear",
    discountType: "PERCENT",
    discountValue: 10,
    minSpend: 5000000,
    usageLimit: 200,
    usedCount: 84,
    expiresAt: "2026-12-31",
    active: true,
  },
  {
    id: "V-2",
    code: "VIPGAMING500K",
    description: "Giảm trực tiếp 500.000₫ cho bàn phím cơ và chuột gaming cao cấp",
    discountType: "FIXED",
    discountValue: 500000,
    minSpend: 2000000,
    usageLimit: 100,
    usedCount: 42,
    expiresAt: "2026-11-30",
    active: true,
  },
  {
    id: "V-3",
    code: "FREESHIP2026",
    description: "Miễn phí vận chuyển toàn quốc cho đơn từ 1.000.000₫",
    discountType: "FIXED",
    discountValue: 50000,
    minSpend: 1000000,
    usageLimit: 500,
    usedCount: 312,
    expiresAt: "2026-10-31",
    active: true,
  },
  {
    id: "V-4",
    code: "WELCOME-TECH",
    description: "Ưu đãi khách hàng mới đăng ký tài khoản TECHGEAR",
    discountType: "PERCENT",
    discountValue: 5,
    minSpend: 500000,
    usageLimit: 1000,
    usedCount: 689,
    expiresAt: "2026-12-31",
    active: true,
  },
];

export default function AdminVouchersPage() {
  const [vouchers, setVouchers] = useState<Voucher[]>(INITIAL_VOUCHERS);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState<Voucher | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Form State
  const [code, setCode] = useState("");
  const [description, setDescription] = useState("");
  const [discountType, setDiscountType] = useState<"PERCENT" | "FIXED">("PERCENT");
  const [discountValue, setDiscountValue] = useState(10);
  const [minSpend, setMinSpend] = useState(1000000);
  const [usageLimit, setUsageLimit] = useState(100);
  const [expiresAt, setExpiresAt] = useState("2026-12-31");

  const openCreate = () => {
    setEditingVoucher(null);
    setCode(`TECH-${Math.random().toString(36).substring(2, 6).toUpperCase()}`);
    setDescription("");
    setDiscountType("PERCENT");
    setDiscountValue(10);
    setMinSpend(1000000);
    setUsageLimit(100);
    setExpiresAt("2026-12-31");
    setDialogOpen(true);
  };

  const handleSaveVoucher = (e: React.FormEvent) => {
    e.preventDefault();
    if (!code.trim()) {
      toast.error("Vui lòng nhập mã khuyến mãi.");
      return;
    }

    if (editingVoucher) {
      setVouchers((prev) =>
        prev.map((v) =>
          v.id === editingVoucher.id
            ? {
                ...v,
                code: code.toUpperCase().trim(),
                description,
                discountType,
                discountValue: Number(discountValue),
                minSpend: Number(minSpend),
                usageLimit: Number(usageLimit),
                expiresAt,
              }
            : v
        )
      );
      toast.success(`Đã cập nhật voucher "${code.toUpperCase()}"!`);
    } else {
      const newV: Voucher = {
        id: `V-${Date.now()}`,
        code: code.toUpperCase().trim(),
        description,
        discountType,
        discountValue: Number(discountValue),
        minSpend: Number(minSpend),
        usageLimit: Number(usageLimit),
        usedCount: 0,
        expiresAt,
        active: true,
      };
      setVouchers([newV, ...vouchers]);
      toast.success(`Đã tạo voucher mới "${code.toUpperCase()}"!`);
    }

    setDialogOpen(false);
  };

  const handleDelete = (id: string, codeName: string) => {
    setVouchers((prev) => prev.filter((v) => v.id !== id));
    toast.success(`Đã xóa voucher "${codeName}"`);
  };

  const copyCode = (codeStr: string) => {
    navigator.clipboard.writeText(codeStr);
    toast.success(`Đã sao chép mã "${codeStr}" vào clipboard!`);
  };

  const filteredVouchers = useMemo(() => {
    return vouchers.filter((v) => {
      return (
        v.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        v.description.toLowerCase().includes(searchTerm.toLowerCase())
      );
    });
  }, [vouchers, searchTerm]);

  const totalPages = Math.ceil(filteredVouchers.length / pageSize) || 1;
  const paginatedVouchers = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredVouchers.slice(start, start + pageSize);
  }, [filteredVouchers, currentPage, pageSize]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Khuyến Mãi & Mã Giảm Giá
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Quản lý chiến dịch ưu đãi, mã giảm giá phần cứng và chương trình trợ giá TECHGEAR.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Mã Đang Hoạt Động
          </span>
          <div className="text-xl font-bold text-foreground mt-1 tabular-nums">
            {vouchers.filter((v) => v.active).length} chương trình
          </div>
          <p className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold mt-0.5">Áp dụng trực tiếp tại giỏ hàng</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Tổng Lượt Sử Dụng
          </span>
          <div className="text-xl font-bold text-blue-600 dark:text-blue-400 mt-1 tabular-nums">
            {vouchers.reduce((acc, v) => acc + v.usedCount, 0)} lượt
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Kích thích tỷ lệ chốt đơn mua</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Chiến Dịch Nổi Bật
          </span>
          <div className="text-xl font-bold text-amber-600 dark:text-amber-500 mt-1 truncate">
            Black Friday & Flash Sale
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Giảm giá thiết bị lên đến 10%</p>
        </Card>
      </div>

      {/* Main Table Card with Integrated Toolbar */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search Input */}
          <div className="relative w-64 sm:w-80">
            <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Tìm mã voucher, mô tả ưu đãi..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-hidden focus:ring-1 focus:ring-ring transition-colors"
            />
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={openCreate}
              className="h-9 px-3 flex items-center gap-1.5 rounded-md bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition-colors shadow-xs cursor-pointer"
            >
              <Plus className="size-3.5" />
              <span>Tạo Mã Mới</span>
            </button>
          </div>
        </div>

        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[160px]">Mã Voucher</TableHead>
                <TableHead>Mô tả chi tiết</TableHead>
                <TableHead>Mức giảm</TableHead>
                <TableHead>Đơn tối thiểu</TableHead>
                <TableHead>Lượt dùng</TableHead>
                <TableHead>Hạn sử dụng</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedVouchers.map((v) => (
                <TableRow key={v.id}>
                  {/* Code */}
                  <TableCell>
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono text-xs font-bold text-amber-600 dark:text-amber-500 bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20">
                        {v.code}
                      </span>
                      <button
                        onClick={() => copyCode(v.code)}
                        className="text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 p-1 transition-colors cursor-pointer"
                        title="Sao chép mã"
                      >
                        <Copy className="size-3" />
                      </button>
                    </div>
                  </TableCell>

                  {/* Description */}
                  <TableCell>
                    <p className="text-xs text-zinc-900 dark:text-zinc-100 font-medium max-w-sm truncate">
                      {v.description}
                    </p>
                  </TableCell>

                  {/* Discount Value */}
                  <TableCell>
                    <span className="font-bold text-xs text-zinc-900 dark:text-zinc-100">
                      {v.discountType === "PERCENT"
                        ? `Giảm ${v.discountValue}%`
                        : `Giảm ${formatVND(v.discountValue)}`}
                    </span>
                  </TableCell>

                  {/* Min Spend */}
                  <TableCell>
                    <span className="text-xs text-zinc-500 font-mono tabular-nums">
                      {formatVND(v.minSpend)}
                    </span>
                  </TableCell>

                  {/* Usage */}
                  <TableCell>
                    <div className="text-xs font-mono">
                      <span className="font-semibold text-zinc-900 dark:text-zinc-100">{v.usedCount}</span>
                      <span className="text-zinc-400"> / {v.usageLimit}</span>
                    </div>
                  </TableCell>

                  {/* Expiry */}
                  <TableCell>
                    <span className="text-xs font-mono text-zinc-500">
                      {v.expiresAt}
                    </span>
                  </TableCell>

                  {/* Actions */}
                  <TableCell className="text-right">
                    <button
                      onClick={() => handleDelete(v.id, v.code)}
                      className="p-1.5 rounded-md border border-zinc-200 dark:border-zinc-800 text-zinc-500 hover:bg-red-500/10 hover:text-red-600 transition-colors cursor-pointer"
                      title="Xóa voucher"
                    >
                      <Trash2 className="size-3.5" />
                    </button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>

        {/* Pagination Footer */}
        {filteredVouchers.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div>
              Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
              <span className="font-semibold text-foreground">
                {Math.min(currentPage * pageSize, filteredVouchers.length)}
              </span>{" "}
              trong tổng số <span className="font-semibold text-foreground">{filteredVouchers.length}</span> mã khuyến mãi
            </div>
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="h-8 px-2.5 rounded-md border border-border bg-background font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer"
              >
                Trước
              </button>
              <span className="px-2 text-foreground font-medium">
                {currentPage} / {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="h-8 px-2.5 rounded-md border border-border bg-background font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </Card>

      {/* CREATE DIALOG */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Tạo Mã Khuyến Mãi Mới</DialogTitle>
            <DialogDescription>
              Thiết lập mã coupon, hình thức giảm và điều kiện áp dụng cho khách hàng.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSaveVoucher} className="space-y-4 pt-2">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Mã Code (In hoa) *</label>
              <input
                type="text"
                required
                placeholder="VD: TECHGEAR20, SALE100K..."
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono uppercase focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Mô tả ưu đãi *</label>
              <textarea
                rows={2}
                required
                placeholder="VD: Giảm 10% tối đa 500k cho khách mua tai nghe và laptop..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Hình thức giảm</label>
                <select
                  value={discountType}
                  onChange={(e) => setDiscountType(e.target.value as any)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
                >
                  <option value="PERCENT">Phần trăm (%)</option>
                  <option value="FIXED">Số tiền cố định (₫)</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">
                  {discountType === "PERCENT" ? "Tỷ lệ giảm (%)" : "Số tiền giảm (₫)"}
                </label>
                <input
                  type="number"
                  min={1}
                  required
                  value={discountValue}
                  onChange={(e) => setDiscountValue(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Đơn tối thiểu (₫)</label>
                <input
                  type="number"
                  min={0}
                  step={50000}
                  required
                  value={minSpend}
                  onChange={(e) => setMinSpend(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Giới hạn số lượt</label>
                <input
                  type="number"
                  min={1}
                  required
                  value={usageLimit}
                  onChange={(e) => setUsageLimit(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Ngày hết hạn</label>
              <input
                type="date"
                required
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>

            <DialogFooter>
              <button
                type="button"
                onClick={() => setDialogOpen(false)}
                className="rounded-lg border border-border px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
              >
                Hủy
              </button>
              <button
                type="submit"
                className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90"
              >
                Tạo mã ngay
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
