"use client";

import React, { useState, useEffect, useMemo } from "react";
import {
  Users,
  Search,
  ShieldCheck,
  UserCheck,
  UserX,
  Mail,
  Phone,
  Calendar,
  RefreshCw,
  MoreHorizontal,
  Trash2,
  Lock,
} from "lucide-react";
import { getAdminUsersApi, deleteAdminUserApi } from "@/lib/api/admin";
import { formatDateVI } from "@/lib/utils";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import { UserProfileDto } from "@/types/user";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<UserProfileDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Delete State
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<UserProfileDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await getAdminUsersApi();
      setUsers(data || []);
    } catch (err: any) {
      toast.error("Không thể tải danh sách người dùng: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleDeleteUser = async () => {
    if (!userToDelete) return;
    if (userToDelete.role === "ADMIN") {
      toast.error("Không thể xóa tài khoản Quản trị viên (ADMIN) cấp cao!");
      setDeleteDialogOpen(false);
      return;
    }

    setIsDeleting(true);
    try {
      await deleteAdminUserApi(userToDelete.id);
      toast.success(`Đã xóa tài khoản "${userToDelete.email}"`);
      setDeleteDialogOpen(false);
      setUserToDelete(null);
      await loadData();
    } catch (err: any) {
      toast.error("Xóa tài khoản thất bại: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsDeleting(false);
    }
  };

  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const fullName = `${u.firstName || ""} ${u.lastName || ""}`.toLowerCase();
      const matchSearch =
        fullName.includes(searchTerm.toLowerCase()) ||
        u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (u.phone && u.phone.includes(searchTerm));

      const matchRole = roleFilter === "ALL" || u.role === roleFilter;

      return matchSearch && matchRole;
    });
  }, [users, searchTerm, roleFilter]);

  const totalAdmins = users.filter((u) => u.role === "ADMIN").length;
  const totalCustomers = users.filter((u) => u.role === "CUSTOMER").length;

  const totalPages = Math.ceil(filteredUsers.length / pageSize) || 1;
  const paginatedUsers = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredUsers.slice(start, start + pageSize);
  }, [filteredUsers, currentPage, pageSize]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Quản Lý Khách Hàng & Phân Quyền
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Danh sách toàn bộ tài khoản khách hàng, nhân viên và quản trị viên hệ thống TECHGEAR.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Tổng Số Tài Khoản
          </span>
          <div className="text-xl font-bold text-foreground mt-1 tabular-nums">
            {loading ? <Skeleton className="h-7 w-16" /> : users.length}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Hệ thống PostgreSQL user_db</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Khách Hàng Mua Sắm
          </span>
          <div className="text-xl font-bold text-blue-600 dark:text-blue-400 mt-1 tabular-nums">
            {loading ? <Skeleton className="h-7 w-16" /> : totalCustomers}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Đã kích hoạt tính năng mua sắm</p>
        </Card>

        <Card className="p-3.5 shadow-xs">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
            Quản Trị Viên (ADMIN)
          </span>
          <div className="text-xl font-bold text-amber-600 dark:text-amber-400 mt-1 tabular-nums">
            {loading ? <Skeleton className="h-7 w-16" /> : totalAdmins}
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">Toàn quyền vận hành hệ thống</p>
        </Card>
      </div>

      {/* Main Users Table Card with Integrated Toolbar */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search & Filter */}
          <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
            <div className="relative w-64 sm:w-72">
              <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Tìm họ tên, email, số điện thoại..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-hidden focus:ring-1 focus:ring-ring transition-colors"
              />
            </div>

            <div className="w-40 sm:w-48">
              <Select
                value={roleFilter}
                onValueChange={(val) => {
                  setRoleFilter(val);
                  setCurrentPage(1);
                }}
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Lọc theo vai trò" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Tất cả vai trò</SelectItem>
                  <SelectItem value="CUSTOMER">Khách hàng (CUSTOMER)</SelectItem>
                  <SelectItem value="ADMIN">Quản trị viên (ADMIN)</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Right: Refresh button */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={loadData}
              disabled={loading}
              title="Làm mới danh sách"
              className="h-9 px-2.5 flex items-center justify-center rounded-md border border-zinc-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 text-zinc-600 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-850 hover:text-zinc-900 transition-colors disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>
        </div>

        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[60px]">ID</TableHead>
                  <TableHead>Họ & Tên</TableHead>
                  <TableHead>Email liên hệ</TableHead>
                  <TableHead>Số điện thoại</TableHead>
                  <TableHead>Vai trò</TableHead>
                  <TableHead>Ngày tham gia</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedUsers.map((u) => {
                  const isAdmin = u.role === "ADMIN";
                  return (
                    <TableRow key={u.id}>
                      <TableCell className="font-mono text-xs font-bold text-muted-foreground">
                        #{u.id}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2.5">
                          <Avatar className="size-8">
                            <AvatarFallback
                              className={`text-xs font-bold ${
                                isAdmin
                                  ? "bg-amber-500/10 text-amber-600 dark:text-amber-400"
                                  : "bg-muted text-muted-foreground"
                              }`}
                            >
                              {u.firstName ? u.firstName[0].toUpperCase() : "U"}
                            </AvatarFallback>
                          </Avatar>
                          <span className="font-bold text-xs text-foreground">
                            {u.firstName} {u.lastName}
                          </span>
                        </div>
                      </TableCell>

                      <TableCell>
                        <span className="text-xs font-mono text-muted-foreground">
                          {u.email}
                        </span>
                      </TableCell>

                      <TableCell>
                        <span className="text-xs font-mono text-foreground">
                          {u.phone || "—"}
                        </span>
                      </TableCell>

                      <TableCell>
                        <Badge
                          variant={isAdmin ? "warning" : "secondary"}
                          className="text-[10px]"
                        >
                          {isAdmin ? "ADMIN" : "CUSTOMER"}
                        </Badge>
                      </TableCell>

                      <TableCell>
                        <span className="text-[11px] text-muted-foreground whitespace-nowrap">
                          {formatDateVI(u.createdAt)}
                        </span>
                      </TableCell>

                      <TableCell className="text-right">
                        {!isAdmin ? (
                          <button
                            onClick={() => {
                              setUserToDelete(u);
                              setDeleteDialogOpen(true);
                            }}
                            className="p-1.5 rounded-md border border-border text-muted-foreground hover:bg-red-500/10 hover:text-red-600 transition-colors"
                            title="Xóa tài khoản"
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        ) : (
                          <span
                            className="p-1.5 text-muted-foreground/40 inline-flex"
                            title="Không thể xóa Admin"
                          >
                            <Lock className="size-3.5" />
                          </span>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>

        {/* Pagination Footer */}
        {!loading && filteredUsers.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div>
              Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
              <span className="font-semibold text-foreground">
                {Math.min(currentPage * pageSize, filteredUsers.length)}
              </span>{" "}
              trong tổng số <span className="font-semibold text-foreground">{filteredUsers.length}</span> tài khoản
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

      {/* DELETE DIALOG */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle className="text-red-600">Xóa Tài Khoản Người Dùng?</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa tài khoản <strong>"{userToDelete?.email}"</strong>? Dữ liệu đơn hàng liên quan có thể bị ảnh hưởng.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <button
              onClick={() => setDeleteDialogOpen(false)}
              className="rounded-lg border border-border px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
            >
              Hủy
            </button>
            <button
              onClick={handleDeleteUser}
              disabled={isDeleting}
              className="rounded-lg bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50"
            >
              {isDeleting ? "Đang xóa..." : "Xác nhận xóa"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
