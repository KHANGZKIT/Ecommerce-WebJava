# TECHGEAR — ADMIN-03 Admin Products Design Specification

> **Document ID:** ADMIN-03-PRODUCTS-DESIGN  
> **Route:** `/admin/products`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `c3bc0de1fd40430ea05f410c102981e3`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [ADMIN-02](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-02-DASHBOARD-DESIGN.md), [FE-03](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-03-SHOP-PRODUCT-LISTING-DESIGN.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md)  
> **Backend Authority:** Spring Boot 3.2 `product-service` & `api-gateway`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Core Purpose

The **Admin Products** screen (`/admin/products`) provides the complete product catalog management surface for TECHGEAR administrators. It enables operators to inspect paginated active products, perform full-text keyword searches, create new catalog items, edit existing product metadata, adjust inventory quantities (via absolute or delta increments), and perform soft-deletion of products.

### Strict YAGNI & Scope Boundaries
* **Core Capabilities Supported (`BACKEND-CONFIRMED`):**
  1. **LIST:** Paginated catalog table (`GET /api/v1/products`).
  2. **SEARCH:** Full-text search by keyword (`GET /api/v1/products/search?keyword=...`).
  3. **CREATE:** Create product form (`POST /api/v1/products`).
  4. **EDIT:** Edit product metadata form (`PATCH /api/v1/products/{id}`).
  5. **INVENTORY UPDATE:** Dedicated stock adjustment modal/form (`PATCH /api/v1/products/{id}/inventory`).
  6. **SOFT DELETE:** Deactivate product with confirmation modal (`DELETE /api/v1/products/{id}`).
* **Strict Separation of Product Details vs. Inventory:**
  * Product metadata (`name`, `description`, `price`, `imageUrl`, `categoryId`) is managed via `PATCH /api/v1/products/{id}` (`UpdateProductRequest`).
  * `active` is **not** exposed as an editable UI field in the Edit Product modal; deactivation is handled exclusively via soft-delete (`DELETE /api/v1/products/{id}`).
  * Stock quantities are **never** updated via product metadata editing; inventory adjustments are strictly processed via `PATCH /api/v1/products/{id}/inventory` (`InventoryUpdateRequest`).
  * `sku` is permanently immutable after creation (read-only in Edit modal).
* **Explicit Enterprise Scope Exclusions (`SCOPE LIMITATION`):**
  * **No Bulk Operations:** No CSV/Excel import/export, no multi-select bulk delete, no bulk edit.
  * **No Image Management:** No image asset manager, CDN uploader, cropper, or multi-angle image gallery (accepts single static image URL string).
  * **No Product Variations / Matrix:** Flat catalog items; no colors, sizes, or child SKU variants.
  * **No Marketing Taxonomy:** No product tags, collections, reviews, customer ratings, badges, or discount promotional engines.
  * **No Supply Chain / Warehouse Features:** No suppliers, purchase orders, reorder thresholds, low-stock automations, cost price, profit margin, or warehouse locations.
  * **No AI Search or Autocomplete:** No Elasticsearch, vector search, search history, or trending searches.

---

## 2. Route & Access Control

* **Canonical Route:** `/admin/products`
* **Route Type:** Protected Administrative Route.
* **Actor & Permissions Matrix:**

| Actor | Action / Method | Behavior & UX Response | Enforcement Layer |
| :--- | :--- | :--- | :--- |
| **Guest (Unauthenticated)** | Any | Intercepted by auth guard; navigates to `/login` | `FE-15 Auth Guard` |
| **Customer (`CUSTOMER` role)**| Any | Renders inline **Access Denied** vitrine; does NOT log out | Client Route Guard |
| **Admin (`ADMIN` role)** | `GET /products` (List) | HTTP 200 OK — loads product page | `ProductController.java:24` |
| **Admin (`ADMIN` role)** | `GET /products/search` | HTTP 200 OK — loads search page | `ProductController.java:37` |
| **Admin (`ADMIN` role)** | `POST /products` (Create) | HTTP 201 Created — returns created `ProductDto` | `ProductController.java:73` |
| **Admin (`ADMIN` role)** | `PATCH /products/{id}` (Edit) | HTTP 200 OK — returns updated `ProductDto` | `ProductController.java:78` |
| **Admin (`ADMIN` role)** | `PATCH /products/{id}/inv` | HTTP 200 OK — returns updated `ProductDto` | `ProductController.java:85` |
| **Admin (`ADMIN` role)** | `DELETE /products/{id}` | HTTP 204 No Content — soft deletes product | `ProductController.java:92` |

