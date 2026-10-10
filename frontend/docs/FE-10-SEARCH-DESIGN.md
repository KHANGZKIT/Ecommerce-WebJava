# TECHGEAR — FE-10 Search Results Design Specification

> **Document ID:** FE-10-SEARCH-DESIGN  
> **Route:** `/search?q=...`  
> **Target Audience:** Public / All Customers  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `56d21e2dc08f43c6bc763510b86af61f`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Search Results** screen (`/search?q=...`) provides customers with a focused, high-clarity view of products matching their text search query. It connects directly to the backend PostgreSQL full-text search capability in `product-service` without speculative layers, unnecessary filters, or non-existent telemetry.

### Strict YAGNI Boundaries
* **No Speculative Filters:** The backend search endpoint (`GET /api/v1/products/search`) accepts only `keyword` and pagination. It does **not** support category, price, or brand filtering. Consequently, **no filter sidebar or filter chips** are introduced.
* **No Marketing or Fake Badges:** Product cards display verified data only: name, category, SKU, price, and stock status. No ratings, reviews, discounts, fake stock scarcity warnings, or delivery estimates.
* **No Unconfirmed Auxiliary Features:** No autocomplete API, trending searches, search history persistence, recommendations, or AI/semantic search.
* **Strict Scope Isolation:** No modifications to existing screens FE-01 through FE-09; no backend or database alterations.

---

## 2. Route & Navigation Architecture

* **Canonical Route:** `/search?q={query}`
  * Example: `/search?q=keyboard`
* **Query Parameter Mapping:**
  * Client URL uses standard web convention `q` (e.g. `?q=keyboard`).
  * Client service maps `q` directly to backend query parameter `keyword` (e.g. `GET /api/v1/products/search?keyword=keyboard&page=0&size=20`).
* **Header Integration:**
  * Header search field is pre-populated with the current decoded query value.
  * Pressing `Enter` or submitting the header search form navigates to `/search?q={encodeURIComponent(keyword)}`.
* **Empty Query Handling:**
  * If customer enters an empty or whitespace query (`q=""` or `q="   "`), frontend does **not** invoke the backend search endpoint.
  * Instead, the client redirects to `/shop` or displays an initial search invitation state prompting the user to enter a search term.

---

## 3. Backend Verification & Integration Contract

### 3.1 Verified Backend Source Analysis

| Component | Actual Code Reference | Implementation Detail |
| :--- | :--- | :--- |
| **Controller** | `ProductController.java:37-42` | `@GetMapping("/search") public ResponseEntity<Page<ProductDto>> search(@RequestParam String keyword, @PageableDefault(size = 20) Pageable pageable)` |
| **Service** | `ProductService.java:81-84` | Calls `productRepository.searchByKeyword(keyword, pageable).map(this::toDto)` (not cached due to broad keyword cardinality) |
| **Repository** | `ProductRepository.java:49-62` | Native PostgreSQL FTS: `to_tsvector('english', coalesce(p.name,'') \|\| ' ' \|\| coalesce(p.description,'')) @@ plainto_tsquery('english', :keyword)` on `active = true` products |
| **Gateway** | `application.yml:55-66` | Route `product-service` forwards `/api/v1/products/search/**` with public rate limiter |

### 3.2 Endpoint Specification
* **Path:** `GET /api/v1/products/search`
* **Query Parameters:**
  * `keyword` *(required, String)*: Search term or phrase parsed by PostgreSQL `plainto_tsquery`.
  * `page` *(optional, Integer)*: 0-indexed page number (default `0`).
  * `size` *(optional, Integer)*: Page size (default `20` via `@PageableDefault(size = 20)`).
  * `sort` *(optional, String)*: Standard Spring Data sort syntax (`property,direction`).
* **Authentication Requirement:**
  * Public browse access. Does not require customer login or JWT token. If present, client sends bearer header for rate-limiting context.

