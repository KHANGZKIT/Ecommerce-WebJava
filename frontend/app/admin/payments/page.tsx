"use client";

import React, { useState, useMemo } from "react";
import {
  CreditCard,
  Search,
  CheckCircle2,
  XCircle,
  RotateCcw,
  RefreshCw,
  DollarSign,
  AlertCircle,
  Calendar,
  Lock,
} from "lucide-react";
import { formatVND, formatDateVI } from "@/lib/utils";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PaymentResponse, PaymentMethod, PaymentStatus } from "@/types/payment";

const INITIAL_PAYMENTS: PaymentResponse[] = [
  {
    id: 501,
    transactionId: "TXN-9021-VNPAY",
    orderId: 1042,
    userId: 34,
    amount: 84990000,
    paymentMethod: "CREDIT_CARD",
    status: "SUCCESS",
    message: "Giao dịch thẻ Visa Techcombank thành công qua VNPay Gateway",
    createdAt: new Date(Date.now() - 1000 * 60 * 35).toISOString(),
  },
  {
    id: 500,
    transactionId: "TXN-8842-MOMO",
    orderId: 1041,
    userId: 33,
    amount: 18450000,
    paymentMethod: "WALLET",
    status: "SUCCESS",
    message: "Thanh toán ví MoMo quét mã QR hoàn tất",
    createdAt: new Date(Date.now() - 1000 * 60 * 180).toISOString(),
  },
  {
    id: 499,
    transactionId: "TXN-7612-CARD",
    orderId: 1040,
    userId: 35,
    amount: 8990000,
    paymentMethod: "DEBIT_CARD",
    status: "SUCCESS",
    message: "Thẻ ghi nợ nội địa Napas xác thực OTP",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
  },
  {
    id: 498,
    transactionId: "TXN-6512-FAIL",
    orderId: 1037,
    userId: 32,
    amount: 32500000,
    paymentMethod: "CREDIT_CARD",
    status: "FAILED",
    message: "Thẻ vượt quá hạn mức thanh toán trong ngày",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 36).toISOString(),
  },
  {
    id: 497,
    transactionId: "TXN-5411-REF",
    orderId: 1036,
    userId: 31,
    amount: 4500000,
    paymentMethod: "WALLET",
    status: "REFUNDED",
    message: "Hoàn tiền cho khách theo yêu cầu đổi sản phẩm",
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 72).toISOString(),
  },
];

