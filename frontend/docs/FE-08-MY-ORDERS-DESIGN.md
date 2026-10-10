# TECHGEAR — FE-08 My Orders Design Specification

> **Document ID:** FE-08-MY-ORDERS-DESIGN  
> **Route:** `/orders`  
> **Target Audience:** Authenticated Customer  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `28dd04db69474f418fb86492ce2655d3`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Purpose & Scope

The **My Orders** screen (`/orders`) is the authenticated customer's central order history vitrine. It enables a logged-in user to:
1. Review the chronological list of their purchases.
2. Inspect essential order-level metrics: Order ID, placement timestamp, order status, item count summary, and total amount paid.
3. Access individual order details via a direct primary navigation action (`VIEW ORDER` &rarr; `/orders/{orderId}`, leading to **FE-09**).

### Strict Boundary Enforcement
*   **List View Only:** FE-08 displays the list of customer orders and pagination controls. It does **not** inline full order details, line-item breakdowns, or delivery addresses (reserved strictly for FE-09).
*   **No Customer Cancellation in List:** Order cancellation is handled exclusively inside the single order detail view (**FE-09**) where complete order state and confirmation safeguards reside.
*   **Zero Speculative Features:** No search input, no status filter tabs, no date pickers, no reorder button, and no external tracking integrations.

---

## 2. Route & Navigation Architecture

*   **Canonical Route:** `/orders`
*   **Global Navigation Anchors:**
    *   **Header Navigation:** The `Account` link in the sticky 64px header reflects an active state (`underline underline-offset-8 decoration-2 decoration-[#1C1917] font-semibold`) when on `/orders`.
    *   **Footer Links:** Under `Customer Portal`, the `Order History` link (`href="/orders"`) is styled in bold high-contrast charcoal (`#1C1917`).
    *   **Post-Checkout Flows:**
        *   `PAYMENT_UNKNOWN` state in **FE-07** provides `VIEW MY ORDERS` (`href="/orders"`).
*   **Access Guard:** Protected route. Requires active authenticated JWT session. If an unauthenticated guest navigates to `/orders`, client router redirects to `/login?redirect=/orders`.

---

## 3. Backend Endpoint & Integration Contract

FE-08 is strictly coupled to the actual Spring Data JPA endpoint in `OrderController.java`:

### 3.1 Endpoint Details
*   **Endpoint:** `GET /api/v1/orders/user/{userId}`
*   **HTTP Method:** `GET`
*   **Controller Method:** `OrderController.getOrdersByUser(Long userId, Pageable pageable)` (`OrderController.java:122-131`)
*   **Gateway Routing:** Routed via API Gateway (`application.yml:85-98`) through `AuthenticationFilter` and `RequestRateLimiter` (`#{@userRateLimiter}`).
*   **Pagination Query Parameters (Spring Data `Pageable`):**
    *   `page`: 0-indexed page number (`?page=0`, default: `0`)
    *   `size`: Number of orders per page (`?size=10`, default: `10`, configured via `@PageableDefault(size = 10, sort = "createdAt")`)
    *   `sort`: Default sort field is `createdAt` (`?sort=createdAt,desc`)

### 3.2 Non-Existent Endpoints (Strict Verification)
*   There is **no** `GET /api/v1/orders` (all-orders endpoint).
*   There is **no** `GET /api/v1/orders/my-orders` endpoint.
*   There is **no** search, status filter, or date range filter parameter accepted by `OrderController.getOrdersByUser`.

---

## 4. Authentication & Security Policy

### 4.1 Header Requirements
*   **Authorization:** `Bearer <JWT_TOKEN>` must be sent with every request.
*   Validated downstream by `OrderJwtAuthenticationFilter` (`OrderSecurityConfig.java:49`).
*   Missing, malformed, or expired token returns HTTP `401 Unauthorized`:
    ```json
    {
      "status": 401,
      "message": "Valid bearer authentication required"
    }
    ```

### 4.2 Ownership & Boundary Isolation
*   **Enforcement in Controller:**
    ```java
    OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
    if (!principal.isAdmin() && !Long.valueOf(principal.userId()).equals(userId)) {
        throw new ApiException("Access denied to user orders", HttpStatus.FORBIDDEN);
    }
    ```