### 3.3 Response Data Contract (`Page<ProductDto>`)

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
      "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80",
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
    "sort": { "sorted": false, "unsorted": true, "empty": true }
  },
  "totalElements": 3,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 3,
  "empty": false
}
```

> **Design Mock vs. Runtime Binding:**
> The `totalElements: 3` and sample keyboard entities above are representative mock data used to illustrate the contract. At runtime, the client dynamically binds `Page<ProductDto>.content` and extracts `totalElements` directly from the live API response. Zero values are hardcoded.

---

## 4. Architectural Verification: Grounded vs. Design Decisions

| Aspect | Classification | Grounding & Rationale |
| :--- | :--- | :--- |
| **Endpoint URL** | `BACKEND-CONFIRMED` | Exact path `/api/v1/products/search` in `ProductController.java`. |
| **Param `keyword`** | `BACKEND-CONFIRMED` | Controller parameter `@RequestParam String keyword`. |
| **Full-Text Matching** | `BACKEND-CONFIRMED` | Repository matches against product `name` and `description` via PostgreSQL `plainto_tsquery`. |
| **Pagination Model** | `BACKEND-CONFIRMED` | Returns Spring Data `Page<ProductDto>` (`totalElements`, `totalPages`, `number`, `size`). |
| **Product Fields** | `BACKEND-CONFIRMED` | `id`, `name`, `sku`, `price`, `stockQuantity`, `imageUrl`, `category.name`. |
| **Absence of Filters** | `BACKEND-CONFIRMED` | Search endpoint does not accept categoryId, priceMin, or inStock parameters. |
| **Route `/search?q=`** | `UI DESIGN DECISION` | Standard web URL routing convention mapping user query `q` to API `keyword`. |
| **Search Input Bar** | `UI DESIGN DECISION` | Dedicated, clean search bar on page allowing customers to adjust or clear their query. |
| **Product Card Anatomy** | `UI DESIGN DECISION` | Direct reuse of FE-03 Shop card layout for visual and behavioral consistency. |
| **No Matches State** | `UI DESIGN DECISION` | Clean industrial empty state directing user back to `/shop`. |

---

## 5. Screen Layout & Visual Hierarchy

The layout maintains the **Obsidian & Paper** design aesthetic with a strict maximum width of `1280px` (`max-w-7xl`), centered margins, and `#FAFAF9` canvas background.

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared 64px Sticky Nav — TECHGEAR Logo, Cart [0], Account) │
├────────────────────────────────────────────────────────────────────────┤
│ 2. SEARCH BAR & BREADCRUMBS                                            │
│    SHOP / SEARCH                                                       │
│    [ Search products, hardware, audio...          [ ↵ SEARCH ] ]       │
├────────────────────────────────────────────────────────────────────────┤
│ 3. SEARCH RESULTS HEADER                                               │
│    SEARCH RESULTS FOR "keyboard"                                       │
│    SHOWING 3 MATCHES                                                   │
├────────────────────────────────────────────────────────────────────────┤
│ 4. PRODUCT GRID (4-Column Precision Hardware Grid)                     │
│    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                  │
│    │ Keychron Q1  │ │ Keychron V1  │ │ NuPhy Air75  │                  │
│    │ 4.990.000 ₫  │ │ 2.100.000 ₫  │ │ 2.990.000 ₫  │                  │
│    │ IN STOCK     │ │ IN STOCK     │ │ 3 UNITS LEFT │                  │
│    └──────────────┘ └──────────────┘ └──────────────┘                  │
├────────────────────────────────────────────────────────────────────────┤
│ 5. PAGINATION (Minimalist Bar — Enabled when totalPages > 1)           │
│    [ PREVIOUS ]   [ 01 ]   [ NEXT ]   // PAGE 1 OF 1                   │
├────────────────────────────────────────────────────────────────────────┤
│ 6. FOOTER (Shared Customer Utility Blueprint)                          │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Breadcrumbs & Context
* **Trail:** `SHOP / SEARCH`
* **Styling:** JetBrains Mono `11px`, `#78716C`, uppercase, tracking `+0.05em`. Clickable `SHOP` links to `/shop`.

### 5.2 Architectural Search Input
* **Container:** High-density input field with `h-12`, `#FFFFFF` surface, `1px solid #E7E5E4` border, and `4px` corner radius.
* **Input Text:** Plus Jakarta Sans `14px`, `#1C1917` text, `#A8A29E` placeholder (`"Search equipment by name or description..."`).
* **Action Button:** `#1C1917` solid button with `#FFFFFF` text, `px-5 text-[11px] font-semibold tracking-[0.06em] uppercase rounded-[3px]` reading `SEARCH`.