export default function AdminPaymentsPage() {
  const [payments, setPayments] = useState<PaymentResponse[]>(INITIAL_PAYMENTS);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Refund Modal State
  const [refundDialogOpen, setRefundDialogOpen] = useState(false);
  const [paymentToRefund, setPaymentToRefund] = useState<PaymentResponse | null>(null);
  const [refundReason, setRefundReason] = useState("");
  const [isRefunding, setIsRefunding] = useState(false);

  const handleProcessRefund = async () => {
    if (!paymentToRefund) return;
    setIsRefunding(true);
    try {
      // Simulate refund API call
      await new Promise((r) => setTimeout(r, 600));
      setPayments((prev) =>
        prev.map((p) =>
          p.id === paymentToRefund.id
            ? { ...p, status: "REFUNDED", message: `Hoàn tiền: ${refundReason || "Theo yêu cầu khách hàng"}` }
            : p
        )
      );
      toast.success(`Đã hoàn tiền ${formatVND(paymentToRefund.amount)} cho giao dịch ${paymentToRefund.transactionId}!`);
      setRefundDialogOpen(false);
      setPaymentToRefund(null);
      setRefundReason("");
    } catch (err: any) {
      toast.error("Hoàn tiền thất bại: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsRefunding(false);
    }
  };

  const filteredPayments = payments.filter((p) => {
    const matchSearch =
      p.transactionId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.orderId.toString().includes(searchTerm) ||
      (p.message && p.message.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchStatus = statusFilter === "ALL" || p.status === statusFilter;

    return matchSearch && matchStatus;
  });

  const getStatusBadge = (status: PaymentStatus) => {
    switch (status) {
      case "SUCCESS":
        return <Badge variant="success">Thành công</Badge>;
      case "PENDING":
        return <Badge variant="warning">Chờ xử lý</Badge>;
      case "FAILED":
        return <Badge variant="destructive">Thất bại</Badge>;
      case "REFUNDED":
        return <Badge variant="outline">Đã hoàn tiền</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const totalPages = Math.ceil(filteredPayments.length / pageSize) || 1;
  const paginatedPayments = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredPayments.slice(start, start + pageSize);
  }, [filteredPayments, currentPage, pageSize]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Giao Dịch & Cổng Thanh Toán
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Nhật ký giao dịch trực tuyến, đối soát thanh toán và xử lý yêu cầu hoàn tiền.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Card className="p-3.5 border border-zinc-200 dark:border-zinc-800 shadow-xs">
          <span className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">
            Tổng Tiền Đã Thanh Toán
          </span>
          <div className="text-xl font-bold text-zinc-900 dark:text-zinc-100 mt-1 tabular-nums">
            {formatVND(112430000)}
          </div>
          <p className="text-[11px] text-emerald-600 dark:text-emerald-400 font-semibold mt-0.5">98.2% giao dịch thành công</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Tiền Đã Hoàn Trả (Refund)
          </span>
          <div className="text-xl font-bold text-amber-600 dark:text-amber-500 mt-1 tabular-nums">
            {formatVND(4500000)}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">1 giao dịch hoàn trả tháng này</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Cổng Thanh Toán Hỗ Trợ
          </span>
          <div className="text-xl font-bold text-blue-600 dark:text-blue-400 mt-1">
            Visa / MoMo / VNPay
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Bảo mật chuẩn quốc tế 3D-Secure</p>
        </Card>
      </div>

      {/* Main Table Card with Integrated Toolbar */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search & Filters */}
          <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
            <div className="relative w-64 sm:w-80">
              <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Tìm mã giao dịch, mã đơn hàng, ghi chú..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-hidden focus:ring-1 focus:ring-ring transition-colors"
              />
            </div>

            <div className="w-44 sm:w-52">
              <Select
                value={statusFilter}
                onValueChange={(val) => {
                  setStatusFilter(val);
                  setCurrentPage(1);
                }}
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Lọc trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả trạng thái</SelectItem>
                  <SelectItem value="SUCCESS">Thành công (SUCCESS)</SelectItem>
                  <SelectItem value="PENDING">Chờ xử lý (PENDING)</SelectItem>
                  <SelectItem value="FAILED">Thất bại (FAILED)</SelectItem>
                  <SelectItem value="REFUNDED">Đã hoàn tiền (REFUNDED)</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => toast.info("Đã đồng bộ dữ liệu cổng thanh toán payment-service.")}
              title="Đối soát dữ liệu"
              className="h-9 px-3 flex items-center gap-1.5 rounded-md border border-border bg-background text-muted-foreground hover:bg-muted hover:text-foreground transition-colors text-xs font-medium cursor-pointer"
            >
              <RefreshCw className="size-3.5" />
              <span>Đối soát</span>
            </button>
          </div>
        </div>

        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[140px]">Mã giao dịch</TableHead>
                <TableHead>Mã đơn</TableHead>
                <TableHead>Phương thức</TableHead>
                <TableHead>Số tiền</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Ghi chú đối soát</TableHead>
                <TableHead>Thời gian</TableHead>
                <TableHead className="text-right">Hoàn tiền</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {paginatedPayments.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>
                    <span className="font-mono text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                      {p.transactionId}
                    </span>
                  </TableCell>
                  <TableCell>
                    <span className="font-mono text-xs font-semibold text-amber-600 dark:text-amber-500">
                      #{p.orderId}
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-[10px] font-mono">
                      {p.paymentMethod}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <span className="font-bold text-xs text-zinc-900 dark:text-zinc-100 tabular-nums">
                      {formatVND(p.amount)}
                    </span>
                  </TableCell>
                  <TableCell>{getStatusBadge(p.status)}</TableCell>
                  <TableCell>
                    <span className="text-xs text-zinc-500 truncate max-w-xs block">
                      {p.message || "—"}
                    </span>
                  </TableCell>
                  <TableCell>
                    <span className="text-[11px] text-zinc-500 whitespace-nowrap">
                      {formatDateVI(p.createdAt)}
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    {p.status === "SUCCESS" ? (
                      <button
                        onClick={() => {
                          setPaymentToRefund(p);
                          setRefundDialogOpen(true);
                        }}
                        className="inline-flex items-center gap-1 rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 px-2 py-1 text-xs font-medium text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-850 hover:text-zinc-900 transition-colors cursor-pointer"
                      >
                        <RotateCcw className="size-3 text-amber-500" />
                        <span>Hoàn tiền</span>
                      </button>
                    ) : (
                      <span className="text-xs text-zinc-400">—</span>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>

        {/* Pagination Footer */}
        {filteredPayments.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div>
              Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
              <span className="font-semibold text-foreground">
                {Math.min(currentPage * pageSize, filteredPayments.length)}
              </span>{" "}
              trong tổng số <span className="font-semibold text-foreground">{filteredPayments.length}</span> giao dịch
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

      {/* REFUND DIALOG */}
      <Dialog open={refundDialogOpen} onOpenChange={setRefundDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Xác Nhận Hoàn Tiền Giao Dịch</DialogTitle>
            <DialogDescription>
              Hoàn tiền số tiền <strong>{formatVND(paymentToRefund?.amount)}</strong> về tài khoản khách hàng qua cổng {paymentToRefund?.paymentMethod}.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 pt-2">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Lý do hoàn tiền *</label>
              <textarea
                rows={3}
                placeholder="Ví dụ: Khách đổi sang dòng máy khác, lỗi phần cứng bảo hành..."
                value={refundReason}
                onChange={(e) => setRefundReason(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>
          </div>

          <DialogFooter>
            <button
              onClick={() => setRefundDialogOpen(false)}
              className="rounded-lg border border-border px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
            >
              Hủy
            </button>
            <button
              onClick={handleProcessRefund}
              disabled={isRefunding}
              className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {isRefunding ? "Đang xử lý..." : "Xác nhận hoàn tiền"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