*   **Strict Customer Isolation:** A customer can **only** view orders matching their authenticated `userId`. Supplying another user's ID yields HTTP `403 Forbidden`:
    ```json
    {
      "status": 403,
      "message": "Access denied to user orders"
    }
    ```
*   **Frontend Data Binding:** The frontend pulls `userId` directly from the active client authentication session (`auth.user.id`).
*   **Zero User ID Selector:** There is **no UI selector, dropdown, or input** allowing a user to choose or modify `userId`.

---

## 5. Response Data Contract & Field Mapping

The backend returns a standard Spring Data `Page<OrderDto>`:

```json
{
  "content": [
    {
      "id": 105,
      "userId": 42,
      "items": [
        {
          "id": 201,
          "productId": 1,
          "productName": "Keychron Q1 Pro Wireless Custom Mechanical Keyboard",
          "productSku": "KEY-Q1P-BLK",
          "unitPrice": 4850000.00,
          "quantity": 1,
          "subtotal": 4850000.00
        }
      ],
      "totalAmount": 12480000.00,
      "status": "CONFIRMED",
      "shippingAddress": "123 Ba Trieu, Hoan Kiem, Hanoi",
      "paymentTransactionId": "TXN-9843-2026",
      "notes": "Handle with care",
      "createdAt": "2026-10-07T10:15:30",
      "updatedAt": "2026-10-07T10:15:35"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false, "empty": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": { "sorted": true, "unsorted": false, "empty": false },
  "numberOfElements": 1,
  "first": true,
  "empty": false
}
```

### 5.1 Fields Utilized in FE-08 List View

| Field Path | Type | Display Treatment in FE-08 |
| :--- | :---: | :--- |
| `content[].id` | `Long` | **Order Identifier:** Monospace font `Order #{id}` (`JetBrains Mono 14px SemiBold`, `#1C1917`). Used for navigation link `/orders/{id}`. |
| `content[].createdAt` | `LocalDateTime` | **Placement Timestamp:** Formatted as `DD MMM YYYY, HH:mm` (`JetBrains Mono 12px`, `#78716C`), e.g., `07 Oct 2026, 10:15`. |
| `content[].status` | `OrderStatus` | **Status Badge:** Restrained Swiss pill tag with subtle border and fill (see Section 6). |
| `content[].items` | `List<OrderItemDto>` | **Item Summary:** Evaluates items count (`{items.length} item(s)`) and displays first product name: `1 item • Keychron Q1 Pro Wireless` (`Plus Jakarta Sans 14px`, `#1C1917`). |
| `content[].totalAmount` | `BigDecimal` | **Total Paid:** Formatted in VND tabular monospace: `12.480.000 ₫` (`JetBrains Mono 14px Bold`, `#1C1917`). |
| `totalPages` | `int` | Controls pagination total count display and disable condition for "Next". |
| `totalElements` | `long` | Counter display: *"Showing {from} to {to} of {totalElements} orders"*. |
| `number` | `int` | Active page index (0-indexed, converted to 1-indexed for display: `number + 1`). |
| `first` / `last` | `boolean` | Toggles disabled styling on Previous / Next pagination buttons. |
| `empty` | `boolean` | Triggers the empty state when `true`. |

### 5.2 Explicit Exclusion of Fabricated Fields (Strict YAGNI)
The backend does **not** supply the following fields, and they are strictly **omitted**:
*   ❌ No tracking numbers or courier names.
*   ❌ No estimated delivery dates.
*   ❌ No tax or shipping fee line items.
*   ❌ No discount or promo code values.
*   ❌ No payment gateway brand logos.
*   ❌ No product thumbnail images in list view (`OrderItemDto` provides `productName` and `productSku`, not image URLs).
*   ❌ No downloadable invoices or VAT protocol declarations.

---

## 6. Order Status Specifications

The backend defines 9 statuses in `OrderStatus.java`. FE-08 renders each with calibrated **Obsidian & Paper** status tokens:

