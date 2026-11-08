"use client";

import React, { useState, useEffect, useMemo } from "react";
import {
  Layers,
  Plus,
  Search,
  Edit,
  Trash2,
  Package,
  RefreshCw,
  CheckCircle2,
  XCircle,
  ExternalLink,
  Laptop,
  Smartphone,
  Headphones,
  Tablet,
  Watch,
  Monitor,
  Cpu,
} from "lucide-react";
import { getCategoriesApi, getProductsApi } from "@/lib/api/catalog";
import {
  createAdminCategoryApi,
  updateAdminCategoryApi,
  deleteAdminCategoryApi,
} from "@/lib/api/admin";
import { formatDateVI } from "@/lib/utils";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
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
import { CategoryDto, ProductDto, CreateCategoryRequest } from "@/types/catalog";

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;

  // Modal State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<CategoryDto | null>(null);
  const [formName, setFormName] = useState("");
  const [formDescription, setFormDescription] = useState("");
  const [formImageUrl, setFormImageUrl] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete State
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [categoryToDelete, setCategoryToDelete] = useState<CategoryDto | null>(null);

  const loadData = async () => {
    setLoading(true);
    try {
      const [catRes, prodRes] = await Promise.all([
        getCategoriesApi(),
        getProductsApi({ size: 1000 }),
      ]);
      setCategories(catRes || []);
      setProducts(prodRes.content || []);
    } catch (err: any) {
      toast.error("Không thể tải danh sách danh mục: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateDialog = () => {
    setEditingCategory(null);
    setFormName("");
    setFormDescription("");
    setFormImageUrl("");
    setDialogOpen(true);
  };

  const openEditDialog = (category: CategoryDto) => {
    setEditingCategory(category);
    setFormName(category.name);
    setFormDescription(category.description || "");
    setFormImageUrl(category.imageUrl || "");
    setDialogOpen(true);
  };

  const handleSaveCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) {
      toast.error("Vui lòng nhập tên danh mục.");
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: CreateCategoryRequest = {
        name: formName.trim(),
        description: formDescription.trim(),
        imageUrl: formImageUrl.trim() || undefined,
      };

      if (editingCategory) {
        await updateAdminCategoryApi(editingCategory.id, payload);
        toast.success(`Đã cập nhật danh mục "${formName}" thành công!`);
      } else {
        await createAdminCategoryApi(payload);
        toast.success(`Đã tạo danh mục mới "${formName}" thành công!`);
      }

      setDialogOpen(false);
      await loadData();
    } catch (err: any) {
      toast.error("Thao tác thất bại: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteCategory = async () => {
    if (!categoryToDelete) return;
    setIsSubmitting(true);
    try {
      await deleteAdminCategoryApi(categoryToDelete.id);
      toast.success(`Đã xóa danh mục "${categoryToDelete.name}"`);
      setDeleteDialogOpen(false);
      setCategoryToDelete(null);
      await loadData();
    } catch (err: any) {
      toast.error("Không thể xóa danh mục: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper to pick category icon
  const getCategoryIcon = (name: string) => {
    const lower = name.toLowerCase();
    if (lower.includes("laptop")) return <Laptop className="size-5 text-amber-500" />;
    if (lower.includes("thoại") || lower.includes("phone")) return <Smartphone className="size-5 text-blue-500" />;
    if (lower.includes("tai nghe") || lower.includes("âm thanh")) return <Headphones className="size-5 text-purple-500" />;
    if (lower.includes("bảng") || lower.includes("tablet") || lower.includes("ipad")) return <Tablet className="size-5 text-emerald-500" />;
    if (lower.includes("đồng hồ") || lower.includes("watch")) return <Watch className="size-5 text-pink-500" />;
    if (lower.includes("màn hình") || lower.includes("monitor")) return <Monitor className="size-5 text-indigo-500" />;
    return <Cpu className="size-5 text-stone-500" />;
  };

  const filteredCategories = useMemo(() => {
    return categories.filter((c) => {
      const matchSearch =
        c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (c.description && c.description.toLowerCase().includes(searchTerm.toLowerCase()));
      return matchSearch;
    });
  }, [categories, searchTerm]);

  const totalPages = Math.ceil(filteredCategories.length / pageSize) || 1;
  const paginatedCategories = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredCategories.slice(start, start + pageSize);
  }, [filteredCategories, currentPage, pageSize]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Danh Mục Thiết Bị Công Nghệ
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Quản trị các nhóm danh mục kinh doanh chủ lực của TECHGEAR và phân bổ sản phẩm.
          </p>
        </div>
      </div>

      {/* Grid of Highlighted Categories */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {loading
          ? [...Array(4)].map((_, i) => <Skeleton key={i} className="h-24 rounded-lg" />)
          : categories.slice(0, 4).map((cat) => {
              const productCount = products.filter((p) => p.category?.id === cat.id).length;
              return (
                <Card key={cat.id} className="p-3.5 relative overflow-hidden group shadow-xs">
                  <div className="flex items-start justify-between">
                    <div className="size-9 rounded-md border border-border bg-muted/30 flex items-center justify-center shrink-0">
                      {getCategoryIcon(cat.name)}
                    </div>
                    <Badge variant={cat.active ? "success" : "secondary"} className="text-[10px] px-1.5 py-0">
                      {cat.active ? "Hoạt động" : "Tạm ẩn"}
                    </Badge>
                  </div>
                  <div className="mt-2.5">
                    <h3 className="font-semibold text-xs text-foreground truncate group-hover:text-amber-600 transition-colors">
                      {cat.name}
                    </h3>
                    <p className="text-[11px] text-muted-foreground mt-0.5">
                      {productCount} thiết bị trong kho
                    </p>
                  </div>
                </Card>
              );
            })}
      </div>

      {/* Main Categories Table */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search Input */}
          <div className="relative w-56 sm:w-64">
            <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Tìm tên danh mục, mô tả..."
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
              onClick={loadData}
              disabled={loading}
              title="Làm mới danh sách"
              className="h-9 px-2.5 flex items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
            </button>
            <button
              onClick={openCreateDialog}
              className="h-9 px-3 flex items-center gap-1.5 rounded-md bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition-colors shadow-xs cursor-pointer"
            >
              <Plus className="size-3.5" />
              <span>Thêm Danh Mục</span>
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
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[60px]">ID</TableHead>
                  <TableHead>Biểu tượng & Tên danh mục</TableHead>
                  <TableHead>Mô tả nghiệp vụ</TableHead>
                  <TableHead className="text-center">Số lượng thiết bị</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedCategories.map((c) => {
                  const productCount = products.filter((p) => p.category?.id === c.id).length;
                  return (
                    <TableRow key={c.id}>
                      <TableCell className="font-mono text-xs font-semibold text-zinc-500">
                        #{c.id}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2.5">
                          <div className="size-7 rounded-md border border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900 flex items-center justify-center shrink-0">
                            {getCategoryIcon(c.name)}
                          </div>
                          <span className="font-semibold text-xs text-zinc-900 dark:text-zinc-100">
                            {c.name}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <p className="text-xs text-zinc-500 max-w-md truncate">
                          {c.description || "Chưa có mô tả"}
                        </p>
                      </TableCell>
                      <TableCell className="text-center">
                        <span className="inline-flex items-center justify-center rounded-md bg-zinc-100 dark:bg-zinc-800 px-2 py-0.5 text-xs font-mono font-semibold text-zinc-900 dark:text-zinc-100">
                          {productCount}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={c.active ? "success" : "secondary"} className="text-[10px] px-1.5 py-0">
                          {c.active ? "Hoạt động" : "Tạm ẩn"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => openEditDialog(c)}
                            className="p-1.5 rounded-md border border-zinc-200 dark:border-zinc-800 text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800 hover:text-zinc-900 transition-colors cursor-pointer"
                            title="Sửa danh mục"
                          >
                            <Edit className="size-3.5" />
                          </button>
                          <button
                            onClick={() => {
                              setCategoryToDelete(c);
                              setDeleteDialogOpen(true);
                            }}
                            className="p-1.5 rounded-md border border-zinc-200 dark:border-zinc-800 text-zinc-500 hover:bg-red-500/10 hover:text-red-600 transition-colors cursor-pointer"
                            title="Xóa danh mục"
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>

        {/* Pagination Footer */}
        {!loading && filteredCategories.length > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div>
              Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
              <span className="font-semibold text-foreground">
                {Math.min(currentPage * pageSize, filteredCategories.length)}
              </span>{" "}
              trong tổng số <span className="font-semibold text-foreground">{filteredCategories.length}</span> danh mục
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

      {/* CREATE / EDIT DIALOG */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editingCategory ? "Cập Nhật Danh Mục" : "Thêm Danh Mục Mới"}
            </DialogTitle>
            <DialogDescription>
              Thiết lập tên và mô tả chi tiết cho nhóm danh mục phần cứng / thiết bị.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSaveCategory} className="space-y-4 pt-2">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Tên danh mục *</label>
              <input
                type="text"
                required
                placeholder="Ví dụ: Laptop, Máy tính bảng, Điện thoại..."
                value={formName}
                onChange={(e) => setFormName(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Mô tả danh mục</label>
              <textarea
                rows={3}
                placeholder="Mô tả các dòng sản phẩm thuộc nhóm này..."
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Ảnh đại diện danh mục (URL)</label>
              <input
                type="text"
                placeholder="/img/HeadsetM.jpg hoặc https://..."
                value={formImageUrl}
                onChange={(e) => setFormImageUrl(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
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
                disabled={isSubmitting}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
              >
                {isSubmitting ? "Đang lưu..." : editingCategory ? "Lưu thay đổi" : "Tạo danh mục"}
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* DELETE DIALOG */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle className="text-red-600">Xóa Danh Mục?</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa danh mục <strong>"{categoryToDelete?.name}"</strong>? Lưu ý: Không nên xóa danh mục đang có sản phẩm liên kết.
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
              onClick={handleDeleteCategory}
              disabled={isSubmitting}
              className="rounded-lg bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50"
            >
              {isSubmitting ? "Đang xóa..." : "Xác nhận xóa"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
