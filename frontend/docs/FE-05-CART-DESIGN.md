# TECHGEAR — Cart Design Specification

> **Document ID:** FE-05-CART-DESIGN  
> **Route:** `/cart`  
> **Version:** 1.0.0  
> **Screen:** Customer Shopping Cart  
> **Visual Reference:** Stitch Project (`projectId: 113306237264437644`)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Backend Verification:** Spring Boot 3.2 `order-service` (`CartController`, `CartService`, `CartRedisRepository`) Verified  
> **Status:** READY FOR REVIEW

---

## 1. Purpose & User Goal

*   **Purpose:** The Cart screen is the dedicated commerce holding area and validation stage of TECHGEAR. It allows customers to review selected equipment, modify purchase quantities, eliminate items, and verify order commitment before transitioning to checkout.
*   **User Goal:** Answer four core questions with absolute clarity and zero friction:
    1.  *What equipment am I purchasing?* (Product name, SKU, studio image, unit price)
    2.  *How many units am I purchasing?* (Interactive quantity stepper)
    3.  *What is my current financial commitment?* (Item subtotal and cart total price in VND)
    4.  *How do I proceed to checkout?* (Prominent "Proceed to Checkout" primary action)
*   **Customer Flow Context:**
    ```
    Home (FE-02) 
      → Shop (FE-03) 
      → Product Detail (FE-04) 
      → Add to Cart 
      → CART (FE-05) 
      → Checkout (/checkout) 
      → Payment (/payment/result) 
      → Order Detail (/orders/:id)
    ```

---

## 2. Backend Cart API & DTO Verification

The Cart screen is 100% grounded in the audited `order-service` implementation. All UI data models map strictly to the existing Redis-backed cart architecture (`CartDto` and `CartItemDto`).

### 2.1 Confirmed API Endpoints

| Endpoint | HTTP Method | Request Payload / Params | Return Type | Backend Behavior |
| :--- | :---: | :--- | :--- | :--- |
| `/api/v1/cart` | `GET` | Authenticated via JWT | `CartDto` | Retrieves the customer's active cart from Redis. Returns an empty `CartDto` with `itemCount: 0` if no cart exists. |
| `/api/v1/cart/items/{itemId}` | `PATCH` | `{ "quantity": Integer }` (`UpdateQuantityRequest`) | `CartDto` | Updates line item quantity. If `quantity == 0`, the item is automatically removed. Caps at `MAX_QUANTITY = 9999`. Recalculates subtotals and `totalPrice`. |
| `/api/v1/cart/items/{itemId}` | `DELETE` | Path variable `itemId: Long` | `CartDto` | Deletes the specific line item. Recalculates cart and returns updated `CartDto`. |
| `/api/v1/cart` | `DELETE` | Authenticated via JWT | `204 No Content` | Clears all items in the user's cart in Redis. |

### 2.2 Backend Confirmed DTO Fields

#### `CartDto` (Top-level Response)
*   `id`: `Long` (Cart entity ID)
*   `userId`: `Long` (Authenticated customer ID)
*   `items`: `List<CartItemDto>` (Active line items)
*   `totalPrice`: `BigDecimal` (Sum of all item subtotals)
*   `itemCount`: `int` (Number of distinct line items in cart)
*   `updatedAt`: `LocalDateTime` (Timestamp of last cart modification)

#### `CartItemDto` (Line Item Model)
*   `id`: `Long` (Unique line item sequence ID used as `itemId` in PATCH/DELETE)
*   `productId`: `Long` (Reference to catalog product)
*   `productName`: `String` (Live snapshot of product name)
*   `productSku`: `String` (Hardware SKU, e.g. `TG-KBM-001`)
*   `unitPrice`: `BigDecimal` (Unit price snapshot in VND)
*   `quantity`: `Integer` (Current quantity)
*   `subtotal`: `BigDecimal` (Calculated by backend as `unitPrice × quantity`)
*   `imageUrl`: `String` (Image URL snapshot)

#### `UpdateQuantityRequest` (Patch Payload)
*   `quantity`: `Integer` (`@NotNull`, `@Min(0)`, `@Max(9999)`)

> **Discrepancy & Field Audit Note:**
> *   `CartDto` does **NOT** contain separate tax, shipping, discount, coupon, voucher, or grand total fields.
> *   `CartItemDto` does **NOT** contain live warehouse stock quantity, variant attributes, or category fields.
> *   Therefore, the Cart UI displays strictly **Subtotal** and **Total** based on `totalPrice`. It does **NOT** invent fake coupons, fake discounts, or fake shipping calculators.