### 5.3 Results Header & Metadata
* **Heading:** Plus Jakarta Sans `28px`, bold (700), `-0.02em` tracking, color `#1C1917`.
  * Format: `SEARCH RESULTS FOR "<keyword>"` (restrained quotation typography).
* **Metadata Counter:** JetBrains Mono `11px`, `#78716C`, uppercase, tracking `+0.06em`.
  * Format: `SHOWING {totalElements} MATCHES` (e.g. `SHOWING 3 MATCHES` in design mockup).
  * **Strict Data Binding Rule:** The caption `"SHOWING 3 MATCHES"` is strictly **DESIGN MOCK DATA** for the canonical screen preview.
  * **Runtime Implementation:** The count is **never hardcoded**. Runtime dynamically reads `totalElements` (or `numberOfElements`) from the incoming `Page<ProductDto>` response.

### 5.4 Product Grid & Card Anatomy (Reused from FE-03)
* **Grid:** `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6`.
* **Dynamic Binding vs. Mock Data:**
  * Sample product names (*Keychron Q1 Pro*, *Keychron V1*, *NuPhy Air75*) and sample prices exist solely for visual mock representation in Stitch.
  * At runtime, product cards are **dynamically mapped** from `Page<ProductDto>.content`.
* **Card Specifications:**
  * **Canvas:** `#FFFFFF` background, `1px solid #E7E5E4` border, `4px` corner radius.
  * **Product Image Pad:** Aspect ratio `1:1`, `#F5F5F4` background, `1px solid #E7E5E4` bottom hairline border, centered object-contain image.
  * **Category Tag:** JetBrains Mono `10px`, uppercase, `#78716C`, tracking `+0.05em`.
  * **Product Name:** Plus Jakarta Sans `14px`, font-semibold, `#1C1917`, line-clamp-2. Clickable link to `/product/{id}`.
  * **SKU:** JetBrains Mono `11px`, `#A8A29E`.
  * **Price (VND Standard):** JetBrains Mono `15px`, font-semibold, `#1C1917`, `tabular-nums`.
    * Standardized TECHGEAR currency representation: Vietnamese Dong formatted with dot thousand separators and the `₫` symbol (e.g. `4.990.000 ₫`, `2.100.000 ₫`, `2.990.000 ₫`, `12.480.000 ₫`).
    * Runtime dynamically formats `ProductDto.price` (`BigDecimal`) into VND locale convention.
  * **Stock Availability:**
    * `stockQuantity > 5`: `IN STOCK` (JetBrains Mono `10px`, `#15803D`, emerald dot).
    * `1 <= stockQuantity <= 5`: `{stockQuantity} UNITS LEFT` (JetBrains Mono `10px`, `#B45309`, amber dot).
    * `stockQuantity <= 0`: `OUT OF STOCK` (JetBrains Mono `10px`, `#78716C`, stone dot).

### 5.5 Pagination Bar
* Displayed when `totalPages > 1`.
* Reuses FE-03 pagination pattern: `[ PREVIOUS ]`, current page indicator `[ 01 ]`, `[ NEXT ]`, and `PAGE {number + 1} OF {totalPages}` caption.

---

## 6. Comprehensive State Specifications

### 6.1 State 1: Initial / Empty Query
* **Trigger:** Customer visits `/search` without `q` parameter or with blank whitespace.
* **Behavior:** No search API request is dispatched.
* **Display:** Centered prompt with search icon:
  * Heading: `ENTER A SEARCH TERM`
  * Description: `"Type product keywords above or explore our curated equipment catalog."`
  * Action: `[ EXPLORE SHOP ]` (`href="/shop"`).

### 6.2 State 2: Loading State
* **Trigger:** Search API request in-flight.
* **Display:**
  * Header retains search query.
  * Counter displays: `SEARCHING CATALOG...`
  * 4-card product skeleton grid with `#F5F5F4` pulsing image placeholders and muted bone typography bars (`animate-pulse`).

### 6.3 State 3: Success with Results
* **Trigger:** `GET /api/v1/products/search?keyword=...` returns HTTP 200 with `totalElements > 0`.
* **Display:** Full results header, match count, and populated 4-column product grid as rendered in canonical Stitch screen `56d21e2dc08f43c6bc763510b86af61f`.

