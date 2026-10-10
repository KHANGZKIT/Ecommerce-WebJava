# TECHGEAR — Product Detail Design Specification

> **Document ID:** FE-04-PRODUCT-DETAIL-DESIGN  
> **Route:** `/products/:id`  
> **Version:** 1.0.0  
> **Screen:** Customer Product Detail Page (PDP)  
> **Visual Reference:** Stitch Project (`projectId: 113306237264437644`)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Backend Verification:** `product-service` (`GET /api/v1/products/{id}`) & `order-service` (`POST /api/v1/cart/items`) Verified  
> **Status:** READY FOR REVIEW

---

## 1. Design Read & User Goal

*   **Design Read:** Architectural vitrine and precision product dossier. The Product Detail Page (PDP) serves as the primary conversion point for customers transitioning from the catalog (`/shop`). Rooted in Swiss minimalism and industrial design philosophy, it treats physical technology hardware as functional industrial artifacts: ample unhurried whitespace, hairline technical dividers, tabular numeric telemetry, and zero promotional noise.
*   **User Goal:** 
    1. Inspect high-fidelity product imagery and verified catalog specifications.
    2. Confirm real-time stock availability and unit pricing in VND.
    3. Select an authenticated purchase quantity (bounded by inventory).
    4. Commit the item to the persistent cart via `POST /api/v1/cart/items`.

---

## 2. Information Architecture (Strict 10-Section Structure)

The layout strictly adheres to a customer-focused, non-bloated 10-section hierarchy:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared 64px Sticky Nav — Logo, Shop, Cart [0], Account)     │
├────────────────────────────────────────────────────────────────────────┤
│ 2. BREADCRUMB (Understated Navigation Trail)                           │
│    Home  /  Shop  /  Keyboards & Mouse  /  Keychron Q1 Pro             │
├────────────────────────────────────────────────────────────────────────┤
│ 3 & 4. MAIN PRODUCT HERO (55% Visual / 45% Information Split)          │
│ ┌──────────────────────────────┬─────────────────────────────────────┐ │
│ │ 3. PRODUCT VISUAL (55%)      │ 4. PRODUCT INFORMATION (45%)        │ │
│ │  - Single Studio Hardware Bay│  - Category Eyebrow                 │ │
│ │  - Unbleached Canvas #F5F5F4 │  - Product Name (Typography Focal)  │ │
│ │  - 1px Precision Border      │  - Short Catalog Description        │ │
│ │  - Monospace SKU Tag         │  - 5. PRICE (VND Tabular Monospace) │ │
│ │  - High-res hardware shot    │  - 6. STOCK / AVAILABILITY Dot      │ │
│ │                              │  - 7. QUANTITY Stepper [ - 1 + ]    │ │
│ │                              │  - 8. ADD TO CART (Signal Amber CTA)│ │
│ └──────────────────────────────┴─────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│ 9. PRODUCT DESCRIPTION & CATALOG METADATA                              │
│    - Editorial Product Overview (from ProductDto.description)          │
│    - Supported Catalog Details (SKU, Category, Stock from ProductDto)  │
├────────────────────────────────────────────────────────────────────────┤
│ 10. FOOTER (Approved Customer-Facing Footer)                           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Section-by-Section Visual & Functional Specification

### 3.1 Header (Shared Component)
*   **Dimensions:** Exact `64px` height, `sticky top-0 z-50`.
*   **Surface:** `#FAFAF9` with `backdrop-filter: blur(8px)` at 92% opacity, hairline bottom border `1px solid #E7E5E4`.
*   **Navigation:** Brand wordmark `TECHGEAR` with monospace descriptor `STORE`.
*   **Context:** `Shop` link maintained in active contextual parent state (`text-[#1C1917] font-medium`).
*   **Actions:** Search trigger (`⌘K`), User Account link, and Cart trigger displaying real-time quantity badge (e.g. `[0]`).

### 3.2 Breadcrumb Trail
*   **Location:** Directly below header, `py-4 border-b border-[#E7E5E4]`.
*   **Typography:** JetBrains Mono `11px`, uppercase, tracking `+0.04em`.
*   **Hierarchy:** `Home / Shop / [Category Name] / [Product Name]`.
    *   Example: `HOME / SHOP / KEYBOARDS & MOUSE / KEYCHRON Q1 PRO CUSTOM MECHANICAL KEYBOARD`.
*   **Interactivity:** Clickable parent links styled in `#78716C` (hover: `#1C1917`). Current product truncated with `text-[#1C1917] font-medium`.

