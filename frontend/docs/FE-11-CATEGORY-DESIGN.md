# TECHGEAR — FE-11 Category Page Design Specification

> **Document ID:** FE-11-CATEGORY-DESIGN  
> **Route:** `/category/:categoryId`  
> **Target Audience:** Public / All Customers  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `cdfa9eee3f92454bb9474c6fbba76e4f`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Category Page** (`/category/:categoryId`) is the customer's focused portal into a single product category. It solves one dedicated task:
$$\text{CATEGORY} \longrightarrow \text{PRODUCT LIST}$$

It presents the category name, its factual description, and the filtered grid of products belonging to that category, backed directly by `product-service` endpoints.

### Strict YAGNI Boundaries
* **Not a Duplicate Shop:** The Category page does not attempt to replicate full multi-faceted filtering. It focuses solely on products belonging to the selected category.
* **No Speculative Filters:** The backend category product query (`findAllByCategoryIdAndActiveTrue`) accepts only pagination and sorting. It does **not** accept price range sliders, brand check boxes, rating filters, or stock toggles. Consequently, **no filter sidebar or filter chips** are introduced.
* **No Unverified Marketing Copy:** No artificial marketing slogans ("Curated for high-performance professionals", "24 precision items", "Best-in-class collection") unless returned directly in `CategoryDto.description`.
* **Zero Speculative Features:** No product comparison, wishlist toggles, reviews, discount badges, trending products, or AI recommendations.
* **Preservation of Existing Work:** Screens FE-01 through FE-10 remain strictly unchanged.

---

## 2. Route & Navigation Architecture

* **Canonical Route:** `/category/:categoryId`
  * Example: `/category/2` (Keyboards & Mouse)
* **Route Parameter:**
  * `:categoryId` *(Long)*: Unique numerical identifier of the category extracted from the URL.
* **Breadcrumbs:**
  * Simple commerce hierarchy: `SHOP / CATEGORIES / {CATEGORY_NAME}`
  * Standard navigation behavior: Clicking `SHOP` navigates to `/shop`; clicking `CATEGORIES` navigates to `/shop`.
  * Free of pseudo-technical telemetry or system labeling.
* **Header Search Field:**
  * Consistent with FE-03/FE-10: Clean rectangular input field with subtle 4px border radius (`rounded-[4px]`), 1px solid `#E7E5E4` border, and `#FFFFFF` background.
  * No `⌘K` keyboard shortcut badge; no oversized pill styling.
* **Category Rail Tabs:**
  * Reuses the horizontal segmented category rail pattern established in FE-03:
    * `[ All Equipment ]` &rarr; `/shop`
    * Category tabs &rarr; `/category/:categoryId` (active tab: `#1C1917` solid background, `#FFFFFF` text)

---

## 3. Backend Verification & Integration Contract

### 3.1 Verified Backend Source Code References

| Component | Code Reference | Implementation Detail |
| :--- | :--- | :--- |
| **Category List** | `CategoryController.java:21-24` | `GET /api/v1/categories` &rarr; returns `ResponseEntity<List<CategoryDto>>` via `categoryService.getAllActiveCategories()`. Strictly filters `active = true` via `CategoryRepository.findAllByActiveTrue()`. |
| **Category Detail** | `CategoryController.java:26-29` | `GET /api/v1/categories/{id}` &rarr; returns `ResponseEntity<CategoryDto>` via `categoryService.getCategoryById(id)`. |
| **Category Service** | `CategoryService.java:34-36` | `categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(...))` |
| **Product Controller** | `ProductController.java:30-35` | `GET /api/v1/products/category/{categoryId}` with `@PageableDefault(size = 20) Pageable pageable` |
| **Product Service** | `ProductService.java:70-75` | `@Cacheable("productsList", key = "'cat-' + ...")` &rarr; `productRepository.findAllByCategoryIdAndActiveTrue(categoryId, pageable).map(this::toDto)` |
| **Product Repository** | `ProductRepository.java:30` | `Page<Product> findAllByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable)` |
| **Gateway Routing** | `application.yml:55-66` | Route `product-service` exposes `/api/v1/categories/**` and `/api/v1/products/category/**` publicly with rate limiting |