---

## 3. Backend Verification & Integration Contracts

### 3.1 Verified Source Code References

| Capability | Backend File & Line | Method & Signature | Response Type | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **List Products** | `ProductController.java:24-28` | `getAllProducts(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable)` | `Page<ProductDto>` | Calls `productRepository.findAllByActiveTrue(pageable)`. |
| **Search Products** | `ProductController.java:37-42` | `search(@RequestParam String keyword, Pageable pageable)` | `Page<ProductDto>` | Calls `productRepository.searchByKeyword(keyword, pageable)`. |
| **Get Product Detail**| `ProductController.java:44-47` | `getById(@PathVariable Long id)` | `ProductDto` | Calls `findByIdAndActiveTrue(id)`. |
| **Create Product** | `ProductController.java:73-76` | `createProduct(@Valid @RequestBody CreateProductRequest request)` | `ProductDto` (201) | Checks SKU uniqueness; links category. |
| **Update Product** | `ProductController.java:78-83` | `updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request)` | `ProductDto` (200) | Updates metadata; does NOT update stock or SKU. |
| **Update Inventory** | `ProductController.java:85-90` | `updateInventory(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request)` | `ProductDto` (200) | Modes: `ABSOLUTE` or `DELTA`. Rejects negative resultant stock. |
| **Delete Product** | `ProductController.java:92-96` | `deleteProduct(@PathVariable Long id)` | `Void` (204) | Sets `active = false` (soft-delete). |
| **List Categories** | `CategoryController.java:21-24` | `getAllCategories()` | `List<CategoryDto>` | Populates category selector in forms. |

---

### 3.2 Request & Response Data Contracts

#### A. List Products Contract (`GET /api/v1/products`)
* **Query Parameters (`BACKEND-CONFIRMED`):**
  * `page`: Integer (0-indexed, default `0`).
  * `size`: Integer (default `20`).
  * `sort`: String (e.g. `createdAt,desc` or `price,asc`, default `createdAt`).