### 3.3 Product Visual / Gallery Bay (55% Desktop Width)
*   **Layout:** Left column in a 55/45 desktop split layout (`w-full lg:w-[55%]`).
*   **Strict Single-Image Policy:** Backed 1-to-1 by `ProductDto.imageUrl`. Absolutely **NO** fake thumbnail carousels, NO multi-angle thumbnails, and NO fake video players.
*   **Visual Frame:**
    *   Large rectangular hardware vitrine container with `aspect-ratio: 4/3` (or `1/1` adaptive).
    *   Surface: Milled neutral `#F5F5F4` providing warm, tactile separation from the `#FAFAF9` page canvas.
    *   Border: `1px solid #E7E5E4`, radius `6px`.
    *   Image presentation: Studio hardware photography centered with generous padding (`p-10`), crisp neutral background, natural product shadow.
*   **Discreet Technical Index:** Monospace catalog tag in top-right corner: `INDEX // [SKU]` (e.g. `INDEX // TG-KBM-001`) in JetBrains Mono `10px`, `#78716C`.

### 3.4 Product Information (45% Desktop Width)
*   **Layout:** Right column (`w-full lg:w-[45%] lg:pl-12 flex flex-col justify-start`).
*   **Vertical Hierarchy:**
    1.  **Category Eyebrow:** JetBrains Mono `11px`, uppercase, tracking `+0.08em`, `#78716C` (e.g. `02 / KEYBOARDS & MOUSE`).
    2.  **Product Name:** Plus Jakarta Sans `32px` (mobile: `26px`), font weight `700`, line height `1.15`, letter spacing `-0.03em`, `#1C1917`. Serves as the primary typographic focal point. Strictly factual; NO unverified marketing superlatives.
    3.  **SKU Indicator:** JetBrains Mono `12px`, `#78716C`, margin-top `4px` (e.g. `SKU: TG-KBM-001`).
    4.  **Short Summary:** Plus Jakarta Sans `15px`, `#57534E`, leading `relaxed`, margin-top `16px`. Factual product description from `ProductDto.description`.

### 3.5 Price Presentation
*   **Typography:** JetBrains Mono `28px` (desktop), font weight `700`, `#1C1917`, with OpenType tabular numbers (`tnum`) enabled.
*   **Currency Format:** Vietnamese Dong formatted according to local convention (e.g. `4.490.000 ₫`).
*   **No Commercial Distortion:** NO strikethrough original prices, NO fake discount percentages (`-20%`), NO promotional flash-sale tags.

### 3.6 Stock & Availability Indicator
*   **Placement:** Positioned immediately adjacent to the price block.
*   **States & Telemetry:**
    *   **In Stock (`stockQuantity > 5`):** Solid green dot (`#15803D`, 6px) + `In Stock (30 units available)` in JetBrains Mono `12px`, `#57534E`.
    *   **Low Stock (`1 <= stockQuantity <= 5`):** Solid Signal Amber dot (`#D97706`, 6px) + `Low Stock (Only 3 units remaining)` in JetBrains Mono `12px`, `#D97706`.
    *   **Out of Stock (`stockQuantity == 0`):** Solid neutral dot (`#78716C`, 6px) + `Out of Stock` in JetBrains Mono `12px`, `#78716C`.
*   **Anti-Jargon:** Strictly no warehouse IDs, internal server latency, or inventory protocol stamps.

### 3.7 Quantity Stepper UX
*   **Geometry:** Rectangular industrial stepper `[ − ] [  1  ] [ + ]`, height `44px`, corner radius `4px`, border `1px solid #E7E5E4`, background `#FFFFFF`.
*   **Controls:**
    *   Decrement Button: Width `44px`, icon minus (`−`), disabled when `quantity == 1`.
    *   Number Display: Width `56px`, centered JetBrains Mono `14px` bold, `#1C1917`.
    *   Increment Button: Width `44px`, icon plus (`+`), disabled when `quantity >= stockQuantity`.
*   **Accessibility:** Buttons provide explicit `aria-label="Decrease quantity"` and `aria-label="Increase quantity"`.
*   **Constraint:** Strictly bounded between `1` and `stockQuantity` (client validation capped by backend `MAX_QUANTITY = 9999`).

### 3.8 Add to Cart CTA
*   **Visual Dominance:** Primary conversion anchor rendered in **Signal Amber** (`#D97706`), text `#FFFFFF`, height `44px`, corner radius `4px`, font Plus Jakarta Sans `14px` bold (600), uppercase tracking `+0.04em`.
*   **Hover/Focus:** Hover background `#B45309`, focus ring `2px solid #1C1917` with `2px offset`.
*   **Integration with Order API:**
    *   Payload: `{ "productId": 5, "quantity": 1 }` sent to `POST /api/v1/cart/items`.
    *   Loading State: Spinner + text "ADDING TO CART...".
    *   Success State: Momentary checkmark feedback + updates header cart badge count + unobtrusive slide notification.
    *   Error State: Inline toast "Unable to add this item to your cart. Please try again." + `Retry` action.