### 3.2 Category List API Verification (`GET /api/v1/categories`)

* **Endpoint:** `GET /api/v1/categories`
* **Response Structure:** Array of category transfer objects `List<CategoryDto>`.
* **Verified Contract Fields (`CategoryDto`):**
  * `id` *(Long)*: Unique category ID.
  * `name` *(String)*: Category display name (e.g. `"Audio"`, `"Keyboards & Mouse"`).
  * `description` *(String)*: Category description string (nullable).
  * `imageUrl` *(String)*: Category banner/pad image URL (nullable).
  * `active` *(boolean)*: Active status flag.
  * `createdAt` *(LocalDateTime)*: Category creation timestamp.
* **Active/Inactive Behavior:**
  * `CategoryService.getAllActiveCategories()` calls `categoryRepository.findAllByActiveTrue()`.
  * Soft-deleted categories (`active = false`) are automatically excluded at the database query level.
* **Category Rail Data Binding & Architecture Decision:**
  * The category navigation rail items dynamically correspond to the active categories returned by `GET /api/v1/categories` (plus `[ All Equipment ]` prepended).
  * **Reuse Decision:** In client runtime, this list can either be fetched directly or sourced from the global layout category cache loaded in FE-01/FE-03, avoiding redundant network requests during category tab transitions. No new API or custom endpoint is introduced.

### 3.3 Dual API Fetch Orchestration (Category Page Mount)

When `/category/:categoryId` mounts, the page coordinates two API calls:

```
                  ┌──> 1. GET /api/v1/categories/{categoryId} ───────> Category Metadata (Name, Description)
Client Mount ─────┤
                  └──> 2. GET /api/v1/products/category/{categoryId} ─> Product Grid (Page<ProductDto>)
```

#### Request 1: Category Metadata
* **Endpoint:** `GET /api/v1/categories/{id}`
* **Response Contract (`CategoryDto`):**
  ```json
  {
    "id": 2,
    "name": "Keyboards & Mouse",
    "description": "Mechanical keyboards and precision mice",
    "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3",
    "active": true,
    "createdAt": "2026-03-01T10:00:00"
  }
  ```

#### Request 2: Category Product Listing
* **Endpoint:** `GET /api/v1/products/category/{categoryId}?page=0&size=20&sort=createdAt,desc`
* **Query Parameters:**
  * `page` *(optional, default 0)*: 0-indexed page number.
  * `size` *(optional, default 20)*: Page size.
  * `sort` *(optional, default unassigned)*: Standard Spring Data sort parameter (e.g. `createdAt,desc`, `price,asc`, `price,desc`, `name,asc`).