---

## 3. Visual System & Art Direction

Inherits 100% of the established **Premium Editorial Tech Commerce** design system (*Obsidian & Paper*):

*   **Canvas:** `#FAFAF9` (warm alabaster archival paper)
*   **Surface:** `#FFFFFF` (pure spec surface for item cards and summary box)
*   **Raised Containers:** `#F5F5F4` (milled neutral for hardware thumbnail bays)
*   **Borders:** `#E7E5E4` (1px solid precision dividers)
*   **Primary Text:** `#1C1917` (obsidian charcoal)
*   **Secondary Text:** `#57534E` (stone neutral) and `#78716C` (muted technical)
*   **Primary Accent:** `#D97706` / `#B45309` (Signal Amber — dedicated to the "Proceed to Checkout" primary conversion CTA)
*   **Typography:**
    *   Plus Jakarta Sans: Headings, product names, button labels, and instructional text.
    *   JetBrains Mono: Tabular prices (`tabular-nums`), SKUs, item counts, and quantity stepper digits.
*   **Geometry:** Strict 4px radius for buttons/steppers/inputs, 6px for summary card containers. Zero-shadow baseline throughout.

---

## 4. Page Structure & Desktop 2-Column Architecture

The Cart page adheres strictly to a 5-section layout within a `1280px` (`max-w-7xl`) centered container:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared 64px Sticky Nav — "Cart" Link Highlighted)           │
├────────────────────────────────────────────────────────────────────────┤
│ 2. PAGE HEADING                                                        │
│    Your Cart  (2 items)                                                │
├────────────────────────────────────────────────────────────────────────┤
│ 3 & 4. CART CONTENT (Desktop 2-Column Grid: 65% Items / 35% Summary)   │
│ ┌───────────────────────────────────────┬────────────────────────────┐ │
│ │ 3. CART ITEMS LIST (Left ~65%)        │ 4. ORDER SUMMARY (Right)   │ │
│ │ ┌───────────────────────────────────┐ │ ┌────────────────────────┐ │ │
│ │ │ [Img]  Keychron Q1 Pro            │ │ │ Order Summary          │ │ │
│ │ │ SKU: TG-KBM-001                   │ │ │ Subtotal   12.480.000 ₫│ │ │
│ │ │ 4.490.000 ₫                       │ │ │ Total      12.480.000 ₫│ │ │
│ │ │ [- 1 +]   Subtotal: 4.490.000 ₫   │ │ │                        │ │ │
│ │ │ [Remove]                          │ │ │ [PROCEED TO CHECKOUT]  │ │ │
│ │ └───────────────────────────────────┘ │ │ └────────────────────────┘ │ │
│ │ ┌───────────────────────────────────┐ │                            │ │
│ │ │ [Img]  Sony WH-1000XM5            │ │                            │ │
│ │ │ SKU: TG-AUD-001                   │ │                            │ │
│ │ │ 7.990.000 ₫                       │ │                            │ │
│ │ │ [- 1 +]   Subtotal: 7.990.000 ₫   │ │                            │ │
│ │ │ [Remove]                          │ │                            │ │
│ │ └───────────────────────────────────┘ │                            │ │
│ └───────────────────────────────────────┴────────────────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│ 5. FOOTER (Shared Customer-Facing Utility Blueprint)                   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Section-by-Section Visual & Functional Specification

### 5.1 Header (Shared Component)
*   **Dimensions:** Exact `64px` height, `sticky top-0 z-50`.
*   **Surface:** `#FAFAF9` with `backdrop-filter: blur(8px)`, hairline bottom border `1px solid #E7E5E4`.
*   **Navigation:** Brand wordmark `TECHGEAR` with monospace descriptor `STORE`.
*   **Context:** Cart icon and badge in active highlighted state (`text-[#1C1917] font-semibold`).
*   **Actions:** Search trigger (`⌘K`), User Account link, Cart trigger displaying real-time item count badge `[2]`.

### 5.2 Page Heading & Item Counter
*   **Top/Bottom Spacing:** `pt-10 pb-6 border-b border-[#E7E5E4] mb-8`.
*   **Hierarchy:**
    *   *Title Row:* Flex container with:
        *   Title: `Your Cart` (Plus Jakarta Sans `32px` bold, tracking `-0.03em`, `#1C1917`).
        *   Item Count Indicator: Simple customer-facing text `2 items` (Plus Jakarta Sans `14px`, text `#78716C`, font normal, aligned alongside title).
