"use client";

import React, { useState, useEffect, useMemo } from "react";
import {
  Plus,
  Search,
  Filter,
  Edit,
  Trash2,
  Package,
  Layers,
  ArrowUpDown,
  MoreHorizontal,
  RefreshCw,
  Cpu,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Eye,
  SlidersHorizontal,
} from "lucide-react";
import {
  getProductsApi,
  getProductsByCategoryApi,
  getCategoriesApi,
} from "@/lib/api/catalog";
import {
  createAdminProductApi,
  updateAdminProductApi,
  updateAdminInventoryApi,
  deleteAdminProductApi,
} from "@/lib/api/admin";
import { formatVND, formatDateVI } from "@/lib/utils";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ProductDto, CategoryDto, CreateProductRequest } from "@/types/catalog";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState<string>("ALL");
  const [stockFilter, setStockFilter] = useState<string>("ALL");
  const [sortBy, setSortBy] = useState<"createdAt" | "price" | "stockQuantity">("createdAt");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);

  // Dialog State: Create / Edit
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ProductDto | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form Fields
  const [formName, setFormName] = useState("");
  const [formSku, setFormSku] = useState("");
  const [formCategory, setFormCategory] = useState<number>(1);
  const [formPrice, setFormPrice] = useState<number>(0);
  const [formStock, setFormStock] = useState<number>(10);
  const [formImageUrl, setFormImageUrl] = useState("");
  const [formDescription, setFormDescription] = useState("");

  // Delete State
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState<ProductDto | null>(null);

  // Quick Stock Adjustment Dialog
  const [stockDialogOpen, setStockDialogOpen] = useState(false);
  const [stockProduct, setStockProduct] = useState<ProductDto | null>(null);
  const [newStockValue, setNewStockValue] = useState<number>(0);

  const loadData = async () => {
    setLoading(true);
    try {
      const [prodRes, catRes] = await Promise.all([
        getProductsApi({ size: 1000 }),
        getCategoriesApi(),
      ]);
      setProducts(prodRes.content || []);
      setCategories(catRes || []);
    } catch (err: any) {
      toast.error("Không thể tải danh sách sản phẩm: " + (err.message || "Lỗi kết nối"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateDialog = () => {
    setEditingProduct(null);
    setFormName("");
    setFormSku(`TG-${Date.now().toString().slice(-6)}`);
    setFormCategory(categories[0]?.id || 1);
    setFormPrice(1000000);
    setFormStock(20);
    setFormImageUrl("");
    setFormDescription("");
    setDialogOpen(true);
  };

  const openEditDialog = (product: ProductDto) => {
    setEditingProduct(product);
    setFormName(product.name);
    setFormSku(product.sku);
    setFormCategory(product.category?.id || 1);
    setFormPrice(product.price);
    setFormStock(product.stockQuantity);
    setFormImageUrl(product.imageUrl || "");
    setFormDescription(product.description || "");
    setDialogOpen(true);
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim() || !formSku.trim()) {
      toast.error("Vui lòng điền đầy đủ tên sản phẩm và SKU.");
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: CreateProductRequest = {
        name: formName.trim(),
        sku: formSku.trim(),
        categoryId: Number(formCategory),
        price: Number(formPrice),
        stockQuantity: Number(formStock),
        imageUrl: formImageUrl.trim() || undefined,
        description: formDescription.trim(),
      };

      if (editingProduct) {
        await updateAdminProductApi(editingProduct.id, payload);
        toast.success(`Đã cập nhật sản phẩm "${formName}" thành công!`);
      } else {
        await createAdminProductApi(payload);
        toast.success(`Đã thêm thiết bị mới "${formName}" vào hệ thống!`);
      }

      setDialogOpen(false);
      await loadData();
    } catch (err: any) {
      toast.error("Thao tác thất bại: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteProduct = async () => {
    if (!productToDelete) return;
    setIsSubmitting(true);
    try {
      await deleteAdminProductApi(productToDelete.id);
      toast.success(`Đã xóa sản phẩm "${productToDelete.name}"`);
      setDeleteDialogOpen(false);
      setProductToDelete(null);
      await loadData();
    } catch (err: any) {
      toast.error("Không thể xóa sản phẩm: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateStock = async () => {
    if (!stockProduct) return;
    setIsSubmitting(true);
    try {
      await updateAdminInventoryApi(stockProduct.id, {
        quantity: Number(newStockValue),
        mode: "ABSOLUTE",
      });
      toast.success(`Đã cập nhật tồn kho cho "${stockProduct.name}" thành ${newStockValue} chiếc!`);
      setStockDialogOpen(false);
      setStockProduct(null);
      await loadData();
    } catch (err: any) {
      toast.error("Cập nhật tồn kho thất bại: " + (err.message || "Lỗi máy chủ"));
    } finally {
      setIsSubmitting(false);
    }
  };

  // Filter & Sort Logic
  const filteredProducts = useMemo(() => {
    return products
      .filter((p) => {
        const query = searchTerm.toLowerCase();
        const matchesSearch =
          !searchTerm ||
          p.name.toLowerCase().includes(query) ||
          p.sku.toLowerCase().includes(query) ||
          (p.category?.name && p.category.name.toLowerCase().includes(query)) ||
          (p.description && p.description.toLowerCase().includes(query));

        const matchesCategory =
          categoryFilter === "ALL" || p.category?.id?.toString() === categoryFilter;

        let matchesStock = true;
        if (stockFilter === "IN_STOCK") matchesStock = p.stockQuantity >= 10;
        else if (stockFilter === "LOW_STOCK") matchesStock = p.stockQuantity > 0 && p.stockQuantity < 10;
        else if (stockFilter === "OUT_OF_STOCK") matchesStock = p.stockQuantity === 0;

        return matchesSearch && matchesCategory && matchesStock;
      })
      .sort((a, b) => {
        let cmp = 0;
        if (sortBy === "price") cmp = a.price - b.price;
        else if (sortBy === "stockQuantity") cmp = a.stockQuantity - b.stockQuantity;
        else cmp = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();

        return sortOrder === "asc" ? cmp : -cmp;
      });
  }, [products, searchTerm, categoryFilter, stockFilter, sortBy, sortOrder]);

  const totalPages = Math.ceil(filteredProducts.length / pageSize) || 1;
  const paginatedProducts = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredProducts.slice(start, start + pageSize);
  }, [filteredProducts, currentPage, pageSize]);

  return (
    <div className="space-y-4">
      {/* Page Header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-foreground">
            Sản Phẩm & Cấu Hình Phần Cứng
          </h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            Quản lý catalog công nghệ, thông số kỹ thuật, giá niêm yết và mức độ tồn kho thiết bị.
          </p>
        </div>
      </div>

      {/* Main Table Card with Integrated Single-Row Toolbar */}
      <Card className="p-0 overflow-hidden shadow-xs">
        {/* Table Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2.5 p-3.5 border-b border-border bg-muted/40">
          {/* Left: Search & Filters */}
          <div className="flex flex-wrap items-center gap-2 flex-1 min-w-0">
            {/* Search Input */}
            <div className="relative w-56 sm:w-64">
              <Search className="absolute left-2.5 top-2.5 size-4 text-muted-foreground" />
              <input
                type="text"
                placeholder="Tìm tên sản phẩm, mã SKU..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1);
                }}
                className="h-9 w-full rounded-md border border-border bg-background pl-8 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-hidden focus:ring-1 focus:ring-ring transition-colors"
              />
            </div>

            {/* Category Filter */}
            <div className="w-40 sm:w-48">
              <Select
                value={categoryFilter}
                onValueChange={(val) => {
                  setCategoryFilter(val);
                  setCurrentPage(1);
                }}
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Tất cả danh mục" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">
                    Tất cả danh mục ({products.length})
                  </SelectItem>
                  {categories.map((c) => {
                    const count = products.filter((p) => p.category?.id === c.id).length;
                    return (
                      <SelectItem key={c.id} value={c.id.toString()}>
                        {c.name} ({count})
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>

            {/* Stock Filter */}
            <div className="w-32 sm:w-36">
              <Select
                value={stockFilter}
                onValueChange={(val) => {
                  setStockFilter(val);
                  setCurrentPage(1);
                }}
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Trạng thái tồn kho" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">Mọi tồn kho</SelectItem>
                  <SelectItem value="IN_STOCK">Còn hàng (≥10)</SelectItem>
                  <SelectItem value="LOW_STOCK">Sắp hết (&lt;10)</SelectItem>
                  <SelectItem value="OUT_OF_STOCK">Hết hàng (0)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Sort Control */}
            <div className="w-32 sm:w-36 hidden md:block">
              <Select
                value={`${sortBy}-${sortOrder}`}
                onValueChange={(val) => {
                  const [sb, so] = val.split("-");
                  setSortBy(sb as any);
                  setSortOrder(so as any);
                  setCurrentPage(1);
                }}
              >
                <SelectTrigger className="h-9">
                  <SelectValue placeholder="Sắp xếp" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt-desc">Mới nhất</SelectItem>
                  <SelectItem value="createdAt-asc">Cũ nhất</SelectItem>
                  <SelectItem value="price-desc">Giá cao</SelectItem>
                  <SelectItem value="price-asc">Giá thấp</SelectItem>
                  <SelectItem value="stockQuantity-asc">Tồn ít nhất</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={loadData}
              disabled={loading}
              title="Đồng bộ danh sách"
              className="h-9 px-2.5 flex items-center justify-center rounded-md border border-border bg-background text-muted-foreground hover:bg-muted hover:text-foreground transition-colors disabled:opacity-50 cursor-pointer"
            >
              <RefreshCw className={`size-3.5 ${loading ? "animate-spin" : ""}`} />
            </button>
            <button
              onClick={openCreateDialog}
              className="h-9 px-3 flex items-center gap-1.5 rounded-md bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 transition-colors shadow-xs cursor-pointer"
            >
              <Plus className="size-3.5" />
              <span>Thêm Thiết Bị</span>
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
          ) : filteredProducts.length === 0 ? (
            <div className="py-16 text-center">
              <Package className="mx-auto size-10 text-muted-foreground/40 mb-3" />
              <h3 className="text-sm font-bold text-foreground">Không tìm thấy sản phẩm nào</h3>
              <p className="text-xs text-muted-foreground mt-1 max-w-sm mx-auto">
                Không có thiết bị công nghệ nào phù hợp với bộ lọc hiện tại. Thử xóa tìm kiếm hoặc bộ lọc.
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[80px]">Ảnh</TableHead>
                  <TableHead>Tên & Cấu hình</TableHead>
                  <TableHead>SKU</TableHead>
                  <TableHead>Danh mục</TableHead>
                  <TableHead>Giá niêm yết</TableHead>
                  <TableHead>Tồn kho</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {paginatedProducts.map((p) => {
                  const isLow = p.stockQuantity > 0 && p.stockQuantity < 10;
                  const isOut = p.stockQuantity === 0;

                  return (
                    <TableRow key={p.id}>
                      {/* Image */}
                      <TableCell>
                        <div className="size-11 rounded-lg border border-border bg-muted/30 overflow-hidden flex items-center justify-center shrink-0">
                          {p.imageUrl ? (
                            <img
                              src={p.imageUrl}
                              alt={p.name}
                              className="size-full object-cover"
                            />
                          ) : (
                            <Cpu className="size-5 text-muted-foreground" />
                          )}
                        </div>
                      </TableCell>

                      {/* Name & Desc */}
                      <TableCell>
                        <div className="max-w-md">
                          <p className="font-bold text-xs text-foreground truncate">
                            {p.name}
                          </p>
                          <p className="text-[11px] text-muted-foreground truncate mt-0.5">
                            {p.description || "Không có mô tả chi tiết"}
                          </p>
                        </div>
                      </TableCell>

                      {/* SKU */}
                      <TableCell>
                        <span className="font-mono text-xs font-semibold text-foreground/80">
                          {p.sku}
                        </span>
                      </TableCell>

                      {/* Category */}
                      <TableCell>
                        <Badge variant="outline" className="text-[11px] font-medium">
                          {p.category?.name || "Chưa phân loại"}
                        </Badge>
                      </TableCell>

                      {/* Price */}
                      <TableCell>
                        <span className="font-bold text-xs text-foreground tabular-nums">
                          {formatVND(p.price)}
                        </span>
                      </TableCell>

                      {/* Stock Quantity */}
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              setStockProduct(p);
                              setNewStockValue(p.stockQuantity);
                              setStockDialogOpen(true);
                            }}
                            className={`font-mono text-xs font-bold underline decoration-dotted underline-offset-2 hover:opacity-80 tabular-nums ${
                              isOut
                                ? "text-red-500"
                                : isLow
                                ? "text-amber-500"
                                : "text-emerald-600 dark:text-emerald-400"
                            }`}
                            title="Bấm để điều chỉnh tồn kho"
                          >
                            {p.stockQuantity} chiếc
                          </button>
                          {isLow && (
                            <Badge variant="warning" className="text-[9px] px-1 py-0">
                              Sắp hết
                            </Badge>
                          )}
                          {isOut && (
                            <Badge variant="destructive" className="text-[9px] px-1 py-0">
                              Hết hàng
                            </Badge>
                          )}
                        </div>
                      </TableCell>

                      {/* Actions */}
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <button className="size-8 inline-flex items-center justify-center rounded-md border border-border hover:bg-muted transition-colors">
                              <MoreHorizontal className="size-4" />
                            </button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => openEditDialog(p)}>
                              <Edit className="size-3.5 mr-2" />
                              <span>Sửa thông tin</span>
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => {
                                setStockProduct(p);
                                setNewStockValue(p.stockQuantity);
                                setStockDialogOpen(true);
                              }}
                            >
                              <SlidersHorizontal className="size-3.5 mr-2 text-amber-500" />
                              <span>Chỉnh tồn kho</span>
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              variant="destructive"
                              onClick={() => {
                                setProductToDelete(p);
                                setDeleteDialogOpen(true);
                              }}
                            >
                              <Trash2 className="size-3.5 mr-2" />
                              <span>Xóa sản phẩm</span>
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>

        {/* Pagination Footer */}
        {!loading && filteredProducts.length > 0 && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 border-t border-border bg-muted/20 text-xs text-muted-foreground">
            <div className="flex flex-wrap items-center gap-3">
              <div>
                Hiển thị <span className="font-semibold text-foreground">{(currentPage - 1) * pageSize + 1}</span> -{" "}
                <span className="font-semibold text-foreground">
                  {Math.min(currentPage * pageSize, filteredProducts.length)}
                </span>{" "}
                trong tổng số <span className="font-semibold text-foreground">{filteredProducts.length}</span> thiết bị
              </div>
              <div className="flex items-center gap-1.5">
                <span className="text-[11px] text-muted-foreground">Hiển thị:</span>
                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                  className="h-7 px-1.5 rounded border border-border bg-background text-xs text-foreground cursor-pointer focus:outline-hidden"
                >
                  <option value={10}>10</option>
                  <option value={15}>15</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>
              </div>
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
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>
              {editingProduct ? "Cập Nhật Thông Tin Thiết Bị" : "Thêm Thiết Bị Công Nghệ Mới"}
            </DialogTitle>
            <DialogDescription>
              Điền các thông số kỹ thuật, giá niêm yết và thông tin bảo hành cho sản phẩm.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSaveProduct} className="space-y-4 pt-2">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Product Name */}
              <div className="sm:col-span-2 space-y-1.5">
                <label className="text-xs font-semibold text-foreground">
                  Tên sản phẩm / Model thiết bị *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Laptop Gaming ASUS ROG Zephyrus G16"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              {/* SKU */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Mã SKU *</label>
                <input
                  type="text"
                  required
                  placeholder="TG-GVN-..."
                  value={formSku}
                  onChange={(e) => setFormSku(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              {/* Category */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Danh mục *</label>
                <Select
                  value={formCategory.toString()}
                  onValueChange={(val) => setFormCategory(Number(val))}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn danh mục" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => (
                      <SelectItem key={c.id} value={c.id.toString()}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Price */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Giá bán (VNĐ) *</label>
                <input
                  type="number"
                  required
                  min={0}
                  step={1000}
                  value={formPrice}
                  onChange={(e) => setFormPrice(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              {/* Stock Quantity */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Số lượng tồn kho *</label>
                <input
                  type="number"
                  required
                  min={0}
                  value={formStock}
                  onChange={(e) => setFormStock(Number(e.target.value))}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              {/* Image URL */}
              <div className="sm:col-span-2 space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Đường dẫn ảnh sản phẩm (URL)</label>
                <input
                  type="url"
                  placeholder="https://... hoặc /img/laptop.jpg"
                  value={formImageUrl}
                  onChange={(e) => setFormImageUrl(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>

              {/* Description */}
              <div className="sm:col-span-2 space-y-1.5">
                <label className="text-xs font-semibold text-foreground">
                  Mô tả thông số kỹ thuật (Specs / Cấu hình)
                </label>
                <textarea
                  rows={3}
                  placeholder="CPU Intel Core Ultra 9, RAM 32GB, RTX 4080, Màn hình OLED 240Hz, Bảo hành 24 tháng chính hãng..."
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:ring-1 focus:ring-ring focus:outline-hidden"
                />
              </div>
            </div>

            <DialogFooter>
              <button
                type="button"
                onClick={() => setDialogOpen(false)}
                className="rounded-lg border border-border px-4 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted transition-colors"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90 transition-colors shadow-xs disabled:opacity-50"
              >
                {isSubmitting ? "Đang lưu..." : editingProduct ? "Lưu thay đổi" : "Tạo thiết bị"}
              </button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* QUICK STOCK ADJUSTMENT DIALOG */}
      <Dialog open={stockDialogOpen} onOpenChange={setStockDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Điều Chỉnh Tồn Kho</DialogTitle>
            <DialogDescription>
              Cập nhật số lượng thiết bị thực tế trong kho: <strong>{stockProduct?.name}</strong>
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 pt-2">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-foreground">Số lượng tồn kho mới</label>
              <input
                type="number"
                min={0}
                value={newStockValue}
                onChange={(e) => setNewStockValue(Number(e.target.value))}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm font-mono tabular-nums focus:ring-1 focus:ring-ring focus:outline-hidden"
              />
            </div>
          </div>

          <DialogFooter>
            <button
              onClick={() => setStockDialogOpen(false)}
              className="rounded-lg border border-border px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
            >
              Hủy
            </button>
            <button
              onClick={handleUpdateStock}
              disabled={isSubmitting}
              className="rounded-lg bg-primary px-4 py-2 text-xs font-bold text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {isSubmitting ? "Đang lưu..." : "Xác nhận tồn kho"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* DELETE CONFIRMATION DIALOG */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle className="text-red-600">Xác Nhận Xóa Thiết Bị?</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa sản phẩm <strong>"{productToDelete?.name}"</strong>? Hành động này sẽ loại bỏ sản phẩm khỏi catalog bán lẻ.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <button
              onClick={() => setDeleteDialogOpen(false)}
              className="rounded-lg border border-border px-3 py-2 text-xs font-semibold text-muted-foreground hover:bg-muted"
            >
              Không, giữ lại
            </button>
            <button
              onClick={handleDeleteProduct}
              disabled={isSubmitting}
              className="rounded-lg bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-700 disabled:opacity-50"
            >
              {isSubmitting ? "Đang xóa..." : "Xóa vĩnh viễn"}
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