* **Response Contract (`Page<ProductDto>`):**
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "Keychron Q1 Pro Wireless Custom Mechanical Keyboard",
        "description": "Full aluminum 75% layout QMK/VIA wireless custom mechanical keyboard.",
        "sku": "KB-KEY-Q1P-BLK",
        "price": 4990000.00,
        "stockQuantity": 15,
        "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3",
        "category": {
          "id": 2,
          "name": "Keyboards & Mouse",
          "description": "Mechanical keyboards and precision mice"
        },
        "active": true,
        "createdAt": "2026-03-01T10:00:00",
        "updatedAt": "2026-03-15T12:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": { "sorted": true, "unsorted": false, "empty": false }
    },
    "totalElements": 4,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true,
    "empty": false
  }
  ```

### 3.3 Error & Edge Case Behaviors

1. **Category Not Found (HTTP 404):**
   * If `GET /api/v1/categories/{id}` is called with an invalid or non-existent ID, `CategoryService.findOrThrow(id)` throws `CategoryNotFoundException` &rarr; returns HTTP 404 with:
     ```json
     {
       "status": 404,
       "message": "Category not found with id: 999",
       "timestamp": "2026-10-07T13:30:00"
     }
     ```
   * Client switches immediately to the **Category Not Found (404)** state.
2. **Empty Category (HTTP 200, `empty: true`):**
   * If a valid category exists but currently contains zero active products, `ProductRepository.findAllByCategoryIdAndActiveTrue` returns HTTP 200 with `content: []`, `totalElements: 0`, `empty: true`.
   * Client renders the category header (name & description) followed by the **Empty Category** state.

---

## 4. Architectural Verification: Grounded vs. Design Decisions vs. Mock Data

| Aspect | Classification | Grounding & Rationale |
| :--- | :--- | :--- |
| **Endpoint: List Active Categories** | `BACKEND-CONFIRMED` | `CategoryController.getAllCategories()` at `/api/v1/categories`. Strictly filters `active = true` via database query. |
| **Endpoint: Get Category Detail** | `BACKEND-CONFIRMED` | `CategoryController.getCategoryById(Long id)` at `/api/v1/categories/{id}`. |
| **Endpoint: Get Products by Cat** | `BACKEND-CONFIRMED` | `ProductController.getByCategory(Long categoryId, Pageable pageable)` at `/api/v1/products/category/{categoryId}`. |
| **Pagination & Sorting** | `BACKEND-CONFIRMED` | Spring Data `Pageable` parameters supported directly on `ProductRepository.findAllByCategoryIdAndActiveTrue`. |
| **Field: `CategoryDto.description`** | `BACKEND-CONFIRMED` | `CategoryDto` includes `description` (`String`). Displayed in header only if non-null. |
| **Absence of Custom Filters** | `BACKEND-CONFIRMED` | Backend does not support brand/price/stock filtering inside the category endpoint. No filter sidebar exists. |
| **VND Currency Formatting** | `BACKEND-CONFIRMED` | `ProductDto.price` (`BigDecimal`) formatted in Vietnamese Dong according to TECHGEAR platform standard (`4.990.000 ₫`). |
| **Horizontal Category Rail** | `UI DESIGN DECISION` | Direct reuse of FE-03 category rail tabs to provide seamless navigation across categories without sidebar bloat. Bound from `GET /api/v1/categories` or global layout cache. |
| **Empty & 404 States** | `UI DESIGN DECISION` | Clean industrial feedback states directing customer back to `/shop`. |
| **Sample Products & "SHOWING 4 ITEMS"** | `DESIGN MOCK DATA` | Screen ID `cdfa9eee3f92454bb9474c6fbba76e4f` renders 4 keyboard/mouse items as design mockup presentation. Runtime binds dynamically from API response. |

---

## 5. Screen Layout & Visual Hierarchy

The layout maintains the **Obsidian & Paper** design aesthetic with a strict maximum width of `1280px` (`max-w-7xl`), centered margins on `#FAFAF9` warm alabaster canvas.

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared 64px Sticky Nav — TECHGEAR Logo, Cart [0], Account) │
├────────────────────────────────────────────────────────────────────────┤
│ 2. BREADCRUMBS & CATEGORY NAVIGATION RAIL                              │
│    SHOP / CATEGORIES / KEYBOARDS & MOUSE                               │
│    [ All ] [ Audio ] [ Keyboards & Mouse (Active) ] [ Gaming ] ...    │
├────────────────────────────────────────────────────────────────────────┤
│ 3. CATEGORY HEADER (Minimalist Editorial)                              │
│    Keyboards & Mouse                                                   │
│    "Mechanical keyboards and precision mice"                           │
├────────────────────────────────────────────────────────────────────────┤
│ 4. CATALOG TOOLBAR (Strict YAGNI)                                      │
│    SHOWING 4 ITEMS                             SORT BY: [ Newest ▼ ]   │
├────────────────────────────────────────────────────────────────────────┤
│ 5. PRODUCT GRID (4-Column Precision Hardware Grid)                     │
│    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ │
│    │ Keychron Q1  │ │ Keychron V1  │ │ NuPhy Air75  │ │ MX Master 3S │ │
│    │ 4.990.000 ₫  │ │ 2.100.000 ₫  │ │ 2.990.000 ₫  │ │ 2.490.000 ₫  │ │
│    │ IN STOCK     │ │ IN STOCK     │ │ 3 UNITS LEFT │ │ IN STOCK     │ │
│    └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│ 6. PAGINATION (Minimalist Bar — Enabled when totalPages > 1)           │
│    [ ← PREVIOUS ]   [ 01 ]   [ NEXT → ]   // PAGE 01 OF 01             │
├────────────────────────────────────────────────────────────────────────┤
│ 7. FOOTER (Shared Customer Utility Blueprint)                          │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Breadcrumbs & Category Tabs Rail
* **Breadcrumb Trail:** `SHOP / CATEGORIES / {CategoryDto.name.toUpperCase()}`
  * Standard commerce breadcrumb navigation: JetBrains Mono `11px`, `#78716C`, uppercase, tracking `+0.05em`.
  * Clean hierarchy without telemetry, system, or architectural metadata tags.
