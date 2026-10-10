# TECHGEAR — FE-09 Order Detail Design Specification

> **Document ID:** FE-09-ORDER-DETAIL-DESIGN  
> **Route:** `/orders/:orderId`  
> **Target Audience:** Authenticated Customer  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `50c596f075084ade9ae301896a10e7d6`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Purpose & Scope

The **Order Detail** screen (`/orders/:orderId`) serves as the authenticated customer's definitive order receipt and status inspection document. It allows a customer to:
1. Examine the complete itemized breakdown of a specific purchase (Product Names, SKUs, Unit Prices, Quantities, and Subtotals).
2. Inspect confirmed order metadata: Order Number, Placement Timestamp, Current Order Status, Final Total Amount, Shipping Address, Customer Delivery Notes, and Payment Transaction Reference.
3. Perform authorized customer lifecycle actions: Specifically **Cancel Order** when the order is in an eligible status (`PENDING` or `CONFIRMED`).

### Strict Architectural Boundaries
*   **Customer Document, Not Admin Dashboard:** Styled with museum vitrine restraint as an archival physical document, rejecting multi-chart enterprise dashboards, fake analytics, and decorative telemetry.
*   **Customer Scope Only:** Excludes admin-only controls (e.g. manual status overrides or refund triggers).
*   **Zero Speculative Features:** No courier tracking, shipping progress steppers, invoice VAT downloads, product re-ordering, reviews/ratings, or recommendation carousels.

---

## 2. Route & Navigation Architecture

*   **Canonical Route:** `/orders/:orderId` (e.g. `/orders/105`).
*   **Route Parameter:** `:orderId` (Numeric order ID matching backend primary key `id`).
*   **Navigation Entry Points:**
    *   **From My Orders (FE-08):** Clicking `VIEW ORDER` on any order row (`href="/orders/{id}"`).
    *   **From Payment Result (FE-07):** Clicking `VIEW ORDER` upon checkout confirmation (`href="/orders/{orderId}"`).
    *   **Breadcrumb Navigation:** `ORDERS / ORDER #{orderId}` at the top of the page; `ORDERS` navigates back to `/orders`.
*   **Access Guard:** Protected route requiring a valid customer JWT session. Unauthenticated access immediately redirects to `/login?redirect=/orders/:orderId`.

---

## 3. Backend Integration Contract [BACKEND-CONFIRMED]

Every data contract and lifecycle transition is strictly audited from `order-service` and `payment-service` source files:

