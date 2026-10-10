# TECHGEAR — Shop / Product Listing Design Specification

> **Document ID:** FE-03-SHOP-PRODUCT-LISTING-DESIGN  
> **Route:** `/shop` (or `/products`)  
> **Version:** 1.0.0  
> **Screen:** Customer Shop / Catalog Listing  
> **Visual Reference:** Home Screen in Stitch Project (`projectId: 113306237264437644`)  
> **Backend Verification:** Spring Boot 3.2 `product-service` Controller & Repository Verified  
> **Status:** Design Specification & Stitch Blueprint

---

## 1. Design Read

> **Design Read:** High-density architectural catalog storefront for hardware enthusiasts, engineers, and creators. Inherits 100% of the Swiss minimalist and editorial tech commerce identity established on the Home screen, shifting from narrative vitrine to structured, high-efficiency product discovery with zero marketing clutter.

---

## 2. Layout Structure & Blueprint

The Shop page is strictly organized into **7 structural sections** bounded within a `1280px` (`max-w-7xl`) container, centered with auto margins on `#FAFAF9` (warm alabaster canvas):

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared 64px Sticky Nav — "Shop" Link Active)                │
├────────────────────────────────────────────────────────────────────────┤
│ 2. SHOP INTRO (Editorial Headline, Understated Collection Scope)       │
│    "Hardware worth keeping." // Curated 24-item equipment catalog      │
├────────────────────────────────────────────────────────────────────────┤
│ 3. CATEGORY NAVIGATION (Understated Horizontal Segmented Rail)         │
│    [ All (24) ]  Audio (4)  Keyboards & Mouse (4)  Gaming (4) ...     │
├────────────────────────────────────────────────────────────────────────┤
│ 4. CATALOG TOOLBAR (Catalog Utility Bar — Backed by Actual APIs)       │
│    Left: Showing 1–20 of 24 items    │  Right: Sort: [ Newest ▼ ]      │
├────────────────────────────────────────────────────────────────────────┤
│ 5. PRODUCT GRID (Disciplined 4-Column Architectural Grid)              │
│    [Card 01]       [Card 02]       [Card 03]       [Card 04]           │
│    [Card 05]       [Card 06]       [Card 07]       [Card 08]           │
│    ...                                                                 │
├────────────────────────────────────────────────────────────────────────┤
│ 6. PAGINATION (Minimalist Bar Mapped to Spring Boot Pageable)          │
│    [ ← Previous ]   [ 01 ]   02   [ Next → ]   //  Page 01 of 02       │
├────────────────────────────────────────────────────────────────────────┤
│ 7. FOOTER (Shared Customer-Facing Utility Blueprint)                   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Section-by-Section Visual Specification

### 3.1 Header (Shared Component)
*   **Dimensions:** Exact `64px` height, `sticky top-0 z-50`.
*   **Surface:** `#FAFAF9` with `backdrop-filter: blur(8px)` at 92% opacity, hairline bottom border `1px solid #E7E5E4`.
*   **State:** The `Shop` link is explicitly set to active: `text-[#1C1917] font-medium border-b-2 border-[#1C1917] pb-1`.
*   **Actions:** Search trigger with `⌘K` shortcut badge, Account profile link, Cart trigger with `[0]` count badge.

### 3.2 Shop Intro / Page Heading
*   **Purpose:** Immediate catalog context without a bloated promotional hero.
*   **Top/Bottom Spacing:** `pt-10 pb-8 border-b border-[#E7E5E4]`.
*   **Hierarchy:**
    *   *Eyebrow:* `CATALOG ARCHIVE // 2026 INDEX` (JetBrains Mono `11px`, `#78716C`, uppercase, tracking `+0.08em`).
    *   *Title:* `Hardware worth keeping.` (Plus Jakarta Sans `36px`, Weight 700, tracking `-0.03em`, `#1C1917`).
    *   *Descriptor:* "Precision tools, audio reference monitors, mechanical keyboards, and daily desk essentials engineered for durability and focus." (Plus Jakarta Sans `15px`, `#57534E`, max width `56ch`). *(No unverified certification or mechanical testing claims).*

### 3.3 Category Navigation (Understated Segmented Rail)
*   **Layout:** Horizontal scrolling/wrapping rectangular tab list with `gap-2 pt-6 pb-6 border-b border-[#E7E5E4]`.
*   **Zero Pill Bubbles:** Uses crisp rectangular tabs with strict `4px` corner radius, hairline border `1px solid #E7E5E4`. No rounded-full pill elements.
*   **6 Official Categories + All:**