* **Category Tabs Rail:**
  * Reuses FE-03 segmented tab list: `gap-2 pt-6 pb-6 border-b border-[#E7E5E4]`.
  * Active tab (`#1C1917` solid background, `#FFFFFF` text, `rounded-[4px]`) visually identifies current category.
  * Inactive tabs (`#FFFFFF` background, `#57534E` text, `border border-[#E7E5E4]`, hover: border `#A8A29E`).

### 5.2 Category Editorial Header
* **Title:** `{CategoryDto.name}` (Plus Jakarta Sans `36px`, Weight 700, `#1C1917`, tracking `-0.03em`).
* **Description:** `{CategoryDto.description}` (Plus Jakarta Sans `15px`, `#57534E`, line-height `1.6`, max-w `56ch`).
  * Only rendered if `CategoryDto.description` is provided and non-empty. Zero speculative marketing copy.
* **No Pseudo-Technical Eyebrow:** Does **not** include artificial labels like `CATEGORY // 02` or system codes. The category title directly leads into the descriptive text.

### 5.3 Header Search Field Specification
* **Styling Rules:** Follows Obsidian & Paper rectangular control guidelines (`rounded-[4px]`, `1px solid #E7E5E4`, `#FFFFFF` surface).
* **No Shortcut Badge:** Does **not** introduce any keyboard shortcut badge (`⌘K`) or excessive pill rounding, maintaining visual parity with FE-03 and FE-10.

### 5.4 Catalog Toolbar
* **Spacing:** `py-4 flex items-center justify-between border-b border-[#E7E5E4] mb-8`.
* **Item Counter:**
  * `SHOWING {totalElements} ITEMS` (JetBrains Mono `11px`, Medium, `#78716C`, tracking `+0.05em`).
  * *"SHOWING 4 ITEMS" in Stitch is strictly design mock presentation.* Runtime reads `totalElements` from the API response.
* **Sort Selector:**
  * Label: `SORT BY:` in JetBrains Mono `11px` (`#78716C`).
  * Native styled select element (`h-9 px-3 bg-white border border-[#E7E5E4] rounded-[4px] text-[13px] text-[#1C1917]`):
    1. `Newest Arrivals` (`sort=createdAt,desc`)
    2. `Price: Low to High` (`sort=price,asc`)
    3. `Price: High to Low` (`sort=price,desc`)
    4. `Alphabetical: A–Z` (`sort=name,asc`)

### 5.5 Product Grid & Card Anatomy (Reused from FE-03)
* **Grid:** `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6`.
* **Card Anatomy:**
  * **Canvas:** `#FFFFFF` background, `1px solid #E7E5E4` border, `4px` corner radius.
  * **Product Image Pad:** Aspect ratio `1:1`, `#F5F5F4` background, `1px solid #E7E5E4` bottom hairline border, centered object-contain image.
  * **Category Tag:** JetBrains Mono `10px`, uppercase, `#78716C`, tracking `+0.05em` (`KEYBOARDS & MOUSE`).
  * **Product Name:** Plus Jakarta Sans `14px`, font-semibold, `#1C1917`, line-clamp-2. Clickable link to `/product/{id}`.
  * **SKU:** JetBrains Mono `11px`, `#A8A29E`.
  * **Price:** JetBrains Mono `15px`, font-semibold, `#1C1917`, `tabular-nums`.
    * Formatted in Vietnamese Dong: dot thousand separators with `₫` suffix (e.g. `4.990.000 ₫`, `2.100.000 ₫`).
    * Runtime binds dynamically from `ProductDto.price`.
  * **Stock Availability Indicator:**
    * `stockQuantity > 5`: `IN STOCK` (JetBrains Mono `10px`, `#15803D`, emerald dot).
    * `1 <= stockQuantity <= 5`: `{stockQuantity} UNITS LEFT` (JetBrains Mono `10px`, `#B45309`, amber dot).
    * `stockQuantity <= 0`: `OUT OF STOCK` (JetBrains Mono `10px`, `#78716C`, stone dot).
  * **Strict YAGNI:** Zero fake ratings, reviews, discounts, or delivery badges.