*   **Anti-Slop Restraint:** Strictly no pseudo-technical labels or variants such as `SELECTION`, `CATALOG`, `SYSTEM`, `TELEMETRY`, or `STATUS`.

### 5.3 Cart Items List (Left Column ~65%)
*   **Container:** `space-y-4` rendered on `#FAFAF9`.
*   **Individual Cart Item Card:**
    *   Surface: `#FFFFFF`, border `1px solid #E7E5E4`, corner radius `6px`, padding `20px` (`p-5`).
    *   Layout: Horizontal row grid (`flex flex-col sm:flex-row gap-5 items-start sm:items-center justify-between`).
    *   **Anatomy:**
        1.  **Product Thumbnail Bay:**
            *   Size: `80px × 80px` (or `96px × 96px`).
            *   Surface: Milled neutral `#F5F5F4`, border `1px solid #E7E5E4`, radius `4px`.
            *   Image: Centered hardware product photography from `CartItemDto.imageUrl`. If empty/broken, displays a neutral studio hardware silhouette placeholder.
        2.  **Product Metadata Block (Flex Grow):**
            *   Product Name: Plus Jakarta Sans `15px` SemiBold (`#1C1917`), clickable link leading to `/products/{productId}`.
            *   SKU Tag: JetBrains Mono `11px`, `#78716C` (e.g. `SKU: TG-KBM-001`).
            *   Unit Price: JetBrains Mono `13px`, `#57534E` (e.g. `4.490.000 ₫ each`).
        3.  **Quantity Stepper Control:**
            *   Rectangular stepper: `[ − ] [  1  ] [ + ]`, height `36px`, border `1px solid #E7E5E4`, radius `4px`, background `#FFFFFF`.
            *   Minus Button: `w-9 h-9 flex items-center justify-center text-[#57534E] hover:text-[#1C1917] hover:bg-[#F5F5F4]`. Disabled when quantity is 1 (unless customer explicitly triggers remove).
            *   Digit Display: `w-10 text-center font-mono text-sm font-semibold text-[#1C1917]`.
            *   Plus Button: `w-9 h-9 flex items-center justify-center text-[#57534E] hover:text-[#1C1917] hover:bg-[#F5F5F4]`. Disabled when quantity reaches `9999`.
            *   Updating State: Subtle low-opacity shimmer on digit during `PATCH` execution.
        4.  **Item Subtotal Block:**
            *   Value: JetBrains Mono `15px` SemiBold (`#1C1917`, tabular numbers), e.g. `4.490.000 ₫`.
            *   Label: JetBrains Mono `10px`, `#78716C`, uppercase (`SUBTOTAL`).
        5.  **Remove Action:**
            *   Understated text link with trash/close indicator: `Remove` (Plus Jakarta Sans `12px`, `#78716C`, hover: `#B91C1C` / red-700, underline on hover).
            *   Accessible `aria-label="Remove Keychron Q1 Pro from cart"`.

