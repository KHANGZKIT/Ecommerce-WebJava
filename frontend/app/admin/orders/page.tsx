"use client";

import React, { useState, useEffect, useCallback } from "react";
import {
  Search,
  Eye,
  Package,
  RefreshCw,
  AlertTriangle,
} from "lucide-react";
import { getAdminOrdersApi, updateAdminOrderStatusApi } from "@/lib/api/admin";
import { formatVND, formatDateVI } from "@/lib/utils";
import { toast } from "sonner";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
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
import { OrderDto, OrderStatus } from "@/types/order";

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  // Detail Modal State
  const [selectedOrder, setSelectedOrder] = useState<OrderDto | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setCurrentPage(1);
    }, 350);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Fetch orders from authoritative backend API
  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdminOrdersApi({
        page: currentPage - 1,
        size: pageSize,
        status: statusFilter !== "ALL" ? statusFilter : undefined,
        search: debouncedSearch.trim() || undefined,
      });
      setOrders(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: any) {
      console.error("Failed to load admin orders", err);
      const msg = err.message || "Không thể kết nối đến máy chủ lấy danh sách đơn hàng.";
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, statusFilter, debouncedSearch]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleUpdateStatus = async (orderId: number, nextStatus: OrderStatus) => {
    setIsUpdatingStatus(true);
    try {
      const updated = await updateAdminOrderStatusApi(orderId, { status: nextStatus });
      setOrders((prev) =>
        prev.map((o) => (o.id === orderId ? { ...o, status: nextStatus } : o))
      );
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder((prev) => (prev ? { ...prev, status: nextStatus } : null));
      }
      toast.success(`Đã cập nhật trạng thái đơn #${orderId} sang "${nextStatus}"`);
      // Refresh to ensure database consistency
      fetchOrders();
    } catch (err: any) {
      toast.error("Không thể cập nhật trạng thái đơn: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case "DELIVERED":
        return <Badge variant="success">Hoàn thành / Đã giao</Badge>;
      case "SHIPPED":
        return <Badge variant="default">Đang vận chuyển</Badge>;
      case "CONFIRMED":
        return <Badge variant="info">Đã xác nhận</Badge>;
      case "PENDING":
        return <Badge variant="warning">Chờ xử lý</Badge>;
      case "PAYMENT_FAILED":
        return <Badge variant="destructive">Thanh toán thất bại</Badge>;
      case "CANCELLED":
        return <Badge variant="destructive">Đã hủy đơn</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Quản Lý Đơn Đặt Hàng
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Dữ liệu đơn hàng thực tế từ hệ thống — theo dõi tiến độ, đối soát giao dịch và cập nhật trạng thái.
          </p>
        </div>
      </div>

      {/* Main Table Card with Integrated Toolbar */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search & Filters */}
          <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
            {/* Search */}
            <div className="relative w-64 sm:w-80">
              <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Tìm mã đơn, địa chỉ giao hàng, mã giao dịch..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-hidden focus:ring-1 focus:ring-ring transition-colors"
              />
            </div>

            {/* Status Filter */}
            <div className="w-48 sm:w-52">
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
                  <SelectItem value="PENDING">Chờ xử lý (PENDING)</SelectItem>
                  <SelectItem value="CONFIRMED">Đã xác nhận (CONFIRMED)</SelectItem>
                  <SelectItem value="SHIPPED">Đang giao (SHIPPED)</SelectItem>
                  <SelectItem value="DELIVERED">Đã giao (DELIVERED)</SelectItem>
                  <SelectItem value="PAYMENT_FAILED">Thất bại (PAYMENT_FAILED)</SelectItem>
                  <SelectItem value="CANCELLED">Đã hủy (CANCELLED)</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => fetchOrders()}
              disabled={loading}
              title="Làm mới danh sách"
              className="h-9 px-3 flex items-center gap-1.5 rounded-md border border-border bg-background text-xs font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-50 cursor-pointer shadow-xs"
            >
              <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
              <span>Đồng bộ</span>
            </button>
          </div>
        </div>

        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : error ? (
            <div className="py-12 text-center px-4">
              <AlertTriangle className="mx-auto size-10 text-amber-500 mb-3" />
              <h3 className="text-sm font-bold text-foreground">Không thể tải dữ liệu đơn hàng</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">{error}</p>
              <button
                onClick={() => fetchOrders()}
                className="mt-4 px-3 py-1.5 rounded-md bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition-colors"
              >
                Thử lại
              </button>
            </div>
          ) : orders.length === 0 ? (
            <div className="py-16 text-center">
              <Package className="mx-auto size-10 text-muted-foreground/40 mb-3" />
              <h3 className="text-sm font-bold text-foreground">Không có đơn hàng nào</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
                Không tìm thấy đơn hàng phù hợp với điều kiện tìm kiếm hoặc cơ sở dữ liệu chưa có đơn hàng.
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[90px]">Mã đơn</TableHead>
                  <TableHead>Khách hàng & Địa chỉ</TableHead>
                  <TableHead>Thiết bị đặt mua</TableHead>
                  <TableHead>Tổng tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Thời gian</TableHead>
                  <TableHead className="text-right">Chi tiết</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orders.map((ord) => (
                  <TableRow key={ord.id}>
                    {/* Order ID */}
                    <TableCell>
                      <span className="font-mono text-xs font-semibold text-foreground">
                        #{ord.id}
                      </span>
                    </TableCell>

                    {/* Customer & Address */}
                    <TableCell>
                      <div className="max-w-xs">
                        <p className="text-xs font-semibold text-foreground">
                          Khách hàng #{ord.userId}
                        </p>
                        <p className="text-[11px] text-muted-foreground truncate mt-0.5" title={ord.shippingAddress}>
                          {ord.shippingAddress}
                        </p>
                      </div>
                    </TableCell>

                    {/* Items */}
                    <TableCell>
                      <div className="max-w-xs">
                        <p className="text-xs font-semibold text-foreground truncate">
                          {ord.items && ord.items.length > 0
                            ? ord.items[0]?.productName || "Sản phẩm công nghệ"
                            : "Chi tiết trong đơn"}
                        </p>
                        {ord.items && ord.items.length > 1 && (
                          <span className="text-[10px] text-muted-foreground font-medium">
                            + {ord.items.length - 1} thiết bị khác
                          </span>
                        )}
                      </div>
                    </TableCell>

                    {/* Total Amount */}
                    <TableCell>
                      <span className="font-bold text-xs text-foreground tabular-nums">
                        {formatVND(ord.totalAmount)}
                      </span>
                    </TableCell>

                    {/* Status Badge */}
                    <TableCell>{getStatusBadge(ord.status)}</TableCell>

                    {/* Time */}
                    <TableCell>
                      <span className="text-[11px] text-muted-foreground whitespace-nowrap">
                        {formatDateVI(ord.createdAt)}
                      </span>
                    </TableCell>

                    {/* Action */}
                    <TableCell className="text-right">
                      <button
                        onClick={() => {
                          setSelectedOrder(ord);
                          setDetailOpen(true);
                        }}
                        className="inline-flex items-center gap-1 rounded-md border border-border bg-background px-2.5 py-1 text-xs font-medium text-foreground hover:bg-muted transition-colors cursor-pointer shadow-2xs"
                      >
                        <Eye className="size-3.5 text-muted-foreground" />
                        <span>Xem</span>
                      </button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>

        {/* Pagination Footer */}
        {!loading && !error && orders.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div>
              Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
              <span className="font-semibold text-foreground">
                {Math.min(currentPage * pageSize, totalElements)}
              </span>{" "}
              trong tổng số <span className="font-semibold text-foreground">{totalElements}</span> đơn hàng
            </div>
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="h-8 px-2.5 rounded-md border border-border bg-background font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer shadow-2xs"
              >
                Trước
              </button>
              <span className="px-2 text-foreground font-medium">
                {currentPage} / {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="h-8 px-2.5 rounded-md border border-border bg-background font-medium text-foreground hover:bg-muted disabled:opacity-40 disabled:pointer-events-none transition-colors cursor-pointer shadow-2xs"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </Card>

      {/* ORDER DETAIL DIALOG */}
      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <div className="flex items-center justify-between pr-6">
              <DialogTitle className="text-base font-extrabold flex items-center gap-2">
                <span>Chi Tiết Đơn Hàng #{selectedOrder?.id}</span>
                {selectedOrder && getStatusBadge(selectedOrder.status)}
              </DialogTitle>
            </div>
            <DialogDescription className="text-xs">
              Thời gian tạo đơn: {formatDateVI(selectedOrder?.createdAt)}
            </DialogDescription>
          </DialogHeader>

          {selectedOrder && (
            <div className="space-y-4 pt-2">
              {/* Customer & Shipping Box */}
              <div className="rounded-lg border border-border bg-muted/20 p-3.5 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="font-bold text-muted-foreground uppercase text-[10px] block mb-1">
                    Địa chỉ nhận hàng:
                  </span>
                  <p className="text-foreground leading-relaxed">
                    {selectedOrder.shippingAddress}
                  </p>
                </div>
                <div>
                  <span className="font-bold text-muted-foreground uppercase text-[10px] block mb-1">
                    Ghi chú từ khách hàng:
                  </span>
                  <p className="text-foreground italic">
                    "{selectedOrder.notes || "Không có ghi chú thêm."}"
                  </p>
                  {selectedOrder.paymentTransactionId && (
                    <div className="mt-2 text-[11px] font-mono text-muted-foreground">
                      Mã thanh toán: <strong className="text-foreground">{selectedOrder.paymentTransactionId}</strong>
                    </div>
                  )}
                </div>
              </div>

              {/* Itemized Hardware Table */}
              <div className="space-y-2">
                <span className="text-xs font-bold text-foreground uppercase tracking-wide">
                  Danh sách thiết bị & linh kiện:
                </span>
                <div className="rounded-lg border border-border overflow-hidden">
                  <Table>
                    <TableHeader>
                      <TableRow className="bg-muted/40">
                        <TableHead>Tên sản phẩm</TableHead>
                        <TableHead>SKU</TableHead>
                        <TableHead className="text-right">Đơn giá</TableHead>
                        <TableHead className="text-center">Số lượng</TableHead>
                        <TableHead className="text-right">Thành tiền</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {selectedOrder.items && selectedOrder.items.length > 0 ? (
                        selectedOrder.items.map((item, idx) => (
                          <TableRow key={idx}>
                            <TableCell className="font-semibold text-xs text-foreground">
                              {item.productName}
                            </TableCell>
                            <TableCell className="font-mono text-xs text-muted-foreground">
                              {item.productSku}
                            </TableCell>
                            <TableCell className="text-right font-mono text-xs tabular-nums text-foreground">
                              {formatVND(item.unitPrice)}
                            </TableCell>
                            <TableCell className="text-center font-bold text-xs text-foreground">
                              x{item.quantity}
                            </TableCell>
                            <TableCell className="text-right font-bold text-xs tabular-nums text-foreground">
                              {formatVND(item.subtotal)}
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={5} className="text-center text-xs text-muted-foreground py-4">
                            Không có chi tiết sản phẩm hoặc đang đồng bộ
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </div>

                {/* Total */}
                <div className="flex justify-end pt-2 text-right">
                  <div>
                    <span className="text-xs text-muted-foreground mr-3">Tổng cộng thanh toán:</span>
                    <span className="text-base font-extrabold text-foreground tabular-nums">
                      {formatVND(selectedOrder.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Quick Status Action Buttons */}
              <div className="pt-2 border-t border-border">
                <span className="text-[11px] font-bold text-muted-foreground uppercase block mb-2">
                  Cập nhật trạng thái xử lý đơn hàng:
                </span>
                <div className="flex flex-wrap gap-2">
                  {selectedOrder.status !== "CONFIRMED" && selectedOrder.status !== "DELIVERED" && (
                    <button
                      onClick={() => handleUpdateStatus(selectedOrder.id, "CONFIRMED")}
                      disabled={isUpdatingStatus}
                      className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs font-bold text-blue-600 dark:text-blue-400 hover:bg-blue-500/20 transition-colors disabled:opacity-50 cursor-pointer"
                    >
                      Xác Nhận Đơn (CONFIRMED)
                    </button>
                  )}

                  {selectedOrder.status !== "SHIPPED" && selectedOrder.status !== "DELIVERED" && (
                    <button
                      onClick={() => handleUpdateStatus(selectedOrder.id, "SHIPPED")}
                      disabled={isUpdatingStatus}
                      className="rounded-md border border-purple-500/30 bg-purple-500/10 px-3 py-1.5 text-xs font-bold text-purple-600 dark:text-purple-400 hover:bg-purple-500/20 transition-colors disabled:opacity-50 cursor-pointer"
                    >
                      Bàn Giao Vận Chuyển (SHIPPED)
                    </button>
                  )}

                  {selectedOrder.status !== "DELIVERED" && (
                    <button
                      onClick={() => handleUpdateStatus(selectedOrder.id, "DELIVERED")}
                      disabled={isUpdatingStatus}
                      className="rounded-md border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-bold text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/20 transition-colors disabled:opacity-50 cursor-pointer"
                    >
                      Đã Giao Thành Công (DELIVERED)
                    </button>
                  )}

                  {selectedOrder.status !== "CANCELLED" && selectedOrder.status !== "DELIVERED" && (
                    <button
                      onClick={() => handleUpdateStatus(selectedOrder.id, "CANCELLED")}
                      disabled={isUpdatingStatus}
                      className="rounded-md border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-600 dark:text-red-400 hover:bg-red-500/20 transition-colors disabled:opacity-50 ml-auto cursor-pointer"
                    >
                      Hủy Đơn Hàng (CANCELLED)
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <button
              onClick={() => setDetailOpen(false)}
              className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-foreground hover:bg-muted cursor-pointer"
            >
              Đóng cửa sổ
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