* **Response Structure (`Page<ProductDto>`):**
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "Keychron Q1 Pro Custom Mechanical Keyboard",
        "description": "Wireless QMK/VIA custom mechanical keyboard with CNC aluminum body.",
        "sku": "TG-KBD-001",
        "price": 199.99,
        "stockQuantity": 35,
        "imageUrl": "https://example.com/images/q1pro.jpg",
        "category": {
          "id": 2,
          "name": "Keyboards & Mice",
          "description": "Precision mechanical keyboards and mice",
          "imageUrl": "https://example.com/images/cat-kbd.jpg",
          "active": true
        },
        "active": true,
        "createdAt": "2026-03-01T10:00:00",
        "updatedAt": "2026-03-15T12:00:00"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 20 },
    "totalElements": 48,
    "totalPages": 3,
    "last": false,
    "first": true,
    "size": 20,
    "number": 0,
    "empty": false
  }
  ```

#### B. Search Products Contract (`GET /api/v1/products/search`)
* **Query Parameters (`BACKEND-CONFIRMED`):**
  * `keyword`: String (required). Mapped to PostgreSQL `to_tsvector @@ plainto_tsquery('english', :keyword)` across `name` and `description`.
  * `page`: Integer (default `0`).
  * `size`: Integer (default `20`).
* **Response Structure:** Identical `Page<ProductDto>` structure filtered to matching active products.

#### C. Create Product Contract (`POST /api/v1/products`)
* **Headers:** `Content-Type: application/json`, `Authorization: Bearer <accessToken>`
* **Payload Structure (`CreateProductRequest.java:10-46`):**
  ```json
  {
    "name": "Sony WH-1000XM5 Wireless Headphones",
    "sku": "TG-AUD-002",
    "description": "Industry-leading noise canceling headphones with dual processors.",
    "price": 399.99,
    "stockQuantity": 14,
    "categoryId": 1,
    "imageUrl": "https://example.com/images/xm5.jpg"
  }
  ```
* **Validation Rules (`BACKEND-CONFIRMED`):**
  * `name`: `@NotBlank`, `@Size(min = 2, max = 200)`, `@Pattern("^(?=.*\\p{L})[\\p{L}\\d\\s\\-'.&()]+$")`.
  * `sku`: `@NotBlank`, `@Size(min = 3, max = 100)`, `@Pattern("^[A-Z0-9\\-]+$")` (uppercase alphanumeric + hyphens).
  * `description`: Optional, `@Size(max = 2000)`.
  * `price`: `@NotNull`, `@DecimalMin("0.01")`, `@Digits(integer = 8, fraction = 2)` (> 0).
  * `stockQuantity`: `@NotNull`, `@Min(0)` (&ge; 0).
  * `categoryId`: `@NotNull` (must match an existing active category).
  * `imageUrl`: Optional String URL.

#### D. Update Product Contract (`PATCH /api/v1/products/{id}`)
* **Headers:** `Content-Type: application/json`, `Authorization: Bearer <accessToken>`
* **Payload Structure (`UpdateProductRequest.java:9-28`):**
  ```json
  {
    "name": "Sony WH-1000XM5 Wireless Headphones (Midnight Blue)",
    "description": "Updated description with new driver specifications.",
    "price": 379.99,
    "imageUrl": "https://example.com/images/xm5-blue.jpg",
    "categoryId": 1,
    "active": true
  }
  ```
* **Validation Rules (`BACKEND-CONFIRMED`):**
  * All fields are optional (partial update).
  * `sku` is **omitted** (immutable).
  * `stockQuantity` is **omitted** (managed separately via inventory endpoint).
* **UI Form Scope vs. Backend DTO (`SEMANTIC ALIGNMENT`):**
  * Backend DTO `UpdateProductRequest` remains unchanged (includes optional `active` field).
  * **Edit Modal UI Scope:** The Edit Product modal form exposes strictly: `name`, `description`, `price`, `imageUrl`, `categoryId` (`sku` is displayed as read-only).
  * **No Dual Semantics for `active`:** The `active` field is **not** exposed as an editable UI control in the Edit modal. The Delete action (`DELETE /api/v1/products/{id}` &rarr; `active = false`) continues to be the single, canonical soft-delete mechanism, avoiding two conflicting UI actions for the same semantics.

#### E. Update Inventory Contract (`PATCH /api/v1/products/{id}/inventory`)
* **Headers:** `Content-Type: application/json`, `Authorization: Bearer <accessToken>`
* **Payload Structure (`InventoryUpdateRequest.java:9-27`):**
  ```json
  {
    "mode": "DELTA",
    "quantity": 10
  }
  ```
* **Validation Rules & Semantics (`BACKEND-CONFIRMED`):**
  * `mode`: `@NotNull`, enum `ABSOLUTE` or `DELTA`.
  * `quantity`: `@NotNull` Integer.
  * In `ABSOLUTE` mode: `quantity >= 0`. Directly resets stock to `quantity`.
  * In `DELTA` mode: `quantity` can be positive (stock arrival) or negative (manual reduction/damaged stock).
  * Resultant stock check (`ProductService.java:172-175`): `newQty = current + quantity`. If `newQty < 0`, backend rejects with **HTTP 422 Unprocessable Entity** (`InsufficientStockException`).
  * If `newQty > Integer.MAX_VALUE`, backend rejects with HTTP 422.

#### F. Soft Delete Contract (`DELETE /api/v1/products/{id}`)
* **Headers:** `Authorization: Bearer <accessToken>`
* **Response:** HTTP 204 No Content.
* **Semantics:** Sets `product.active = false`. Because `findAllByActiveTrue` only returns active entities, the soft-deleted product is immediately removed from the catalog listing upon refresh.

---

## 4. UI Architecture & Screen Anatomy

### 4.1 Canonical Stitch Screen Identification
* **Screen ID:** `c3bc0de1fd40430ea05f410c102981e3`
* **Title:** `TECHGEAR — Admin Products`
* **Screenshot Artifact:** `projects/113306237264437644/files/44a18924f54240bf94d4d0dd3e917cc6`
* **HTML Source Artifact:** `projects/113306237264437644/files/8cd18815fe474320997d81acab1299b2`
* **Target Viewport:** Desktop `1440px` (max canvas `max-w-7xl`, responsive table with compact row density).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR // ADMIN                    DASHBOARD  [PRODUCTS]  CATEGORIES  USERS  ORDERS  PAYMENTS │ admin@techgear.io [ADMIN]  LOG OUT │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PORTAL // CATALOG MANAGEMENT                                                                      │
│ PRODUCTS                                                                      [ + NEW PRODUCT ]  │
│ Catalog inventory, pricing, stock adjustments, and product CRUD operations.                      │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ [Search by keyword (name or description)...] [SEARCH] [Clear]                48 ACTIVE PRODUCTS  │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ PRODUCT NAME                SKU          CATEGORY        PRICE    STOCK   STATUS   ACTIONS   │ │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ Keychron Q1 Pro Custom...   TG-KBD-001   Keyboards & M   $199.99     35   ACTIVE   [E][S][D] │ │
│ │ Sony WH-1000XM5 Wireless..  TG-AUD-002   Audio           $399.99     14   ACTIVE   [E][S][D] │ │
│ │ Logitech MX Master 3S...    TG-MSE-003   Keyboards & M    $99.99     52   ACTIVE   [E][S][D] │ │
│ │ Dell UltraSharp 27 4K...    TG-DSP-004   Displays        $579.99      8   ACTIVE   [E][S][D] │ │
│ │ CalDigit TS4 Thunderbolt 4  TG-ACC-005   Accessories     $399.95      0   ACTIVE   [E][S][D] │ │
│ └──────────────────────────────────────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Showing 1–5 of 48 products (Page 1 of 10)                           [← PREV] [1] [2] [3] [NEXT →] │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ TECHGEAR OPERATIONAL ADMINISTRATION // V1.0                  ENDPOINT SCOPE: SPRING BOOT 3.2     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

> *Wireframe Data Note:* "48 ACTIVE PRODUCTS" and "Showing 1–5 of 48 products" are **DESIGN MOCK DATA**. At runtime, this binds dynamically to `Page<ProductDto>.totalElements`. Do not hard-code.

---

## 5. Detailed Component Specifications

### 5.1 Top Navigation & Header Station
* **Top Admin Header:** 64px sticky bar (`#FFFFFF` with `1px solid #E7E5E4`).
  * Wordmark: `TECHGEAR // ADMIN` (Plus Jakarta Sans `15px` bold).
  * Active Navigation Tab: `PRODUCTS` is active with high-contrast text `#1C1917` and a 2px hairline bottom indicator.
  * Right Rail: Administrator identity `admin@techgear.io [ADMIN]` and `LOG OUT` action.