| Backend `OrderStatus` | Customer-Facing Text | Foreground | Background | Border | Semantics |
| :--- | :--- | :---: | :---: | :---: | :--- |
| `CONFIRMED` | `CONFIRMED` | `#15803D` | `#F0FDF4` | `#BBF7D0` | Order paid and confirmed in system. |
| `PROCESSING` | `PROCESSING` | `#0369A1` | `#F0F9FF` | `#BAE6FD` | Warehouse preparation underway. |
| `SHIPPED` | `SHIPPED` | `#0369A1` | `#F0F9FF` | `#BAE6FD` | Dispatched from archival hub. |
| `DELIVERED` | `DELIVERED` | `#1C1917` | `#F5F5F4` | `#E7E5E4` | Order completed and delivered. |
| `PENDING` | `PENDING` | `#B45309` | `#FFFBEB` | `#FDE68A` | Order awaiting confirmation/payment. |
| `PAYMENT_FAILED` | `PAYMENT FAILED` | `#DC2626` | `#FEF2F2` | `#FECACA` | Payment simulation declined, stock released. |
| `PAYMENT_UNKNOWN`| `PAYMENT UNKNOWN` | `#B45309` | `#FFFBEB` | `#FDE68A` | Indeterminate gateway response. |
| `CANCELLED` | `CANCELLED` | `#DC2626` | `#FEF2F2` | `#FECACA` | Cancelled by customer or system. |
| `REFUNDED` | `REFUNDED` | `#78716C` | `#F5F5F4` | `#E7E5E4` | Transaction refunded. |

> [!NOTE]
> **Transient Checkout Status Handling:**  
> The transient string status `CONFIRMED_CART_CLEAR_PENDING` (returned as HTTP 202 during checkout) is recorded in the database and entity model as `OrderStatus.CONFIRMED`. By the time orders are queried from `/orders/user/{userId}`, they appear as `CONFIRMED`. No internal persistence mechanics, compensation wording, or Redis indicators are displayed to the user.

---

## 7. Visual Hierarchy & ASCII Layout Architecture

