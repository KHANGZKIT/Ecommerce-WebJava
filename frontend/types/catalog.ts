export interface CategoryDto {
  id: number;
  name: string;
  description: string;
  imageUrl?: string | null;
  active: boolean;
  createdAt: string;
}

export interface ProductDto {
  id: number;
  name: string;
  description: string;
  sku: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string | null;
  category?: CategoryDto | null;
  active: boolean;
  createdAt: string;
  updatedAt?: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ProductQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  keyword?: string;
}

export interface CreateProductRequest {
  name: string;
  description: string;
  sku: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  imageUrl?: string;
}

export interface UpdateProductRequest {
  name: string;
  description: string;
  sku: string;
  price: number;
  stockQuantity: number;
  categoryId: number;
  imageUrl?: string;
}

export interface InventoryUpdateRequest {
  quantity: number;
  mode: "ABSOLUTE" | "DELTA";
}

export interface CreateCategoryRequest {
  name: string;
  description: string;
  imageUrl?: string;
}