### 5.4 Order Summary Panel (Right Column ~35%)
*   **Container:** Sticky container (`sticky top-24`), surface `#FFFFFF`, border `1px solid #E7E5E4`, radius `6px`, padding `24px` (`p-6`).
*   **Header:** `Order Summary` (Plus Jakarta Sans `18px` SemiBold, `#1C1917`, border-b border-[#E7E5E4] pb-4 mb-5).
*   **Line Items Breakdown:**
    *   **Subtotal:**
        *   Label: `Subtotal` (Plus Jakarta Sans `14px`, `#57534E`).
        *   Value: JetBrains Mono `15px`, Medium, `#1C1917` (e.g. `12.480.000 ₫`).
    *   **Divider:** Hairline border `border-t border-[#E7E5E4] my-4`.
    *   **Total Order Value:**
        *   Label: `Total` (Plus Jakarta Sans `16px`, SemiBold, `#1C1917`).
        *   Value: JetBrains Mono `22px`, Bold, `#1C1917` (e.g. `12.480.000 ₫`).
*   **Truthful Context Notice:**
    *   Text: "Final order details are confirmed at checkout." (Plus Jakarta Sans `12px`, `#78716C`, mt-2 mb-6 leading-relaxed).
    *   Strictly excludes unconfirmed tax/shipping claims, fake promotional banners, or non-existent discount inputs.
*   **Primary Conversion CTA:**
    *   Button: `Proceed to Checkout` (`href="/checkout"`).
    *   Styling: Solid **Signal Amber** (`bg-[#D97706] hover:bg-[#B45309] text-white w-full h-12 rounded-[4px] font-semibold text-sm tracking-wide uppercase flex items-center justify-center transition-colors`).
    *   Disabled State: Grayed out (`bg-[#E7E5E4] text-[#78716C] cursor-not-allowed`) if `items.length == 0`.
*   **Secondary Action:**
    *   Link: `Continue Shopping` (`href="/shop"`), centered, Plus Jakarta Sans `13px`, text `#57534E`, hover `#1C1917`, mt-4.

### 5.5 Footer (Shared Component)
*   **Customer-Facing Structure:** 100% identical to the approved FE-03 / FE-04 footer.
*   Column 1 (Brand): `TECHGEAR` ("Curated tech gear and workspace equipment.").
*   Column 2 (Categories): 6 official catalog categories (Audio, Keyboards & Mouse, Gaming, Accessories, Monitors, Laptop Accessories).
*   Column 3 (Customer Portal): Order History (`/orders`), Account Profile (`/profile`).
*   Bottom Rule: `© 2026 TECHGEAR. All rights reserved.`
*   Zero exposed microservices, zero Spring Boot stamps, zero telemetry latency metrics.

---

## 6. Interaction & Error Handling Specifications

### 6.1 Quantity Update Flow (`PATCH /api/v1/cart/items/{itemId}`)
1.  **User Trigger:** Customer clicks `[+]` or `[−]` on a line item.
2.  **Optimistic Visual State:** Digit updates immediately, quantity buttons enter subtle `pointer-events-none` state to prevent concurrent double-clicks.
3.  **API Call:** Frontend sends `PATCH /api/v1/cart/items/{itemId}` with payload `{"quantity": newQuantity}`.
4.  **Success:** Server returns updated `CartDto`. Frontend updates line item `subtotal` and Order Summary `totalPrice` with smooth numeric transition.
5.  **Failure / Network Error:** 
    *   Frontend reverts digit to previous value.
    *   Displays non-blocking inline toast: *"Unable to update quantity. Please try again."*
    *   Item is **NOT** removed or corrupted.

### 6.2 Item Removal Flow (`DELETE /api/v1/cart/items/{itemId}`)
1.  **User Trigger:** Customer clicks `Remove`.
2.  **Visual Transition:** Line item enters muted opacity (`opacity-50`).
3.  **API Call:** Frontend sends `DELETE /api/v1/cart/items/{itemId}`.
4.  **Success:** Server returns updated `CartDto`. Line item gracefully fades out (`200ms ease-out`), list collapses cleanly, Order Summary updates total price. Header cart badge decrements.
5.  **Failure:** Item restores full opacity. Displays inline error banner: *"Unable to remove item. Please try again."*

---

## 7. State Specifications

### 7.1 Loading State
*   **Trigger:** Displayed while `GET /api/v1/cart` is initially fetching.
*   **Skeleton Geometry:**
    *   Left column renders 2 skeleton card placeholders (`h-24 bg-[#FFFFFF] border border-[#E7E5E4] rounded-[6px] p-5 flex gap-4 animate-pulse`).
    *   Right column renders skeleton summary card (`h-64 bg-[#FFFFFF] border border-[#E7E5E4] rounded-[6px] p-6 animate-pulse`).
*   **Zero Layout Shift:** Containers match the exact dimensions of populated cards.

### 7.2 Empty Cart State
*   **Trigger:** Displayed when `CartDto.items` is empty (`itemCount == 0`).
*   **Container:** Centered vitrine panel on `#FFFFFF`, border `1px solid #E7E5E4`, radius `6px`, padding `py-20 px-8 max-w-xl mx-auto my-12 text-center`.
*   **Copy:**
    *   *Title:* `Your cart is empty.` (Plus Jakarta Sans `24px` bold, `#1C1917`).
    *   *Description:* "No equipment has been added yet. Explore our curated catalog of precision hardware." (Plus Jakarta Sans `14px`, `#57534E`, max-w-md mx-auto mt-2 mb-8 leading-relaxed).
    *   *Primary Action:* Solid Obsidian button `Explore Equipment Catalog` (`href="/shop"`), height `44px`, px-8, text-white, 4px radius.
*   **Anti-Slop Compliance:** Absolutely no fake "You may also like" recommendation carousels.

### 7.3 Service Error State
*   **Trigger:** Displayed when `GET /api/v1/cart` returns HTTP 500 or network failure.
*   **Container:** Centered card on `#FEF2F2` (soft red neutral), border `1px solid #FECACA`, radius `6px`, padding `p-8 max-w-lg mx-auto my-16 text-center`.
*   **Copy:**
    *   *Title:* "Unable to load your cart."
    *   *Description:* "A connection error occurred while retrieving your cart. Please try again."
    *   *Action:* Outlined retry button `Retry Connection`.
*   **Privacy Rule:** Never exposes Spring Boot, microservice names, Redis keys, or exception stack traces.

---

## 8. Responsive Adaptations

*   **Desktop (≥ 1024px):** 
    *   Structured 2-column layout (`65%` Cart Items / `35%` Order Summary).
    *   Order Summary panel sits sticky at `top-24` during vertical scrolling.
*   **Tablet (768px – 1023px):**
    *   2-column layout maintained with narrower item cards, or collapses to vertical stack if viewport height requires.
*   **Mobile (< 768px):**
    *   Single-column sequence:
        1. Page Heading with item count.
        2. Cart Items list (each card stacks thumbnail + title on row 1, stepper + subtotal on row 2, remove button on bottom right).
        3. Order Summary panel positioned directly beneath Cart Items.
    *   "Proceed to Checkout" button spans full viewport width with touch-friendly 48px height.

---

## 9. Accessibility & Motion

*   **Accessibility (WCAG 2.1 AA):**
    *   High-contrast text: `#1C1917` on `#FAFAF9` canvas (> 14:1 ratio).
    *   Quantity buttons provide explicit `aria-label`: `aria-label="Decrease quantity of [Product Name]"` and `aria-label="Increase quantity of [Product Name]"`.
    *   Remove button provides explicit `aria-label="Remove [Product Name] from cart"`.
    *   All interactive buttons have visible `focus-visible` offset outlines (`2px solid #1C1917`).
    *   Zero emoji icons used as functional controls.
*   **Motion Guidelines (Restrained 4/10):**
    *   Subtotal price update: Subtle micro-fade transition (`150ms`).
    *   Item removal: Smooth height collapse and fade-out (`200ms ease-out`).
    *   CTA press feedback: Subtle scale compression (`active:scale-[0.99]`).
    *   Zero parallax, zero floating drop-shadows.

---

## 10. Explicit Exclusions (Strict YAGNI)

In strict accordance with project constraints, the following features are deliberately **EXCLUDED**:
1.  **Coupon / Promo Code Engine:** Backend does not support coupon entry at the cart stage.
2.  **Shipping Cost Calculator:** Shipping selection and address calculations occur strictly on `/checkout`.
3.  **Taxes / VAT Line Item:** Backend does not provide separate tax breakdown fields in `CartDto`.
4.  **Wishlist / Save For Later:** No wishlist microservice exists.
5.  **Product Recommendations / Cross-sell:** No recommendation engine exists.
6.  **Real Payment Integration:** Payment occurs in `payment-service` downstream after checkout.
7.  **Guest Cart Syncing:** Backend requires authenticated user session (`OrderPrincipal`).

---

## 11. Stitch Visual Reference

*   **Project ID:** `113306237264437644` (TECHGEAR)
*   **Screen ID:** `9a5cdc33c0d14e91a77d98c80c79ab2a`
*   **Screen Name:** `projects/113306237264437644/screens/9a5cdc33c0d14e91a77d98c80c79ab2a`
*   **Screen Title:** `TECHGEAR — Customer Cart`
*   **Route:** `/cart`
*   **Design System:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
*   **Device Target:** Desktop (`2560 × 2176 px`)
*   **Screenshot Artifact:** [View Screenshot](https://lh3.googleusercontent.com/aida/AEtjO1UuWMN0wrYA665n4MPOldq6Q_fD8FYqa-JPN2ImBNRHhDrjEq6jO2VIW1ldGFIr0WnDorMi7C4X03kIfoLJy0AEQVTRTQs9NghZ0638FM0KKmiyjeFxhCTWJ0S9yWgtbfgTVRbGOv-ljfZMNdWdycUxWFzQlIBeYmshSBgipmsWSBHLcQGNDZZkV5PPAkM_Ek8SBf18TYYwxOJIxbJYAx7GgwevzLbJ9CjI81BuEqEBHfdHSqLPJEWYLg)
*   **Visual Continuity:** Reuses exact header, typography scale, border framing, and customer-facing footer from approved Home (`FE-02`), Shop (`FE-03`), and Product Detail (`FE-04`) screens.
*   **Status:** `COMPLETE` / `READY FOR REVIEW`