*   **Out of Stock State:** When `stockQuantity == 0`, button is disabled (`bg-[#F5F5F4] text-[#78716C] border-[#E7E5E4] cursor-not-allowed`) displaying "OUT OF STOCK".

### 3.9 Product Description & Catalog Details
*   **Section Header:** `py-12 border-t border-[#E7E5E4] mt-16`.
*   **Purpose:** Present product information directly supported by `ProductDto` without inventing unbacked database attributes.
*   **Editorial Overview:** Full descriptive text from `ProductDto.description` formatted in Plus Jakarta Sans `15px`, line-height `1.7`, text `#57534E`. Any hardware details (such as layout, material, or connectivity) are presented naturally as part of the descriptive text copy string, NOT parsed into synthetic table schema keys.
*   **Supported Catalog Details (Directly Mapped to ProductDto):**
    *   **SKU:** `ProductDto.sku` (e.g. `TG-KBM-001`)
    *   **Category:** `ProductDto.category.name` (e.g. `Keyboards & Mouse`)
    *   **Unit Price:** `ProductDto.price` (e.g. `4.490.000 ₫`)
    *   **Inventory Availability:** `ProductDto.stockQuantity` (e.g. `30 units available`)
*   **Explicit Audit Rule:** NO synthetic tabular attributes (NO `Chassis Material`, NO `Layout`, NO `Connectivity`, NO `Switch Type`, NO `QMK/VIA Support`, NO custom specs table) as the backend `ProductDto` does not support attribute key-value pairs or EAV/variant tables.

### 3.10 Footer (Approved Customer-Facing Utility Blueprint)
*   **Customer-Facing Structure (Reused from FE-03 without invented business/legal scope):**
    *   **Column 1 (Brand):** `TECHGEAR` wordmark and concise description ("Curated tech gear and workspace equipment.").
    *   **Column 2 (Catalog Categories):** Navigation links to the 6 official catalog categories: Audio, Keyboards & Mouse, Gaming, Accessories, Monitors, Laptop Accessories.
    *   **Column 3 (Customer Portal):** Direct links to supported sitemap flows: Order History (`/orders`), Account Profile (`/profile`).
    *   **Bottom Rule:** Clean copyright notice: `© 2026 TECHGEAR. All rights reserved.`
*   **Explicit Exclusions:** Strictly NO invented legal/policy pages (NO Shipping Policy, NO Returns & Warranty, NO Privacy Policy, NO Terms of Service), NO corporate entity suffix ("TECHGEAR INC."), and NO backend telemetry/metrics ("0ms latency", "SYSTEM OPERATIONAL").

---

## 4. State Specifications

### 4.1 Loading State
*   Displays while `GET /api/v1/products/{id}` is resolving.
*   Skeleton Geometry:
    *   Left bay: 55% width gray pulsed block (`#F5F5F4`, `animate-pulse`, aspect 4:3).
    *   Right column: Category bar (12px high), Title bar (32px high), Price bar (24px high), Stepper & Button container placeholders.
*   No intrusive modal spinners or blocking overlays.

### 4.2 Product Not Found State (404)
*   Triggered when `GET /api/v1/products/{id}` returns HTTP 404.
*   Container: Centered panel on `#FFFFFF`, border `1px solid #E7E5E4`, radius `6px`, padding `py-20 px-8 max-w-xl mx-auto my-16 text-center`.
*   Copy:
    *   *Eyebrow:* `CATALOG ERROR` (JetBrains Mono `11px`, `#78716C`).
    *   *Title:* "Product Not Found" (Plus Jakarta Sans `24px` bold, `#1C1917`).
    *   *Description:* "The equipment you are looking for does not exist or has been retired from the catalog."
    *   *Action:* Primary button "Return to Shop" (`href="/shop"`).

### 4.3 Out of Stock State
*   Triggered when `stockQuantity <= 0` or `active == false`.
*   Visual treatment:
    *   Stock indicator displays neutral dot + text `Out of Stock`.
    *   Quantity stepper controls disabled (`opacity-50`, cursor `not-allowed`).
    *   Add to Cart button disabled with copy "OUT OF STOCK".

### 4.4 Add to Cart Feedback States
*   **Success:** Temporary subtle banner or slide drawer confirmation:
    *   "Added 1 × Keychron Q1 Pro to cart."
    *   Quick link: "View Cart" (`href="/cart"`).
    *   Header cart counter increments immediately.
*   **Failure (Network / 400 Bad Request):**
    *   Inline error alert: "Unable to add this item to your cart. Please try again."
    *   Action: "Retry" button.

---

## 5. Backend API Mapping & Verification