### 5.5 Pagination Bar
* Displayed when `totalPages > 1`.
* Reuses FE-03 pagination pattern: `[ ← PREVIOUS ]`, current page indicator `[ 01 ]`, `[ NEXT → ]`, and `PAGE {number + 1} OF {totalPages}` caption.

---

## 6. Comprehensive State Specifications

### 6.1 State 1: Loading State
* **Trigger:** API requests in-flight on initial page navigation or category tab switch.
* **Display:**
  * Header and breadcrumbs rendered.
  * Category title placeholder: `h-9 w-64 bg-stone-200 animate-pulse rounded-[3px]`.
  * Category description placeholder: `h-5 w-96 bg-stone-100 animate-pulse rounded-[3px] mt-2`.
  * Product grid: 4-card skeleton with `#F5F5F4` pulsing image placeholders and muted typography bars.

### 6.2 State 2: Success State (Populated Category)
* **Trigger:** Both requests return HTTP 200 and `totalElements > 0`.
* **Display:** Full editorial header, category rail, catalog toolbar, and 4-column product grid as rendered on canonical Stitch screen `cdfa9eee3f92454bb9474c6fbba76e4f`.

### 6.3 State 3: Empty Category State (`NO PRODUCTS`)
* **Trigger:** `GET /api/v1/categories/{id}` returns HTTP 200, but `GET /api/v1/products/category/{categoryId}` returns `empty: true` (`totalElements === 0`).
* **Display:**
  * Category title and description render normally in header.
  * Centered feedback container on `#FFFFFF`, `1px solid #E7E5E4`, `p-16 text-center max-w-xl mx-auto my-12 rounded-[4px]`:
    * Eyebrow: `NO PRODUCTS AVAILABLE` (JetBrains Mono `11px`, `#A8A29E`, uppercase tracking `+0.08em`).
    * Heading: `"No products in this category yet."` (Plus Jakarta Sans `16px`, `#1C1917`).
    * Helper text: `"We are currently restocking equipment for this category. Explore other categories or visit the shop."` (`#78716C`, `14px`).
    * Primary Action: `[ BACK TO SHOP ]` button (`bg-[#1C1917] text-white px-6 py-3 text-[12px] font-semibold tracking-wider rounded-[3px] uppercase hover:bg-stone-800 transition-colors`, `href="/shop"`).

### 6.4 State 4: Category Not Found State (HTTP 404)
* **Trigger:** `GET /api/v1/categories/{id}` returns HTTP 404 (`CategoryNotFoundException`).
* **Display:**
  * Centered error container on `#FFFFFF`, `1px solid #E7E5E4`, `p-16 text-center max-w-xl mx-auto my-16 rounded-[4px]`:
    * Eyebrow: `404 // NOT FOUND` (JetBrains Mono `11px`, `#DC2626`, uppercase tracking `+0.08em`).
    * Heading: `CATEGORY NOT FOUND` (Plus Jakarta Sans `22px`, font-bold, `#1C1917`).
    * Description: `"The requested category ID does not exist or has been retired from the catalog."` (`#78716C`, `14px`).
    * Primary Action: `[ BACK TO SHOP ]` button (`href="/shop"`).

### 6.5 State 5: API Error State
* **Trigger:** Network failure, gateway timeout (504), or internal error (500).
* **Display:**
  * Error container with left accent border (`border-l-4 border-l-[#DC2626]`):
    * Heading: `UNABLE TO LOAD CATEGORY` (Plus Jakarta Sans `18px`, font-bold, `#1C1917`).
    * Description: `"A network error prevented loading this category. Please verify your connection."` (`#78716C`, `14px`).
    * Action: `[ RETRY ]` button that re-triggers the dual API fetch.