| Index | Label | Target Route / Query | Count |
| :---: | :--- | :--- | :---: |
| **ALL**| **All Equipment** (Default Active) | `/shop` | `(24)` |
| **01** | **Audio** | `/shop?category=1` (`/category/1`) | `(4)` |
| **02** | **Keyboards & Mouse** | `/shop?category=2` (`/category/2`) | `(4)` |
| **03** | **Gaming** | `/shop?category=3` (`/category/3`) | `(4)` |
| **04** | **Accessories** | `/shop?category=4` (`/category/4`) | `(4)` |
| **05** | **Monitors** | `/shop?category=5` (`/category/5`) | `(4)` |
| **06** | **Laptop Accessories** | `/shop?category=6` (`/category/6`) | `(4)` |

*   **Active Tab Styling:** Background `#1C1917`, text `#FFFFFF`, border `#1C1917`, `rounded-[4px]`.
*   **Inactive Tab Styling:** Background `#FFFFFF`, text `#57534E`, border `#E7E5E4`, `rounded-[4px]`, hover: text `#1C1917`, hover: border `#A8A29E`.

### 3.4 Catalog Toolbar
*   **Spacing:** `py-4 flex items-center justify-between border-b border-[#E7E5E4] mb-8`.
*   **Left (Product Counter):**
    *   `SHOWING 1–20 OF 24 ITEMS` (JetBrains Mono `11px`, Medium, `#78716C`, uppercase tracking `+0.05em`).
*   **Right (Sort Selector):**
    *   Label: `SORT BY:` in JetBrains Mono `11px` (`#78716C`).
    *   Native styled selector (`h-9 px-3 bg-white border border-[#E7E5E4] rounded-[4px] text-[13px] text-[#1C1917] focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706]`):
        1.  `Newest Arrivals` (`sort=createdAt,desc` — Default backend sort)
        2.  `Price: Low to High` (`sort=price,asc`)
        3.  `Price: High to Low` (`sort=price,desc`)
        4.  `Alphabetical: A–Z` (`sort=name,asc`)

### 3.5 Product Grid & Card Anatomy
*   **Grid Specs:** `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6`.
*   **Card Anatomy (Strictly Reused from Home):**
    1.  **Image Pad:** Aspect ratio `1:1`, unbleached neutral background `#F5F5F4`, border-bottom `1px solid #E7E5E4`, padding `20px`. Centered high-resolution studio hardware image.
    2.  **Category Tag:** JetBrains Mono `10px`, uppercase, `#78716C`, tracking `+0.05em`.
    3.  **Product Title:** Plus Jakarta Sans `15px`, SemiBold (600), `#1C1917`, line-clamp `1`.
    4.  **Metadata / SKU:** JetBrains Mono `10px`, `#A8A29E`, uppercase (e.g., `SKU: TG-AUD-001`).
    5.  **Price:** JetBrains Mono `16px`, SemiBold (600), `#1C1917`, `tabular-nums` (e.g., `7.990.000 ₫`).
    6.  **Stock Dot:** JetBrains Mono `11px`, emerald dot `● In Stock (35)` (`#15803D`).
    7.  **Interaction:** Click anywhere on card navigates to `/products/{id}`. Hover triggers subtle border shift from `#E7E5E4` to `#A8A29E` and 1.5% hardware image scale.

### 3.6 Pagination Bar
*   **Container:** `py-12 flex flex-col sm:flex-row items-center justify-between border-t border-[#E7E5E4] mt-12 gap-4`.
*   **Left (Page Range):** `PAGE 01 OF 02 // 20 PER PAGE` in JetBrains Mono `11px` (`#78716C`).
*   **Center / Right (Pagination Controls):**
    *   `← Previous` button: Disabled state (`text-[#A8A29E] bg-[#F5F5F4] border border-[#E7E5E4] rounded-[4px] px-3 py-1.5 text-xs font-mono cursor-not-allowed`).
    *   Page Numbers:
        *   `[ 01 ]` (Active: `bg-[#1C1917] text-white border border-[#1C1917] rounded-[4px] px-3 py-1.5 text-xs font-mono`).
        *   `02` (Inactive: `bg-white text-[#1C1917] border border-[#E7E5E4] rounded-[4px] px-3 py-1.5 text-xs font-mono hover:border-[#1C1917]`).
    *   `Next →` button: Active state (`bg-white text-[#1C1917] border border-[#E7E5E4] rounded-[4px] px-3 py-1.5 text-xs font-mono hover:border-[#1C1917]`).

### 3.7 Footer (Shared Component)
*   **Customer-Facing Clean Utility Blueprint:** Strictly contains customer-facing storefront navigation and portal information without exposing internal implementation details, Spring Boot version, microservices, observability stamps, API versioning, or operational status telemetry (no "0ms latency" or "SYSTEM OPERATIONAL").
    1.  **Brand Column:** `TECHGEAR` logo and tagline ("Curated tech gear and workspace equipment.").
    2.  **Categories Column:** Navigation links to all 6 categories (Audio, Keyboards & Mouse, Gaming, Accessories, Monitors, Laptop Accessories).
    3.  **Customer Portal Column:** Direct links to supported sitemap flows: Order History (`/orders`), Account Profile (`/profile`).
    4.  **Bottom Rule:** Clean copyright notice: `© 2026 TECHGEAR. All rights reserved.`

