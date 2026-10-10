export interface CartItemDto {
  id?: number;
  productId: number;
  productName: string;
  productSku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  imageUrl?: string | null;
}

export interface CartDto {
  id?: number;
  userId: number;
  items: CartItemDto[];
  totalPrice: number;
  itemCount: number;
  updatedAt?: string | null;
}

export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

export interface UpdateQuantityRequest {
  quantity: number;
}