### 3.1 Primary Fetch Endpoint
*   **Endpoint:** `GET /api/v1/orders/{id}`
*   **HTTP Method:** `GET`
*   **Controller Method:** `OrderController.getOrderById(@PathVariable Long id)` ([OrderController.java:112-120](file:///d:/CODE/WebJava/observable-microservices-ecommerce/order-service/src/main/java/com/ecommerce/orderservice/controller/OrderController.java#L112-L120))
*   **Gateway Routing:** Routed via API Gateway (`application.yml:87-98`) with `AuthenticationFilter` and `RequestRateLimiter`.
*   **Authentication:** Requires header `Authorization: Bearer <JWT_TOKEN>`.
*   **Ownership & IDOR Protection:**
    ```java
    OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
    OrderDto order = orderService.getOrderById(id);
    if (!principal.isAdmin() && !Long.valueOf(principal.userId()).equals(order.getUserId())) {
        throw new ApiException("Access denied to order " + id, HttpStatus.FORBIDDEN);
    }
    ```
    *   If the user is not authenticated: Returns HTTP `401 Unauthorized`.
    *   If the order ID does not exist: Returns HTTP `404 Not Found` (`OrderNotFoundException`).
    *   If the order exists but belongs to a different customer: Returns HTTP `403 Forbidden` (`"Access denied to order {id}"`).
    *   Customer cannot change the user identity in transit; the frontend extracts `orderId` strictly from the URL route.

### 3.2 Order Cancellation Endpoint
*   **Endpoint:** `POST /api/v1/orders/{id}/cancel`
*   **HTTP Method:** `POST`
*   **Controller Method:** `OrderController.cancelOrder(@PathVariable Long id)` ([OrderController.java:133-143](file:///d:/CODE/WebJava/observable-microservices-ecommerce/order-service/src/main/java/com/ecommerce/orderservice/controller/OrderController.java#L133-L143))
*   **Service Business Rules:** ([OrderService.java:414-426](file:///d:/CODE/WebJava/observable-microservices-ecommerce/order-service/src/main/java/com/ecommerce/orderservice/service/OrderService.java#L414-L426))
    ```java
    if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
        throw new OrderStatusException("Cannot cancel order in status: " + order.getStatus());
    }
    order.setStatus(OrderStatus.CANCELLED);
    ```
*   **Customer Cancellation Permission:** Customer **is authorized** to cancel their own order **only** while the status is `PENDING` or `CONFIRMED`.
*   **Cancellation Response:** Returns the updated `OrderDto` with `status: "CANCELLED"`.

### 3.3 Admin vs Customer Capability Matrix [BACKEND-CONFIRMED]

| Capability | Customer Permission | Admin Permission | Backend Endpoint & Enforcement |
| :--- | :---: | :---: | :--- |
| **View Order** | ✅ Own orders only | ✅ Any order | `GET /api/v1/orders/{id}` (`userId` match enforced) |
| **Cancel Order** | ✅ If `PENDING` or `CONFIRMED` | ✅ If `PENDING` or `CONFIRMED` | `POST /api/v1/orders/{id}/cancel` |
| **Update Status Manually** | ❌ Forbidden | ✅ Permitted | `PATCH /api/v1/orders/{id}/status` (`hasRole("ADMIN")`) |
| **Process Refund** | ❌ Forbidden | ✅ Permitted | `POST /api/v1/payments/refund` in `payment-service` (`hasRole("ADMIN")`) |

> [!CRITICAL]
> **Zero Customer Refund Actions:**  
> The backend enforces that payment refunds are strictly an **admin-only** operation (`PaymentSecurityConfig.java:48`). The customer-facing Order Detail interface **must not** render any "Request Refund", "Dispute Charge", or refund submission forms.

---

## 4. Data Contract & Response Fields [BACKEND-CONFIRMED]

### 4.1 Exact `OrderDto` Schema
```java
public class OrderDto {
    private Long id;
    private Long userId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;
    private String paymentTransactionId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4.2 Exact `OrderItemDto` Schema
```java
public class OrderItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
```

### 4.3 Field Mapping Matrix

| Source Field | Type | Verified In Backend | UI Presentation in FE-09 | Decision Type |
| :--- | :---: | :---: | :--- | :---: |
| `id` | `Long` | Yes | Header title: `ORDER #105` and breadcrumb `ORDER #105` (`JetBrains Mono`). | `BACKEND-CONFIRMED` |
| `createdAt` | `LocalDateTime` | Yes | Formatted as `Placed on 07 Oct 2026, 10:15` (`JetBrains Mono 12px`, `#78716C`). | `BACKEND-CONFIRMED` |
| `status` | `OrderStatus` | Yes | Status badge: `[CONFIRMED]` with calibrated color token. | `BACKEND-CONFIRMED` |
| `items` | `List<OrderItemDto>` | Yes | Section `ORDERED ITEMS ({items.length})` with itemized card rows. | `BACKEND-CONFIRMED` |
| `items[].productName` | `String` | Yes | Product headline title (`Plus Jakarta Sans 15px font-semibold`). | `BACKEND-CONFIRMED` |
| `items[].productSku` | `String` | Yes | Code line: `SKU: KEY-Q1P-BLK` (`JetBrains Mono 12px`). | `BACKEND-CONFIRMED` |
| `items[].quantity` | `Integer` | Yes | Meta tag: `QTY: 1` (`JetBrains Mono 12px`). | `BACKEND-CONFIRMED` |
| `items[].unitPrice` | `BigDecimal` | Yes | Spec line: `Unit Price: 4.850.000 ₫` (`JetBrains Mono 12px`). | `BACKEND-CONFIRMED` |
| `items[].subtotal` | `BigDecimal` | Yes | Right-aligned line price: `4.850.000 ₫` (`JetBrains Mono 15px Bold`). | `BACKEND-CONFIRMED` |
| `totalAmount` | `BigDecimal` | Yes | Prominent price readout: `12.480.000 ₫` (`JetBrains Mono 24px Bold`). | `BACKEND-CONFIRMED` |
| `shippingAddress` | `String` | Yes | Rendered under `SHIPPING ADDRESS` card (`Plus Jakarta Sans 14px`). | `BACKEND-CONFIRMED` |
| `notes` | `String` | Yes | Rendered under `DELIVERY NOTES` (italic, stone neutral) if non-blank. | `BACKEND-CONFIRMED` |
| `paymentTransactionId` | `String` | Yes | Rendered under `PAYMENT` card with label `Transaction ID` (`TXN-9843-2026`). Strictly omits infrastructure and verification terminology. | `BACKEND-CONFIRMED` |

### 4.4 Explicit Exclusions of Fabricated Fields [BACKEND-CONFIRMED & STRICT YAGNI]
The backend does **not** provide the following fields, and they are strictly **forbidden** from appearing in FE-09:
*   ❌ **No Carrier / Courier Name:** Backend has no shipping vendor integration.
*   ❌ **No Tracking Number or External Tracking Links:** No logistics tracking field exists.
*   ❌ **No Estimated Delivery Date:** No delivery calculation exists.
*   ❌ **No Tax / Shipping Fee Breakdown:** `OrderDto` contains only `totalAmount` and item `subtotal`.
*   ❌ **No Discount / Coupon Line Items:** Backend has no promotions engine.
*   ❌ **No Payment Method Icon / Card Logos:** `OrderDto` preserves only `paymentTransactionId`, not the card brand.
*   ❌ **No Payment Verification Infrastructure Terminology:** No wording like "verified via Payment Service", "payment verification", or "secure verification".
*   ❌ **No Product Thumbnail Images:** `OrderItemDto` contains name and SKU, but no image URL.
*   ❌ **No PDF Invoice Download / VAT Protocol Banners:** No invoice generation service exists.

---

## 5. Order Status Specifications [BACKEND-CONFIRMED]

FE-09 maps the exact 9 enum constants in `OrderStatus.java` to restrained, tactile Swiss badges:

| Backend `OrderStatus` | Customer-Facing Text | Text Color | Background | Border | Cancel Permitted? |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `CONFIRMED` | `[CONFIRMED]` | `#15803D` | `#F0FDF4` | `#BBF7D0` | **Yes** (Customer cancel allowed) |
| `PENDING` | `[PENDING]` | `#B45309` | `#FFFBEB` | `#FDE68A` | **Yes** (Customer cancel allowed) |
| `PROCESSING` | `[PROCESSING]` | `#0369A1` | `#F0F9FF` | `#BAE6FD` | ❌ No (Locked in warehouse) |
| `SHIPPED` | `[SHIPPED]` | `#0369A1` | `#F0F9FF` | `#BAE6FD` | ❌ No (Dispatched) |
| `DELIVERED` | `[DELIVERED]` | `#1C1917` | `#F5F5F4` | `#E7E5E4` | ❌ No (Order fulfilled) |
| `CANCELLED` | `[CANCELLED]` | `#DC2626` | `#FEF2F2` | `#FECACA` | ❌ No (Already cancelled) |
| `REFUNDED` | `[REFUNDED]` | `#78716C` | `#F5F5F4` | `#E7E5E4` | ❌ No (Already refunded) |
| `PAYMENT_FAILED` | `[PAYMENT FAILED]` | `#DC2626` | `#FEF2F2` | `#FECACA` | ❌ No (Order failed) |
| `PAYMENT_UNKNOWN`| `[PAYMENT UNKNOWN]`| `#B45309` | `#FFFBEB` | `#FDE68A` | ❌ No (Indeterminate) |

---

## 6. Order Cancellation Workflow [BACKEND-CONFIRMED & UI DECISION]

1. **Button Visibility Condition [BACKEND-CONFIRMED]:**  
   The `CANCEL ORDER` button is rendered in the header **only** when `order.status === 'PENDING'` or `order.status === 'CONFIRMED'`. For any other status, the button is omitted.
2. **Button Styling [UI DECISION]:**  
   Machined outline button (`h-9 px-4 text-xs font-mono font-semibold uppercase text-[#DC2626] border border-[#FECACA] bg-[#FFFFFF] hover:bg-[#FEF2F2] rounded-[4px] transition-colors`).
3. **Cancellation Flow (Strictly Safe — Zero Direct Execution on Click) [UI DECISION]:**
   ```
   [CANCEL ORDER] Click
          │
          ▼
   Confirmation Dialog
   ├── Title: "Cancel Order"
   ├── Question: "Are you sure you want to cancel this order? Reserved items will be released."
   └── Actions:
       ├── [Cancel Order] (Destructive Confirm)
       └── [Keep Order]   (Secondary Dismiss)
          │
          ├─────────────────────────┐
          ▼                         ▼
   [Cancel Order] Click      [Keep Order] Click
          │                         │
          ▼                         ▼
   POST /api/v1/orders/{id}/cancel  Closes dialog
          │                         (No state change)
          ▼
   Refresh Order Detail
   (Status updates to CANCELLED)
   ```
4. **Dialog Specifications:**
   *   **Dialog Title:** `Cancel Order` (`Plus Jakarta Sans 18px font-bold text-[#1C1917]`).
   *   **Confirmation Prompt:** *"Are you sure you want to cancel this order? Reserved items will be released."* (`Plus Jakarta Sans 14px text-[#57534E] leading-relaxed`).
   *   **Action Buttons:**
       *   Primary Destructive: Solid red / obsidian button `Cancel Order` (`h-10 px-4 text-xs font-mono font-semibold uppercase bg-[#DC2626] text-[#FFFFFF] rounded-[4px] hover:bg-[#B91C1C]`).
       *   Secondary Neutral: Machined outline button `Keep Order` (`h-10 px-4 text-xs font-mono font-semibold uppercase bg-[#FFFFFF] border border-[#E7E5E4] text-[#1C1917] rounded-[4px] hover:bg-[#F5F5F4]`).
5. **Execution & State Refresh:**
   *   Calls `POST /api/v1/orders/{id}/cancel` with customer's bearer token.
   *   Upon success (HTTP 200), client refreshes the order detail state (or re-invokes `GET /api/v1/orders/{id}`).
   *   Order status updates immediately to `CANCELLED`, the status badge shifts to `[CANCELLED]`, and the `CANCEL ORDER` button is removed.
   *   No new API or backend changes introduced.

---

## 7. Visual Hierarchy & ASCII Layout Architecture [UI DESIGN DECISION]

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Sticky 64px Nav — TECHGEAR STORE, Shop, Account [Active], [0]) │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                       [ Canvas: #FAFAF9 ]                              │
│                                                                        │
│   ORDERS / ORDER #105                                                  │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ ORDER #105                       [CONFIRMED]   [CANCEL ORDER]  │   │
│   │ Placed on 07 Oct 2026, 10:15                                   │   │
│   └────────────────────────────────────────────────────────────────┘   │
│   ──────────────────────────────────────────────────────────────────   │
│                                                                        │
│   ┌───────────────────────────────┐ ┌──────────────────────────────┐   │
│   │ 2. ORDERED ITEMS (2) (Span 8) │ │ 3. SPECIFICATIONS (Span 4)   │   │
│   │ ┌───────────────────────────┐ │ │ ┌──────────────────────────┐ │   │
│   │ │ Keychron Q1 Pro Wireless  │ │ │ │ ORDER TOTAL              │ │   │
│   │ │ SKU: KEY-Q1P-BLK • QTY: 1 │ │ │ │ 12.480.000 ₫             │ │   │
│   │ │ Unit Price: 4.850.000 ₫   │ │ │ └──────────────────────────┘ │   │
│   │ │ Subtotal: 4.850.000 ₫     │ │ │ ┌──────────────────────────┐ │   │
│   │ ├───────────────────────────┤ │ │ │ SHIPPING ADDRESS         │ │   │
│   │ │ Audioengine A2+ Wireless  │ │ │ │ 123 Ba Trieu, Hanoi      │ │   │
│   │ │ SKU: AUD-A2P-WHT • QTY: 1 │ │ │ │ DELIVERY NOTES           │ │   │
│   │ │ Unit Price: 7.630.000 ₫   │ │ │ │ Handle with care         │ │   │
│   │ │ Subtotal: 7.630.000 ₫     │ │ │ └──────────────────────────┘ │   │
│   │ └───────────────────────────┘ │ │ ┌──────────────────────────┐ │   │
│   │                               │ │ │ PAYMENT                  │ │   │
│   │                               │ │ │ Transaction ID           │ │   │
│   │                               │ │ │ TXN-9843-2026            │ │   │
│   │                               │ │ └──────────────────────────┘ │   │
│   └───────────────────────────────┘ └──────────────────────────────┘   │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ 4. FOOTER (Shared 4-Column Customer Architecture)                      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Detailed Layout & Styling Specifications [UI DESIGN DECISION]

### 8.1 Desktop Grid (&ge; 1024px)
*   **Max Width:** `max-w-[1100px] mx-auto px-8 py-10`.
*   **Breadcrumbs:** `JetBrains Mono text-[11px] text-[#78716C] tracking-widest uppercase mb-4`.
*   **Header Bar:** `flex justify-between items-start pb-6 border-b border-[#E7E5E4] mb-8`.
    *   Title: `Plus Jakarta Sans text-3xl font-extrabold text-[#1C1917] tracking-tight`.
    *   Meta timestamp: `JetBrains Mono text-xs text-[#78716C] mt-1`.
*   **Left Column (col-span-8):**
    *   Section Label: `JetBrains Mono 11px uppercase tracking-wider text-[#78716C] mb-3`.
    *   Card Container: `bg-[#FFFFFF] border border-[#E7E5E4] rounded-[6px] overflow-hidden shadow-none`.
    *   Line Items: Clean typographic rows with `p-6 border-b border-[#E7E5E4] last:border-b-0 flex justify-between items-start`.
    *   Title: `Plus Jakarta Sans 15px font-semibold text-[#1C1917]`.
    *   Codes: `JetBrains Mono 12px text-[#78716C]`.
    *   Subtotal: `JetBrains Mono 15px font-bold text-[#1C1917]`.
*   **Right Column (col-span-4):**
    *   Stacked cards (`space-y-6`), each in `bg-[#FFFFFF] border border-[#E7E5E4] rounded-[6px] p-6 shadow-none`.
    *   Card 1 (`ORDER TOTAL`): `JetBrains Mono 11px uppercase tracking-wider text-[#78716C] mb-3`. Total: `JetBrains Mono 24px font-bold text-[#1C1917] tracking-tight`.
    *   Card 2 (`SHIPPING ADDRESS` & `DELIVERY NOTES`): High-legibility prose (`Plus Jakarta Sans 14px text-[#1C1917]`).
    *   Card 3 (`PAYMENT`):
        *   Card Header: `PAYMENT` (`JetBrains Mono 11px uppercase tracking-wider text-[#78716C] mb-2`).
        *   Label: `Transaction ID` (`JetBrains Mono 10px uppercase tracking-wider text-[#78716C] mb-0.5`).
        *   Value: Monospace tabular code `TXN-9843-2026` (`JetBrains Mono 13px font-bold text-[#1C1917]`).
        *   Strictly zero mentions of payment service verification or technical infrastructure.

### 8.2 Mobile Responsive Layout (< 768px)
*   **Container:** Collapses into a single column (`px-4 py-6`).
*   **Order Header:** Stacks vertically; `CANCEL ORDER` button becomes full-width with a **44px minimum touch target**.
*   **Ordered Items:** Stacks product title above unit price and subtotal; no horizontal table scrolling.
*   **Summary & Specifications:** Reordered directly below the ordered items list.

---

## 9. State Specifications & Error Handling [UI DESIGN DECISION]

| State | Trigger | Presentation & Behavioral Treatment |
| :--- | :--- | :--- |
| **Loading** | Initial API fetch in progress | Restrained skeleton blocks: Header title placeholder and two-column skeleton cards with `bg-[#E7E5E4] animate-pulse rounded-[4px]`. No full-screen spinning wheels. |
| **Success** | HTTP 200 from `GET /api/v1/orders/{id}` | Full rendered document (Sections 7 & 8). |
| **404 Not Found** | HTTP 404 (`OrderNotFoundException`) | Centered architectural card: `ORDER NOT FOUND`. Copy: *"The requested order record does not exist or has been archived."* CTA: Machined button `BACK TO ORDERS` (`href="/orders"`). |
| **403 Forbidden** | HTTP 403 (`ApiException: Access denied to order {id}`) | Centered architectural card: `ACCESS DENIED`. Copy: *"You do not have permission to view this order record."* CTA: Machined button `BACK TO ORDERS` (`href="/orders"`). |
| **401 Unauthorized**| HTTP 401 (Missing/Expired JWT) | Redirect to `/login?redirect=/orders/{id}`. |
| **Network / 500 Error** | Connection loss / internal gateway failure | Centered error card: `UNABLE TO LOAD ORDER RECORD`. Copy: *"A network or server interruption occurred."* CTA: `RETRY` (re-triggers fetch). |

---

## 10. Motion & Micro-interactions [UI DESIGN DECISION]

Following `ui-ux-pro-max` dial settings:
*   `DESIGN_VARIANCE: 7`
*   `MOTION_INTENSITY: 3` (Restrained, functional, non-distracting)
*   `VISUAL_DENSITY: 3` (Architectural, airy document feel)
*   **Approved Animations:**
    *   Document mount: Subtle fade-in (`opacity: 0 &rarr; 1`, `translateY: 4px &rarr; 0px`, `200ms ease-out`).
    *   Button hover: Border and background shift (`150ms ease`).
*   **Strictly Prohibited:** Celebratory confetti, full-screen loaders, animated progress bars, decorative 3D perspective shifts.

---

## 11. Stitch Screen Asset & Metadata

*   **Project ID:** `113306237264437644`
*   **Screen ID:** `50c596f075084ade9ae301896a10e7d6`
*   **Screen Title:** `TECHGEAR — Order Detail`
*   **Active Design System:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
*   **Device Type:** `DESKTOP` (`2560 x 2252`)
*   **Screenshot URL:**  
    `https://lh3.googleusercontent.com/aida/AEtjO1XY6Jss6n4-D0TmSTAK3QnVpPlXVUde38YfLOoGi1fMegQlPcgfvmx841gqI-iib_s8NxtNfdUsJZd8SaVVRDivyFwL3MRawlRDIqtF0_eydf4T9b0C6uxta6NuC2J3j96dwDUCvfpaNIR148fWRGrF79fre5ZtoNgNty8ZwEVvFMeuk0iEOaZlHWfm-vYuPYr7BqRfUeXKWF47PmlwI33kGUgdxLYsFMCeCRIGP9sJS8wSLlyoxg9HNw`

---

## 12. Final Verification Checklist

- [x] **GET order endpoint verified:** `GET /api/v1/orders/{id}` confirmed in `OrderController.java:112-120`.
- [x] **Authentication verified:** Requires `Authorization: Bearer <JWT>` via `OrderSecurityConfig.java:49`.
- [x] **Ownership/IDOR verified:** Enforces `principal.userId().equals(order.getUserId())` or 403 Forbidden.
- [x] **DTO fields verified:** Exact fields of `OrderDto` and `OrderItemDto` verified from source code.
- [x] **OrderStatus verified:** 9 statuses checked against `OrderStatus.java`.
- [x] **Cancel capability verified:** Allowed only when `PENDING` or `CONFIRMED` via `POST /api/v1/orders/{id}/cancel`.
- [x] **Refund capability verified:** Verified as strictly admin-only in `payment-service` (`hasRole("ADMIN")`); customer refund actions omitted.
- [x] **No fake backend fields:** No tax, shipping fee, carrier, tracking number, delivery estimate, or discount fields.
- [x] **No unsupported ecommerce features:** No review ratings, wishlists, or recommendations.
- [x] **No new backend work:** Strictly frontend specification and design.
- [x] **Stitch uses Obsidian & Paper:** Reused `assets/592f25a1d5524b0091e02d25cfc6dd89`.
- [x] **Exactly 1 canonical FE-09 screen:** Screen `50c596f075084ade9ae301896a10e7d6` generated and cleaned.
- [x] **Documentation created:** `frontend/docs/FE-09-ORDER-DETAIL-DESIGN.md` with explicit `BACKEND-CONFIRMED` vs `UI DESIGN DECISION` markings.
- [x] **Existing FE-01 &rarr; FE-08 not modified:** Preserved intact.
- [x] **No Git commit / push:** Working tree clean.