### 6.4 State 4: No Results (`NO MATCHES`)
* **Trigger:** Backend returns HTTP 200 with `empty: true` (`totalElements === 0`).
* **Display:**
  * Container: Centered panel on `#FFFFFF`, `1px solid #E7E5E4`, `p-16 text-center max-w-xl mx-auto my-12 rounded-[4px]`.
  * Eyebrow: `NO MATCHES` (JetBrains Mono `11px`, `#A8A29E`, uppercase tracking `+0.08em`).
  * Message: `"No products matched your search for \"{keyword}\"."` (Plus Jakarta Sans `16px`, `#1C1917`).
  * Helper: `"Check spelling, try broader keywords, or browse all equipment categories."` (`#78716C`, `14px`).
  * Primary Action: `[ BACK TO SHOP ]` button (`bg-[#1C1917] text-white px-6 py-3 text-[12px] font-semibold tracking-wider rounded-[3px] uppercase hover:bg-stone-800 transition-colors`).

### 6.5 State 5: API Error
* **Trigger:** Backend service returns HTTP 500, gateway timeout (504), or rate-limit rejection (429).
* **Display:**
  * Container: Centered error panel on `#FFFFFF`, `1px solid #E7E5E4`, `border-l-4 border-l-[#DC2626]`.
  * Heading: `UNABLE TO RETRIEVE SEARCH RESULTS`
  * Description: `"A network or service disruption prevented loading results for \"{keyword}\"."`
  * Action: `[ RETRY SEARCH ]` button that re-executes the API call.

---

## 7. Responsive Layout & Mobile Behavior

* **Desktop (`>= 1024px`):**
  * 4-column product grid (`lg:grid-cols-4 gap-6`).
  * Horizontal search toolbar with integrated action button.
  * Full breadcrumbs and match metadata aligned left and right.
* **Tablet (`768px – 1023px`):**
  * 2-column product grid (`sm:grid-cols-2 gap-5`).
  * Search bar spans container width.
* **Mobile (`< 768px`):**
  * Search input and submit button stacked vertically or inline with full width.
  * 2-column or single-column responsive product card presentation with strict `touch-action` optimization.
  * All interactive touch targets (search button, pagination buttons, card links) meet `>= 44px` height requirement.
  * Zero horizontal scroll or container overflowing (`overflow-x-hidden`).

---

## 8. Stitch Artifact Specification

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `56d21e2dc08f43c6bc763510b86af61f`
* **Screen Title:** `TECHGEAR — Search Results`
* **Design Token Reference:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
* **Visual Audit Confirmation:**
  * Canvas: Warm alabaster `#FAFAF9`.
  * Typography: Plus Jakarta Sans for editorial text, JetBrains Mono for SKU, price, and counters.
  * Pricing: Standardized Vietnamese Dong presentation (`4.990.000 ₫`, `2.100.000 ₫`, `2.990.000 ₫`) using `JetBrains Mono` with dot thousand separators and `₫` suffix, consistent with FE-01 → FE-03.
  * Mock Data Isolation: The 3 sample keyboard cards and `"SHOWING 3 MATCHES"` indicator serve strictly as visual design mock data. Runtime binds dynamically to `Page<ProductDto>`.
  * Components: Shared header, breadcrumbs, search input bar, results metadata, 4-column product grid, pagination footer, customer footer.
  * Compliance: Zero fake filters, zero fake ratings, zero telemetry widgets.

---

## 9. Verification Checklist

- [x] Search endpoint verified (`GET /api/v1/products/search`)
- [x] Query parameter verified (`keyword` mapped from URL `q`)
- [x] Response format verified (`Page<ProductDto>`)
- [x] Pagination verified (`Pageable` parameters: `page`, `size`)
- [x] Sort/filter evaluated: No unsupported filters created
- [x] Strict YAGNI observed: Zero fake reviews, badges, autocomplete, or AI features
- [x] Reused FE-03 product card anatomy and typography
- [x] Obsidian & Paper design system faithfully applied
- [x] Exactly 1 canonical Stitch screen created (`56d21e2dc08f43c6bc763510b86af61f`)
- [x] Comprehensive documentation created (`FE-10-SEARCH-DESIGN.md`)
- [x] FE-01 through FE-09 completely preserved without modification

---

**FE-10 STATUS: READY FOR REVIEW**
