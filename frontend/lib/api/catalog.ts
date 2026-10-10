import { fetchApi } from "./client";
import { CategoryDto, Page, ProductDto, ProductQueryParams } from "@/types/catalog";

export async function getProductsApi(params?: ProductQueryParams): Promise<Page<ProductDto>> {
  return fetchApi<Page<ProductDto>>("/api/v1/products", {
    params: {
      page: params?.page,
      size: params?.size,
      sort: params?.sort,
    },
  });
}

export async function getProductByIdApi(id: number | string): Promise<ProductDto> {
  return fetchApi<ProductDto>(`/api/v1/products/${id}`);
}

export async function getProductsByCategoryApi(
  categoryId: number | string,
  params?: ProductQueryParams
): Promise<Page<ProductDto>> {
  return fetchApi<Page<ProductDto>>(`/api/v1/products/category/${categoryId}`, {
    params: {
      page: params?.page,
      size: params?.size,
      sort: params?.sort,
    },
  });
}

export async function searchProductsApi(
  keyword: string,
  params?: ProductQueryParams
): Promise<Page<ProductDto>> {
  return fetchApi<Page<ProductDto>>("/api/v1/products/search", {
    params: {
      keyword,
      page: params?.page,
      size: params?.size,
      sort: params?.sort,
    },
  });
}

export async function getCategoriesApi(): Promise<CategoryDto[]> {
  return fetchApi<CategoryDto[]>("/api/v1/categories");
}

export async function getCategoryByIdApi(id: number | string): Promise<CategoryDto> {
  return fetchApi<CategoryDto>(`/api/v1/categories/${id}`);
}