All PDP actions map directly to frozen, audited microservice endpoints:

| UI Action / Component | Microservice | Endpoint | Method | Payload / Parameters | Success Response |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **Fetch Product Details** | `product-service` | `/api/v1/products/{id}` | `GET` | `id` (Path variable: Long) | `200 OK` → `ProductDto` |
| **Fetch Category Context** | `product-service` | `/api/v1/categories/{id}` | `GET` | `id` (Path variable: Long) | `200 OK` → `CategoryDto` |
| **Add to Cart** | `order-service` | `/api/v1/cart/items` | `POST` | `{ "productId": Long, "quantity": Integer }` | `200 OK` → `CartDto` |
| **Read Cart Count** | `order-service` | `/api/v1/cart` | `GET` | Authenticated via JWT bearer | `200 OK` → `CartDto` (sums item quantities) |

### Non-Supported Features Excluded by Strict YAGNI:
1.  **Variants (Color / Switch / Size):** `ProductDto` does not have child variant entities. Products are flat SKUs. Excluded.
2.  **Wishlist / Save for Later:** No wishlist microservice or table exists. Excluded.
3.  **Customer Reviews / Ratings:** No review microservice or rating column exists. Excluded.
4.  **Product Comparison:** Not supported by catalog data. Excluded.
5.  **Coupons / Promo Codes on PDP:** Discount evaluation occurs strictly at checkout in `order-service`. Excluded.
6.  **Related Products / AI Recommendations:** No recommendation engine exists. Excluded.

---

## 6. Responsive Adaptations

*   **Desktop (≥ 1024px):** 
    *   Asymmetric 55% / 45% split layout.
    *   Sticky right-column product info during long spec scrolling.
    *   Quantity stepper and Add to Cart button aligned horizontally side-by-side.
*   **Tablet (768px – 1023px):**
    *   Stacked layout with visual container taking 100% width on top (height capped at 480px).
    *   Product information below with full-width action controls.
*   **Mobile (< 768px):**
    *   Single-column sequence:
        1. Breadcrumb (truncated).
        2. Product visual (1:1 aspect ratio, full width).
        3. Category & Product title.
        4. Price & Availability.
        5. Quantity & Add to Cart button (stacked or sticky bottom action bar).
        6. Product description & catalog metadata.

---

## 7. Accessibility & Motion Guidelines

*   **Accessibility (WCAG 2.1 AA):**
    *   High-contrast text ratios: `#1C1917` on `#FAFAF9` is > 14:1.
    *   All interactive elements have visible `focus-visible` offset rings (`#1C1917` and `#D97706`).
    *   Semantic headings: single `<h1>` for product title, `<h2>` for description & catalog details.
    *   Quantity stepper buttons include explicit `aria-label` tags.
    *   Product image provides descriptive `alt` attribute (e.g. `alt="Keychron Q1 Pro Wireless Custom Mechanical Keyboard"`).
*   **Motion Guidelines (Restrained 4/10):**
    *   Image entrance: subtle fade-in (`opacity 0 -> 1`, `duration 200ms ease-out`).
    *   Quantity number change: micro-fade transition (`150ms`).
    *   CTA press: subtle scale feedback (`active:scale-[0.99]`).
    *   Zero parallax, zero continuous floating animations.

---

## 8. Stitch Visual Reference

*   **Project ID:** `113306237264437644` (TECHGEAR)
*   **Screen ID:** `c1d1802ae632448fa985d7fb79a25e8d`
*   **Screen Name:** `projects/113306237264437644/screens/c1d1802ae632448fa985d7fb79a25e8d`
*   **Screen Title:** `TECHGEAR — Keychron Q1 Pro Product Detail`
*   **Route:** `/products/:id`
*   **Design System:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
*   **Device Target:** Desktop (`2560 × 3394 px`)
*   **Screenshot Artifact:** [View Screenshot](https://lh3.googleusercontent.com/aida/AEtjO1XaYWTv7jKvFUaQSsPhv1L7_ohsXgHmMNNfcu-5FxJAzHv8HfPer6l3mQ0eNrSc5Mz0eKykvDOW8ptnbojgNxSwSz7bnniLvNijGL8pnduobd0AmmGhSM7IalDM3qcLmR1g-v8n0Gge6m0twmjUosoMzF-WYKf6ViAiQT7nTzpHd5KUL-0l_59EthPC29U0nQq1Kjh_2KHjUI0-flfDiqL7LV43ZaxFSQ1mWjPC9oKMRFZW6F1XkhbiJg)
*   **Visual Continuity:** Shares exact header, typography scales, border framing, and customer-facing footer with approved Home (`FE-02`) and Shop (`FE-03`) screens.
*   **Status:** `COMPLETE` / `READY FOR REVIEW`