* **Header Station:**
  * Breadcrumb: `PORTAL // CATALOG MANAGEMENT` in JetBrains Mono `11px` text `#78716C` uppercase tracking-wider.
  * Title: `PRODUCTS` in Plus Jakarta Sans `28px` font-bold text `#1C1917` uppercase tracking-tight.
  * Subtitle: `Catalog inventory, pricing, stock adjustments, and product CRUD operations.` in Plus Jakarta Sans `14px` text `#57534E`.
  * Primary Action: `[ + NEW PRODUCT ]` button (bg `#1C1917`, text `#FAFAF9`, 4px radius, Plus Jakarta Sans `12px` font-semibold px-4 py-2.5 hover:bg-[#292524]).

---

### 5.2 Search & Control Strip
* **Search Input:**
  * Placeholder: `Search by keyword (name or description)...`
  * Styling: bg `#FFFFFF`, border `1px solid #E7E5E4`, text `#1C1917`, font-mono `12px`, h-10 px-3.5 rounded `[4px]` w-80 focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706].
* **Search Button:**
  * Label: `SEARCH` in JetBrains Mono `11px` font-semibold uppercase text `#FAFAF9` bg `#1C1917` h-10 px-4 rounded `[4px]`.
  * Behavior: Submits `GET /api/v1/products/search?keyword={val}&page=0`.
* **Clear Filter Trigger:**
  * Label: `Clear search` (text `#78716C` hover:text-[#1C1917] font-mono `11px` underline cursor-pointer). Resets search query and re-fetches `GET /api/v1/products?page=0`.
* **Live Entity Count Vitrine:**
  * **DESIGN MOCK DATA:**
    `48 ACTIVE PRODUCTS`
  * **Runtime:**
    `Page<ProductDto>.totalElements`
  * **Rule:** Không hard-code 48. Bound dynamically to `{totalElements} ACTIVE PRODUCTS`.

---

### 5.3 Products Data Table
* **Container:** bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`, overflow-hidden, `shadow-none`.
* **Table Header Row (`h-10 bg-[#FAFAF9] border-b border-[#E7E5E4] px-4`):**
  * `PRODUCT NAME`: Left-aligned, Plus Jakarta Sans `11px` font-semibold text `#78716C` uppercase tracking-wider.
  * `SKU`: JetBrains Mono `11px` text `#78716C` uppercase.
  * `CATEGORY`: Plus Jakarta Sans `11px` text `#78716C` uppercase.
  * `PRICE`: Right-aligned, JetBrains Mono `11px` text `#78716C` uppercase.
  * `STOCK`: Right-aligned, JetBrains Mono `11px` text `#78716C` uppercase.
  * `STATUS`: Center-aligned, JetBrains Mono `11px` text `#78716C` uppercase.
  * `ACTIONS`: Right-aligned, JetBrains Mono `11px` text `#78716C` uppercase.

* **Table Rows (`h-12 border-b border-[#E7E5E4] px-4 hover:bg-[#FAFAF9] transition-colors`):**
  * **Product Name:** Plus Jakarta Sans `13px` font-medium text `#1C1917` truncate max-w-xs.
  * **SKU:** JetBrains Mono `12px` font-semibold text-[#1C1917] bg-[#F5F5F4] px-2 py-0.5 rounded `[2px]`.
  * **Category Name:** Plus Jakarta Sans `12px` text `#57534E` (derived from `category.name`).
  * **Price:** Tabular JetBrains Mono `13px` font-medium text `#1C1917` (e.g. `$199.99`).
  * **Stock Quantity & Status Display:** Tabular JetBrains Mono `13px` text `#1C1917`.
    * **No Invented Threshold:** Backend does not define a low-stock threshold; the visual "low stock" / "sắp hết hàng" rule is removed. Không tạo threshold mới.
    * **Verified Visual Rules:**
      * **OUT OF STOCK** khi `stockQuantity = 0`: Styled with red out-of-stock badge / numeral (`bg-[#FEE2E2] text-[#991B1B]` `0` / `OUT OF STOCK`).
      * **IN STOCK** khi `stockQuantity > 0`: Hiển thị số lượng tồn kho `stockQuantity` (e.g. `35` / `IN STOCK`).
  * **Status:** Badge `ACTIVE` in JetBrains Mono `10px` bg-[#DCFCE7] text-[#166534] px-2 py-0.5 rounded `[2px]`.
  * **Actions Rail (Right-Aligned):**
    * `[ EDIT ]`: Outline button (bg-white border `1px solid #E7E5E4` text `#1C1917` text-xs font-mono px-2.5 py-1 rounded `[4px]` hover:bg-[#F5F5F4]). Triggers Edit Product Modal.
    * `[ STOCK ]`: Outline button (bg-white border `1px solid #E7E5E4` text-[#D97706] text-xs font-mono px-2.5 py-1 rounded `[4px]` hover:bg-[#F5F5F4]). Triggers Inventory Update Modal.
    * `[ DELETE ]`: Outline button (bg-white border `1px solid #E7E5E4` text-[#DC2626] text-xs font-mono px-2.5 py-1 rounded `[4px]` hover:bg-[#FEF2F2]). Triggers Soft-Delete Confirmation Modal.

---

### 5.4 Pagination Rail
* **Container:** `flex justify-between items-center py-4 text-xs font-mono text-[#57534E]`.
* **Summary Text:**
  * **DESIGN MOCK DATA:** `Showing 1–20 of 48 products (Page 1 of 3)`
  * **Runtime:** Bound dynamically to `Page<ProductDto>` metadata (`totalElements`, `size`, `number`, `totalPages`). Do not hard-code.
* **Navigation Controls:**
  * `[ ← PREV ]`: Disabled if `first == true`.
  * Page Numbers: `[ 1 ]` (active: bg `#1C1917` text `#FAFAF9`), `[ 2 ]`, `[ 3 ]` (inactive: bg `#FFFFFF` border `1px solid #E7E5E4`).
  * `[ NEXT → ]`: Disabled if `last == true`.

---

## 6. Modal & Action Workflows

To maintain maximum usability and avoid complex multi-screen routing, CRUD actions on `/admin/products` operate via clean, focused modal dialogues:

### 6.1 Create Product Modal Workflow
* **Trigger:** Click `[ + NEW PRODUCT ]`.
* **Modal Architecture:**
  * Width: `max-w-xl`, surface `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`.
  * Title: `CREATE PRODUCT` (Plus Jakarta Sans `18px` font-bold text `#1C1917`).
* **Form Fields (`CreateProductRequest`):**
  1. **Product Name (`name`):** Text input, required, placeholder e.g. `Sony WH-1000XM5 Wireless Headphones`.
  2. **SKU (`sku`):** Text input, required, uppercase alphanumeric e.g. `TG-AUD-002` (helper: `Must be uppercase alphanumeric with hyphens. Immutable after creation.`).
  3. **Category (`categoryId`):** Dropdown selector populated dynamically from `GET /api/v1/categories`. Required.
  4. **Price (`price`):** Number input, step `0.01`, required, prefix `$`. Must be > 0.
  5. **Initial Stock Quantity (`stockQuantity`):** Integer input, required, min `0`.
  6. **Image URL (`imageUrl`):** Text input, optional, placeholder `https://...`.
  7. **Description (`description`):** Textarea, optional, max 2000 chars.
* **Actions:**
  * `[ CANCEL ]`: Closes modal without mutation.
  * `[ CREATE PRODUCT ]`: Executes `POST /api/v1/products`.
* **On Success (HTTP 201):** Closes modal, shows success notification, and refreshes catalog table (`GET /api/v1/products`).

---

### 6.2 Edit Product Metadata Modal Workflow
* **Trigger:** Click `[ EDIT ]` on a product table row.
* **Modal Architecture:**
  * Title: `EDIT PRODUCT // TG-KBD-001` (Header displays SKU as immutable reference).
* **Form Fields (`UpdateProductRequest`):**
  * **Editable Metadata:**
    1. **Product Name (`name`):** Text input, pre-populated with current name.
    2. **Description (`description`):** Textarea, pre-populated with current description.
    3. **Price (`price`):** Number input, pre-populated with current price.
    4. **Image URL (`imageUrl`):** Text input, pre-populated with current URL.
    5. **Category (`categoryId`):** Dropdown selector pre-selected with current category.
  * **Read-Only Reference:**
    * **SKU (`sku`):** Displayed as **read-only disabled badge** (*"SKU is immutable"*).
* **Semantic Policy on `active` & Separation:**
  * **`active` is NOT exposed in the Edit Modal:** `active` is not presented as an editable field in this form. The Delete action (`DELETE /api/v1/products/{id}` &rarr; `active = false`) continues to be the only way to soft-delete a product. Do not create two separate UI actions for the exact same business semantics. The backend DTO (`UpdateProductRequest`) remains unchanged.
  * **Stock Quantity is NOT present:** Inventory adjustments are strictly processed via the dedicated `[ STOCK ]` modal.
* **Actions:**
  * `[ CANCEL ]`: Closes modal.
  * `[ SAVE CHANGES ]`: Executes `PATCH /api/v1/products/{id}`.
* **On Success (HTTP 200):** Closes modal, refreshes table, updates product cache.

---

### 6.3 Update Inventory Modal Workflow
* **Trigger:** Click `[ STOCK ]` on a product table row.
* **Modal Architecture:**
  * Width: `max-w-md`.
  * Title: `UPDATE INVENTORY` (Plus Jakarta Sans `18px` font-bold text `#1C1917`).
  * Specimen Info: Displays product name, SKU, and **Current Stock:** `35 units` (JetBrains Mono font-bold).
* **Form Fields (`InventoryUpdateRequest`):**
  1. **Adjustment Mode (`mode`):** Segmented selector:
     * `[ ABSOLUTE ]` (Set exact stock level)
     * `[ DELTA ]` (Add or subtract units)
  2. **Quantity (`quantity`):**
     * If `ABSOLUTE` mode: Integer input &ge; 0. Placeholder e.g. `50`.
     * If `DELTA` mode: Signed integer input. Placeholder e.g. `+10` or `-5`.
  3. **Projected Final Stock (UI-Only Preview):**
     * Giữ "Projected Final Stock" chỉ như một client-side **UI-only preview** (ví dụ: `Projected Final Stock: 45 units` in JetBrains Mono):
       * **`ABSOLUTE`:** `projected = requested quantity`
       * **`DELTA`:** `projected = current stock + delta`
     * **Source of Truth & Backend Decoupling:**
       * `projected` **không phải backend field** (payload `InventoryUpdateRequest` chỉ gửi `mode` và `quantity`).
       * Backend response (`ProductDto.stockQuantity`) vẫn là **source of truth**.
       * **Không giả định preview = successful update**; UI chỉ cập nhật state sau khi nhận HTTP 200 từ backend.
* **Validation & Guard:**
  * If `projected < 0`, submit button bị disabled kèm cảnh báo inline: *"Resultant stock cannot be reduced below zero."*
* **Actions:**
  * `[ CANCEL ]`: Closes modal.
  * `[ UPDATE STOCK ]`: Executes `PATCH /api/v1/products/{id}/inventory`.
* **On Success (HTTP 200):** Closes modal, updates table row stock figure immediately.

---

### 6.4 Soft-Delete Confirmation Modal Workflow
* **Trigger:** Click `[ DELETE ]` on a product table row.
* **Modal Architecture:**
  * Width: `max-w-md`.
  * Title: `DEACTIVATE PRODUCT?`
* **Confirmation Body:**
  * *"Are you sure you want to deactivate **Keychron Q1 Pro Custom Mechanical Keyboard** (`TG-KBD-001`)? The product will be soft-deleted and immediately hidden from the active catalog."*
  * Operational Note: Historical orders referencing this product remain intact and unaffected.
* **Actions:**
  * `[ CANCEL ]`: Closes modal.
  * `[ DEACTIVATE ]`: Primary destructive button (bg `#DC2626`, text `#FFFFFF`, 4px radius). Executes `DELETE /api/v1/products/{id}`.
* **On Success (HTTP 204):** Closes modal, product is removed from the active list view, success alert displayed.

---

## 7. Category Integration & Cross-Navigation

* **Category Selector Data Source:** Dropdown options in Create/Edit modals are fetched dynamically via `GET /api/v1/categories` (`List<CategoryDto>`).
* **No Category Drawer:** To adhere strictly to approved architecture, there is **no Category Drawer** or inline category creation workflow inside `/admin/products`.
* **Cross-Navigation to Categories:** If an admin needs to create a new category taxonomy node, they navigate directly to `/admin/categories`.

---

## 8. Client-Side State, Validation & Error Handling

### 8.1 Loading States
* **Initial Catalog Fetch:** Renders 5 skeleton table rows with `animate-pulse` placeholders (`h-12 bg-[#F5F5F4]`) while `GET /api/v1/products` is pending.
* **Action Pending:** Buttons display loading state (`Creating...`, `Saving...`, `Updating...`) and prevent double-submission.

### 8.2 Error Handling & HTTP Status Mapping

| HTTP Status | Backend Cause | Client UX Handling |
| :--- | :--- | :--- |
| **HTTP 400 Bad Request** | Validation failed (`MethodArgumentNotValidException`) | Highlights failing form fields with inline red helper text extracted from the `errors` map. |
| **HTTP 404 Not Found** | Product or Category ID not found | Displays notification toast: *"Product or Category reference no longer exists."* |
| **HTTP 409 Conflict** | SKU already exists (`ProductAlreadyExistsException`) | Highlights the `sku` input field with: *"Product with SKU already exists. Please use a unique SKU."* |
| **HTTP 422 Unprocessable Entity** | Insufficient stock on delta reduction (`InsufficientStockException`) | Modal displays error banner: *"Cannot reduce inventory below 0 units."* |
| **HTTP 401 Unauthorized** | Token expired or invalid | Clears auth state and navigates to `/login` (FE-15). |
| **HTTP 403 Forbidden** | User lacks `ADMIN` role | Displays inline Access Denied banner; does NOT log user out. |
| **Network Error / 500** | Server failure | Inline error card above table with `[ RETRY ]` button. |

---

## 9. Responsive Specifications

* **Desktop (&ge; 1280px):** Full 7-column data table with complete SKUs, categories, right-aligned monetary and stock figures, and inline action button group.
* **Tablet (768px – 1279px):** Horizontal scrolling enabled on table container (`overflow-x-auto`) or category name truncated, maintaining min-width `900px` for tabular alignment.
* **Mobile (< 768px):**
  * Table transforms into compact stacked product cards.
  * Each card displays product name, SKU badge, price, stock indicator, and a full-width action bar with `[ EDIT ]`, `[ STOCK ]`, and `[ DELETE ]` touch targets (&ge; 44px).
  * Search bar expands to 100% width.

---

## 10. Design System Token Compliance

| Token Dimension | Value | Application Context |
| :--- | :--- | :--- |
| **Canvas Background** | `#FAFAF9` | Primary page underlay (Warm Alabaster). |
| **Surface Background** | `#FFFFFF` | Table surface, modal dialogues, input fields. |
| **Milled Neutral Pad**| `#F5F5F4` | Inset SKU badges, table header row `#FAFAF9`. |
| **Hairline Dividers** | `#E7E5E4` | 1px border on table rows, modal perimeters, inputs. |
| **Primary Solids** | `#1C1917` | Headers, product titles, primary action buttons. |
| **Secondary Typography**| `#57534E` | Subtitles, category labels, pagination summary. |
| **Metadata Typography** | `#78716C` | JetBrains Mono table headers, breadcrumbs, SKUs. |
| **Functional Accent** | `#D97706` | Status highlights, interactive focus rings. |
| **Destructive Action** | `#DC2626` | Delete action button, deactivation modal confirm. |
| **Corner Radii** | `4px` / `6px` | `4px` on buttons, inputs, badges; `6px` on container panels. |
| **Elevation** | `shadow-none` | Absolutely flat surfaces; zero floating drop shadows. |

---

## 11. Final Audit & Verification Checklist

- [x] **All Product Endpoints Verified:** `GET /products`, `GET /products/search`, `GET /products/{id}`, `POST /products`, `PATCH /products/{id}`, `PATCH /products/{id}/inventory`, `DELETE /products/{id}`.
- [x] **Request DTOs Verified:** `CreateProductRequest` and `UpdateProductRequest` audited from source code.
- [x] **Inventory DTO Verified:** `InventoryUpdateRequest` audited; `ABSOLUTE` and `DELTA` modes verified.
- [x] **Response DTO Verified:** `ProductDto` mapped 1-to-1 with backend.
- [x] **Validation Rules Verified:** Regex patterns, `@NotBlank`, `@Size`, `@Digits`, `@DecimalMin`, `@Min` documented.
- [x] **Pagination & Sorting Verified:** Spring Data `Pageable` parameters documented.
- [x] **Search Parameter Verified:** Exact parameter `@RequestParam String keyword` documented.
- [x] **Category Relationship Verified:** Populated via `GET /api/v1/categories`; no Category Drawer.
- [x] **Product vs. Inventory Separated:** Stock is **not** editable via metadata `PATCH /products/{id}`; handled exclusively via `/inventory`.
- [x] **SKU Immutability Verified:** `sku` cannot be altered after creation (read-only in Edit modal).
- [x] **Soft Delete Verified:** `DELETE` sets `active = false`; product excluded from active lists.
- [x] **Active Product Count Clarification:** Documented `48 ACTIVE PRODUCTS` strictly as `DESIGN MOCK DATA`; runtime binds to `Page<ProductDto>.totalElements` (never hard-coded).
- [x] **Stock Status Grounded:** Removed visual "low stock" rule due to absence of backend threshold. Exclusively uses `OUT OF STOCK` when `stockQuantity = 0` and `IN STOCK` when `stockQuantity > 0`.
- [x] **Projected Final Stock UI-Only:** Documented as client-side UI-only preview (`ABSOLUTE`: `projected = requested quantity`, `DELTA`: `projected = current stock + delta`). Confirmed not a backend field; backend response is source of truth.
- [x] **Edit Modal Active Field Excluded:** Form fields limited to `name`, `description`, `price`, `imageUrl`, `categoryId` (`sku` read-only). `active` is not exposed as an editable UI field; single soft-delete path preserved; backend DTO unchanged.
- [x] **Security & Roles Verified:** Mutations require `ADMIN` role.
- [x] **No Enterprise Bloat:** No CSV, bulk edit, variants, suppliers, or warehouse systems.
- [x] **Canonical Stitch Screen Created:** Screen ID `c3bc0de1fd40430ea05f410c102981e3` generated in project `113306237264437644`.
- [x] **Code & Infrastructure Integrity:** Zero frontend code written, zero packages added, zero backend modifications, zero Git commands executed.
- [x] **Existing Docs Untouched:** `ADMIN-01`, `ADMIN-02`, and `FE-01` through `FE-15` preserved completely.

---

**ADMIN-03 STATUS: READY FOR REVIEW**