---

## 7. Responsive Layout & Mobile Behavior

* **Desktop (`>= 1024px`):**
  * 4-column product grid (`lg:grid-cols-4 gap-6`).
  * Horizontal category tab rail with full category list.
  * Inline catalog toolbar with left tally and right sort selector.
* **Tablet (`768px – 1023px`):**
  * 2-column product grid (`sm:grid-cols-2 gap-5`).
  * Category rail horizontally scrollable with scroll hints.
* **Mobile (`< 768px`):**
  * Horizontally swipeable category rail (`overflow-x-auto no-scrollbar py-2`).
  * Catalog toolbar stacks: tally top, sort selector bottom full-width.
  * 2-column product grid (`grid-cols-2 gap-3`) or single-column with clean aspect ratios.
  * All interactive touch targets (category tabs, sort selector, pagination buttons) meet `>= 44px` height requirement.
  * Zero horizontal page overflow (`overflow-x-hidden`).

---

## 8. Stitch Canonical Artifact Specification

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `cdfa9eee3f92454bb9474c6fbba76e4f`
* **Screen Title:** `TECHGEAR — Keyboards & Mouse Category`
* **Design Token Reference:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
* **Visual Audit Confirmation:**
  * Canvas: Warm alabaster `#FAFAF9`.
  * Surface: Pure spec white `#FFFFFF` with hairline `1px solid #E7E5E4` borders.
  * Typography: Plus Jakarta Sans for editorial text, JetBrains Mono for SKU and price.
  * Pricing: Strict Vietnamese Dong presentation (`4.990.000 ₫`, `2.100.000 ₫`, `2.990.000 ₫`, `2.490.000 ₫`) using `JetBrains Mono` with dot thousand separators and `₫` suffix.
  * Header Polish: Clean rectangular search control (`rounded-[4px]`) without `⌘K` shortcut badge or pill distortion.
  * Category Header Polish: Removed pseudo-technical eyebrow (`CATEGORY // 02`). Header leads directly with category title and description.
  * Commerce Breadcrumbs: Standard navigation (`SHOP / CATEGORIES / KEYBOARDS & MOUSE`) without telemetry terminology.
  * Category Rail Verification: Bound from `GET /api/v1/categories` (filtered active categories) or shared layout cache.
  * Mock Data Isolation: 4 sample cards and `"SHOWING 4 ITEMS"` are design mock presentations; runtime binds dynamically to `Page<ProductDto>`.
  * Compliance: Zero filter sidebars, zero fake ratings, zero telemetry widgets.

---

## 9. Verification Checklist

- [x] Category endpoint verified (`GET /api/v1/categories/{id}`)
- [x] Category DTO verified (`CategoryDto`: `id`, `name`, `description`, `imageUrl`, `active`, `createdAt`)
- [x] Product-by-category endpoint verified (`GET /api/v1/products/category/{categoryId}`)
- [x] Query parameters verified (`page`, `size`, `sort`)
- [x] Pagination verified (`Page<ProductDto>`)
- [x] Sorting verified (Spring Data `sort` parameters)
- [x] Authentication verified (Public browse, no JWT required)
- [x] 404 verified (`CategoryNotFoundException` &rarr; HTTP 404)
- [x] Empty state verified (`empty: true` &rarr; `NO PRODUCTS AVAILABLE`)
- [x] Product card reused from FE-03
- [x] VND format consistent (`4.990.000 ₫`)
- [x] No fake metadata
- [x] No fake product count
- [x] No fake marketing claims
- [x] No duplicate Shop/filter system
- [x] No scope creep
- [x] Exactly 1 canonical Stitch screen created (`cdfa9eee3f92454bb9474c6fbba76e4f`)
- [x] Documentation created (`FE-11-CATEGORY-DESIGN.md`)
- [x] FE-01 through FE-10 completely preserved without modification

---

**FE-11 STATUS: READY FOR REVIEW**