---

## 4. Loading, Empty, and Error States

### 4.1 Loading State (Skeleton Grid)
*   Maintains layout geometry during `GET /api/v1/products` fetch.
*   Renders 8 skeleton cards with `#F5F5F4` pulsed background (`animate-pulse`), matching the exact 1:1 image pad and 3 text placeholder bars.
*   Zero circular spinning spinners.

### 4.2 Empty State (No Products Found)
*   Triggered when `Page<ProductDto>.empty == true` (e.g. search keyword has no match or category returns 0 items).
*   Container: `py-20 text-center border border-[#E7E5E4] rounded-[6px] bg-white my-8`.
*   Copy:
    *   *Mono Tag:* `NO MATCHES`
    *   *Headline:* "No equipment matching your criteria."
    *   *Description:* "The selected category or search filter returned no products." (Does not imply an HTTP 404, as the API returns an empty successful 200 OK page).
    *   *Action:* Solid obsidian button "Reset to Full Catalog" (`href="/shop"`).

### 4.3 Error State (Service Unavailable)
*   Triggered on network failure or 500 error from `product-service`.
*   Container: `p-8 border border-[#FECACA] bg-[#FEF2F2] rounded-[6px] my-8 flex items-center justify-between`.
*   Text: "Unable to retrieve the equipment catalog." (States strictly what the frontend knows without assuming backend internal state).
*   Action: Outlined retry button "Retry".

---

## 5. Backend API Mapping & Verification

All UI components map 1-to-1 with actual endpoints in `product-service`:

| UI Action / Component | Backend Endpoint | HTTP Method | Query Parameters & Constraints |
| :--- | :--- | :---: | :--- |
| **Initial Catalog Load** | `/api/v1/products` | `GET` | `page=0`, `size=20`, `sort=createdAt,desc` |
| **Category Filter** | `/api/v1/products/category/{categoryId}` | `GET` | `categoryId=1..6`, `page=0`, `size=20`, `sort=...` |
| **Sorting Selection** | `/api/v1/products` | `GET` | `sort=price,asc` / `sort=price,desc` / `sort=name,asc` |
| **Pagination Next/Prev**| `/api/v1/products` | `GET` | `page=1`, `size=20` |
| **Keyword Search** | `/api/v1/products/search` | `GET` | `keyword=...`, `page=0`, `size=20` (PostgreSQL tsvector FTS) |
| **Category List** | `/api/v1/categories` | `GET` | Fetches active categories dynamically for the navigation rail |

### Explicit Discrepancy & Non-Supported Controls:
*   **Price Range Slider:** Backend has no `minPrice` or `maxPrice` query parameters in `ProductController`. Deliberately excluded.
*   **Brand Filter:** Entity `Product` does not have a `brand` column. Deliberately excluded.
*   **Rating / Reviews Filter:** No review service or rating column exists in backend. Deliberately excluded.
*   **Multi-Category Selection:** Backend endpoint only accepts a single `categoryId` path parameter. Deliberately restricted to single category selection.

---

## 6. Responsive Adaptations

*   **Desktop (≥ 1024px):** 4-column product grid, full horizontal category rail, toolbar with left product counter and right sort.
*   **Tablet (768px – 1023px):** 2-column product grid, horizontal category rail with natural touch-scroll, toolbar elements stacked or flex-spaced.
*   **Mobile (< 768px):** 1-column (or compact 2-column) grid, category rail using horizontally scrollable rectangular category tabs (`overflow-x-auto`) preserving the strict 4px radius and Swiss/Editorial visual language (no rounded-full pill-shaped UI), pagination collapses to centered `Prev / Page / Next`.

---

## 7. Stitch Visual Reference

*   **Project ID:** `113306237264437644` (TECHGEAR)
*   **Screen ID:** `232e75ea0a9c46c6837974031e1df17b`
*   **Screen Name:** `projects/113306237264437644/screens/232e75ea0a9c46c6837974031e1df17b`
*   **Screen Title:** `TECHGEAR — Customer Shop`
*   **Device Type:** `DESKTOP` (2560 × 4270)
*   **Design System:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
*   **Screenshot Artifact:** [View Screenshot](https://lh3.googleusercontent.com/aida/AEtjO1U-BBitSlWtOSdLcReMVLAYZYz4Kp4PgIOhMUrbGDTPj_WQkZK8DMFqAB14mM3mAOHSGzMAEbP_FHvUfJw5x9I7jwud4L4RSxao6Tin1Askg7JmNrDiIbnW_HRl3jizkSgtdHQ3i-vXVP7bmbben20fvqj1dilAsMDckE6n_dEp4HN4Jo2j0lAR2tOdhsuLhpDCxuC2uupAiZDOQengKYDQOcRBqbjf4_LXzpIl1wqfbgADzL3VMRUz)
*   **Status:** `COMPLETE` / `READY FOR REVIEW`