FE-08 uses an architectural spec-sheet table layout centered within the standard TECHGEAR viewport:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Sticky 64px Nav — TECHGEAR STORE, Shop, Account [Active], [0]) │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                       [ Canvas: #FAFAF9 ]                              │
│                                                                        │
│   ACCOUNT // ORDER HISTORY                                             │
│   MY ORDERS                                                            │
│   Review your past purchases, order status, and transaction totals.    │
│   ──────────────────────────────────────────────────────────────────   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ 2. ARCHITECTURAL ORDER HISTORY TABLE CARD                      │   │
│   │    (Max-width 1100px, 1px solid #E7E5E4, bg #FFFFFF, r-6px)    │   │
│   │ ┌────────────────────────────────────────────────────────────┐ │   │
│   │ │ ORDER NUMBER │ DATE     │ STATUS    │ ITEMS │ TOTAL │ ACTION│ │   │
│   │ ├────────────────────────────────────────────────────────────┤ │   │
│   │ │ Order #105   │ 07 Oct.. │[CONFIRMED]│ 1 it..│ 12.4M │[VIEW] │ │   │
│   │ │ Order #102   │ 28 Sep.. │[PROCESS..]│ 2 it..│  3.2M │[VIEW] │ │   │
│   │ │ Order #089   │ 15 Aug.. │[DELIVER..]│ 1 it..│  6.8M │[VIEW] │ │   │
│   │ ├────────────────────────────────────────────────────────────┤ │   │
│   │ │ Showing 1 to 3 of 3 orders      [Previous]  [1]  [Next]     │ │   │
│   │ └────────────────────────────────────────────────────────────┘ │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ 3. FOOTER (Shared 4-Column Customer Architecture)                      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Desktop Layout Specifications (&ge; 1024px)

*   **Max Width & Padding:** `max-w-[1100px] mx-auto px-8 py-12`.
*   **Header Section:**
    *   Breadcrumb: `ACCOUNT // ORDER HISTORY` (`JetBrains Mono 11px`, `#78716C`, uppercase tracking-widest).
    *   Title: `MY ORDERS` (`Plus Jakarta Sans 30px`, font-extrabold, tracking-tight, uppercase, `#1C1917`).
    *   Subtitle: *"Review your past purchases, order status, and transaction totals."* (`Plus Jakarta Sans 14px`, `#57534E`).
    *   Divider: Hairline rule `border-b border-[#E7E5E4] pb-6`.
*   **Order History Table Card:**
    *   Container: `border border-[#E7E5E4] bg-[#FFFFFF] rounded-[6px] overflow-hidden mt-8 shadow-none`.
    *   Table Header Row: `bg-[#F5F5F4] border-b border-[#E7E5E4] px-6 py-3.5 grid grid-cols-12 gap-4 items-center font-mono text-[11px] font-semibold tracking-wider text-[#78716C] uppercase`:
        *   Cols 1–2 (span 2): `ORDER NUMBER`
        *   Cols 3–4 (span 2): `DATE`
        *   Cols 5–6 (span 2): `STATUS`
        *   Cols 7–9 (span 3): `ITEMS`
        *   Cols 10–11 (span 2): `TOTAL`
        *   Col 12 (span 1, right-aligned): `ACTION`
    *   Table Rows:
        *   Grid: `grid grid-cols-12 gap-4 items-center px-6 py-5 border-b border-[#E7E5E4] last:border-b-0 hover:bg-[#FAFAF9] transition-colors`.
        *   Order Number: `JetBrains Mono 14px font-bold text-[#1C1917]`.
        *   Date: `JetBrains Mono 12px text-[#78716C]`.
        *   Status: Swiss tag with 2px radius, font-mono 11px, px-2.5 py-1.
        *   Items: `Plus Jakarta Sans 14px text-[#1C1917] truncate`.
        *   Total: `JetBrains Mono 14px font-bold text-[#1C1917] tabular-nums`.
        *   Action CTA: Machined outline button `VIEW ORDER` (`href="/orders/{id}"`, h-9 px-4 text-xs font-mono font-semibold tracking-wider uppercase text-[#1C1917] bg-[#FFFFFF] border border-[#E7E5E4] rounded-[4px] hover:bg-[#F5F5F4] hover:border-[#1C1917] transition-all flex items-center justify-center).
*   **Pagination Bar:**
    *   Container: `px-6 py-4 bg-[#FAFAF9] border-t border-[#E7E5E4] flex justify-between items-center`.
    *   Summary Text: `JetBrains Mono 12px text-[#78716C]` (*"Showing 1 to 3 of 3 orders"*).
    *   Controls:
        *   Previous: `px-3 py-1.5 text-xs font-mono rounded-[4px] border border-[#E7E5E4] bg-[#FFFFFF] text-[#A8A29E] cursor-not-allowed` (when `first == true`).
        *   Page Number: Active `w-8 h-8 flex items-center justify-center bg-[#1C1917] text-[#FAFAF9] font-mono text-xs rounded-[4px] font-bold`.
        *   Next: `px-3 py-1.5 text-xs font-mono rounded-[4px] border border-[#E7E5E4] bg-[#FFFFFF] text-[#A8A29E] cursor-not-allowed` (when `last == true`).

---

## 9. Mobile Responsive Layout (< 768px)

On mobile screens, the 12-column table collapses cleanly into structured stacked order cards to preserve readability without horizontal overflow:

*   **Container:** Single-column layout with `px-4 py-8`.
*   **Stacked Card Anatomy:**
    *   Header Line (flex justify-between items-center):
        *   Left: `Order #{id}` (`JetBrains Mono 14px font-bold text-[#1C1917]`).
        *   Right: Status Badge (e.g. `[CONFIRMED]`).
    *   Meta Line: `createdAt` date (`JetBrains Mono 12px text-[#78716C]`).
    *   Items Summary: Product title snippet (`Plus Jakarta Sans 13px text-[#57534E]`).
    *   Total Line (flex justify-between items-baseline pt-2 border-t border-[#E7E5E4]):
        *   Label: `TOTAL` (`JetBrains Mono 11px uppercase text-[#78716C]`).
        *   Value: `12.480.000 ₫` (`JetBrains Mono 15px font-bold text-[#1C1917]`).
    *   Action CTA: Full-width machined outline button `VIEW ORDER` with **44px touch target** height (`rounded-[4px]`).
*   **Pagination on Mobile:** Stacked layout with Prev/Next buttons filling equal halves below the summary text.

---

## 10. Loading State

When fetching data from `GET /api/v1/orders/user/{userId}`:

*   **Structure:** Skeleton rows replicating the table grid structure.
*   **Visual Treatment:**
    *   Container: White card with `1px solid #E7E5E4` border.
    *   Rows: 3 rows with subdued rectangular placeholders (`h-4 bg-[#E7E5E4] rounded-[2px] animate-pulse`).
    *   Action button placeholder: `h-9 w-24 bg-[#F5F5F4] border border-[#E7E5E4] rounded-[4px] animate-pulse`.
*   **Strict Restraint:** No circular spinner wheels, no artificial loading delay, no progress percentages.

---

## 11. Empty State

When the customer has zero orders (`empty == true` or `totalElements == 0`):

*   **Container:** Centered architectural card on `#FAFAF9` canvas (`max-w-md mx-auto py-16 px-6 text-center bg-[#FFFFFF] border border-[#E7E5E4] rounded-[6px]`).
*   **Visual Elements:**
    *   Icon: Understated shopping bag glyph inside `w-12 h-12 mx-auto mb-4 rounded-full border border-[#E7E5E4] bg-[#F5F5F4] flex items-center justify-center text-[#78716C]`.
    *   Headline: `NO ORDERS YET` (`Plus Jakarta Sans 22px font-bold uppercase tracking-tight text-[#1C1917]`).
    *   Subtitle: *"You haven't placed an order yet."* (`Plus Jakarta Sans 14px text-[#57534E] mb-6`).
    *   Action CTA: Solid Obsidian button `START SHOPPING` (`href="/shop"`, h-11 px-6 uppercase font-mono text-xs font-semibold tracking-wider text-[#FAFAF9] bg-[#1C1917] hover:bg-[#292524] rounded-[4px] inline-flex items-center justify-center transition-colors`).
*   **Strict Restraint:** No product recommendations, promotional coupons, or fake discounts injected into the empty state.

---

## 12. Error State

When the API request fails (HTTP 500, network loss, or timeout):

*   **Container:** Centered card with warning hairline border (`max-w-md mx-auto py-12 px-6 text-center bg-[#FFFFFF] border border-[#FECACA] rounded-[6px]`).
*   **Visual Elements:**
    *   Icon: Subtle red alert glyph inside `w-12 h-12 mx-auto mb-4 rounded-full border border-[#FECACA] bg-[#FEF2F2] flex items-center justify-center text-[#DC2626]`.
    *   Headline: `UNABLE TO LOAD ORDERS` (`Plus Jakarta Sans 20px font-bold uppercase tracking-tight text-[#1C1917]`).
    *   Subtitle: *"We couldn't retrieve your order history. Please try again."* (`Plus Jakarta Sans 14px text-[#57534E] mb-6`).
    *   Action CTA: Machined outline button `RETRY` (calls the same `getOrdersByUser` query, h-10 px-6 uppercase font-mono text-xs font-semibold tracking-wider text-[#1C1917] bg-[#FFFFFF] border border-[#E7E5E4] hover:bg-[#F5F5F4] rounded-[4px] transition-colors`).
*   **Strict Restraint:** The retry action purely re-triggers the existing `GET` query. No retry services or backend modifications are introduced.

---

## 13. Navigation to FE-09 (Order Detail)

*   **Navigation Target:** `/orders/{orderId}`
*   **Trigger:** Click on `VIEW ORDER` in any row.
*   **Data Passing:** Zero reliance on temporary client router state or memory passing. FE-09 independently fetches its required data using the dedicated backend endpoint `GET /api/v1/orders/{id}`.
*   **Scope Isolation:** FE-08 does not contain modal popups or inline accordions for order details.

---

## 14. Motion & Micro-interactions

Following `ui-ux-pro-max` and `design-taste-frontend` dial settings:
*   `DESIGN_VARIANCE: 7`
*   `MOTION_INTENSITY: 3` (Restrained, functional, non-distracting)
*   `VISUAL_DENSITY: 3` (Airy, architectural, tabular)
*   **Approved Micro-interactions:**
    *   **Table Entrance:** Subtle opacity transition (`opacity: 0 &rarr; 1`, `translateY: 4px &rarr; 0px`, `200ms ease-out`).
    *   **Row Hover:** Gentle background shift to `#FAFAF9` (`150ms ease`).
    *   **Button Hover:** Machined border darkening from `#E7E5E4` to `#1C1917` (`150ms ease`).
*   **Strictly Prohibited:** Staggered row cascades > 100ms, bounce effects, spring oscillations, decorative 3D tilt, confetti.

---

## 15. Explicit Exclusions (Strict YAGNI Guarantee)

The following capabilities are **explicitly excluded** from FE-08:

| Excluded Feature | Rationale |
| :--- | :--- |
| **Order Search Bar** | Backend `getOrdersByUser` does not provide full-text search parameters. |
| **Status Filter Tabs** | Backend endpoint does not take a `status` query parameter. |
| **Date Range Filter** | Backend endpoint does not accept date range parameters. |
| **Sort Dropdown** | Backend defaults to `createdAt` sort; custom client sort is speculative scope. |
| **Cancel Order Button** | Cancellation is restricted to FE-09 Order Detail to prevent accidental double-cancellations. |
| **Refund Request Button** | Backend refunding is admin-only via `payment-service` (`POST /payments/refund`). |
| **Reorder / Buy Again** | No composite reorder API exists in backend. |
| **Carrier Tracking / Couriers** | Backend has no logistics/shipping integration or tracking numbers. |
| **VAT Invoice Generation** | No invoice generation service exists in the repository. |
| **User ID Selector** | Security violation; customers can only query their own authenticated orders. |

---

## 16. Stitch Screen Asset & Metadata

*   **Project ID:** `113306237264437644`
*   **Screen ID:** `28dd04db69474f418fb86492ce2655d3`
*   **Screen Title:** `TECHGEAR — My Orders`
*   **Active Design System:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
*   **Device Type:** `DESKTOP` (`2560 x 2152`)
*   **Stitch Screenshot URL:**  
    `https://lh3.googleusercontent.com/aida/AEtjO1XxdW0ILDTiAZPo1hM4YVqOhBENkNns-89jjKgTNkCeDi-Efqc0zLBoN1l4ehqH421lXSOC3qjOcQyWP-T_fu0cfhEQA-D8WNpOjbDn59r6Xd5yUx8YS0XAuLdiF7YN6Bcs7zZqaHqwq7ftQ-G8Q89dMRxNazYjw1BDN7hWvzmFOsHZCYEmqv3rteeQM6yrYkYoTcf46ble-kCDSZbhwe9Bf7TYWuM_tcEglymZ3d5ybbOo7BUQ2m46og`

---

## 17. Final Verification Checklist

- [x] **Endpoint Verified:** `GET /api/v1/orders/user/{userId}` confirmed in `OrderController.java:122-131`.
- [x] **Response Fields Verified:** Spring Data `Page<OrderDto>` containing `id`, `createdAt`, `status`, `items`, `totalAmount`, `totalPages`, `totalElements`.
- [x] **OrderStatus Verified:** Real statuses checked against `OrderStatus.java:1-14`.
- [x] **Ownership/Security Verified:** Confirmed 403 enforcement for non-owned IDs (`OrderController.java:127-129`).
- [x] **Zero User ID Selector:** UI strictly binds to session `userId`.
- [x] **Zero Fabricated Fields:** No tracking numbers, couriers, tax breakdowns, or invoices.
- [x] **Zero Speculative Controls:** No filter tabs, sort dropdowns, or search inputs.
- [x] **Empty State Specified:** "NO ORDERS YET" with "START SHOPPING" CTA.
- [x] **Loading State Specified:** Subtle 3-row skeleton loader.
- [x] **Error State Specified:** "UNABLE TO LOAD ORDERS" with simple retry.
- [x] **Navigation to FE-09 Configured:** `VIEW ORDER` &rarr; `/orders/{orderId}`.
- [x] **Responsive Specifications:** Stacked layout defined for mobile (&ge; 44px touch targets).
- [x] **Restrained Motion:** 150–200ms subtle micro-interactions only.
- [x] **Design System Reused:** Reused *Obsidian & Paper* (`assets/592f25a1d5524b0091e02d25cfc6dd89`).
- [x] **Stitch Screen Generated & Cleaned:** Screen `28dd04db69474f418fb86492ce2655d3` verified.
- [x] **Zero Backend Changes:** No files modified in backend services.
- [x] **Zero React Code Added:** Specification document and Stitch screen only.
- [x] **Zero Git Commits / Pushes:** Workspace clean and unstaged.
