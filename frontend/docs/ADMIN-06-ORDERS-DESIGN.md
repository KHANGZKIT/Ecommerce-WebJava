# TECHGEAR — ADMIN-06 Admin Orders Operations Design Specification

> **Document ID:** ADMIN-06-ORDERS-DESIGN  
> **Route:** `/admin/orders`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `291d3261062944d080d460cfd5b8b453`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [ADMIN-02](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-02-DASHBOARD-DESIGN.md), [ADMIN-05](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-05-USERS-DESIGN.md), [FE-08](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-08-MY-ORDERS-DESIGN.md), [FE-09](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-09-ORDER-DETAIL-DESIGN.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md)  
> **Backend Authority:** Spring Boot 3.2 `order-service`, `payment-service` & `api-gateway`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Core Architectural Reality

The **Admin Orders** screen (`/admin/orders`) provides targeted operational lookup, status transitions, and cancellation controls for TECHGEAR administrators. 

```
┌────────────────────────────────────────────────────────────────────────┐
│               ARCHITECTURAL REALITY: NO UNBOUNDED ORDER LIST           │
│                                                                        │
│   The backend exposes NO global "GET /api/v1/orders" listing endpoint. │
│   All admin operations are executed through two targeted vectors:      │
│                                                                        │
│   1. VECTOR A: DIRECT ORDER LOOKUP      GET /api/v1/orders/{id}        │
│   2. VECTOR B: USER ORDER HISTORY       GET /api/v1/orders/user/{id}   │
└────────────────────────────────────────────────────────────────────────┘
```

### Strict YAGNI & Anti-Slop Boundaries
* **Core Capabilities Supported (`BACKEND-CONFIRMED`):**
  1. **LOOKUP BY ORDER ID:** Fetch a single order by its numeric ID (`GET /api/v1/orders/{id}`). Admin bypasses customer ownership check.
  2. **LOOKUP BY USER ID:** Fetch paginated order history for any registered user (`GET /api/v1/orders/user/{userId}?page={p}&size={s}&sort=createdAt`). Admin bypasses customer ownership check.
  3. **UPDATE ORDER STATUS:** Transition an order's status to any valid backend enum value (`PATCH /api/v1/orders/{id}/status?status={newStatus}`). Restricted strictly to administrators.
  4. **CANCEL ORDER:** Cancel eligible pending orders (`POST /api/v1/orders/{id}/cancel`). Allowed **strictly** when order is in `PENDING` or `CONFIRMED` status.
  5. **PAYMENT CROSS-INSPECTION LINK:** If `paymentTransactionId` is present, provide a direct link to the payment transaction inspection screen (`/admin/payments?orderId={id}`).
* **Critical Absence of Global Order Listing (`BACKEND-CONFIRMED`):**
  * `OrderController` does **NOT** expose `GET /api/v1/orders`.
  * `OrderRepository` does **NOT** have an unbounded `findAll(Pageable)` method exposed through any controller.
  * **Strict Exclusion:** The UI **never** renders a fake global order table, fake global pagination ("Showing 1–20 of 500 orders"), a fake recent orders feed, or simulated order analytics cards.
* **Explicit Scope Exclusions (`SCOPE LIMITATION`):**
  * **No Shipping / Carrier Tracking:** No courier selection, tracking numbers, waybill labels, or shipping webhooks.
  * **No Tax / Shipping Fee Rows:** Backend `OrderDto` has no `tax` or `shippingFee` fields.
  * **No Invoice Generator / PDF Export:** No VAT invoice generation or PDF download buttons.
  * **No Manual Line-Item Editing:** Admins cannot add, remove, or modify quantities on existing order items.
  * **No Bulk Status Updates / Bulk Cancel:** Operations apply strictly per individual order.
  * **No Order Analytics / Charts:** No revenue graphs, average order value cards, or daily volume metrics.
  * **No Customer Communications:** No email resend, SMS dispatch, or notification triggers.

---

## 2. Route & Access Control

* **Canonical Route:** `/admin/orders`
* **Query Parameters Supported (URL-Driven Lookup):**
  * `/admin/orders?orderId={id}`: Automatically executes Direct Order Lookup for the specified order ID.
  * `/admin/orders?userId={userId}`: Automatically executes Customer Order History Lookup for the specified user ID (e.g. arriving from `/admin/users` via `[ VIEW USER ORDERS ]`).
* **Route Type:** Protected Administrative Route.
* **Actor & Permissions Matrix:**

| Actor | Action / Method | Behavior & UX Response | Enforcement Layer | Classification |
| :--- | :--- | :--- | :--- | :--- |
| **Guest (Unauthenticated)** | Any | Intercepted by auth guard; navigates to `/login` | `FE-15 Auth Guard` | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Any | Renders inline **Access Denied** vitrine; does **NOT** log out | Client Route Guard | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Direct API call | HTTP 403 Forbidden | `OrderSecurityConfig.java:48` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /orders/{id}` | HTTP 200 OK — ownership check **bypassed** | `OrderController.java:114-118` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /orders/user/{userId}`| HTTP 200 OK — ownership check **bypassed**; returns `Page<OrderDto>` | `OrderController.java:124-129` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `PATCH /orders/{id}/status`| HTTP 200 OK — updates status; returns updated `OrderDto` | `OrderController.java:146-154` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `POST /orders/{id}/cancel` | HTTP 200 OK — cancels eligible order; returns updated `OrderDto` | `OrderController.java:134-142` | `BACKEND-CONFIRMED` |

### Security & Ownership Bypass Verification (`BACKEND-CONFIRMED`)
In `OrderController.java:114-118`:
```java
OrderPrincipal principal = OrderSecurityUtils.getAuthenticatedPrincipal();
OrderDto order = orderService.getOrderById(id);
if (!principal.isAdmin() && !Long.valueOf(principal.userId()).equals(order.getUserId())) {
    throw new ApiException("Access denied to order " + id, HttpStatus.FORBIDDEN);
}
```
* **Verification:** When `principal.isAdmin()` is `true`, the condition `!principal.isAdmin()` evaluates to `false`. The ownership check is completely bypassed, permitting administrators to inspect and act on any customer's order.
* **Verified in Tests:** Tested in `OrderSecurityBoundaryTest.java:534-543` (`admin_canAccessAnyOrder_returns200`), `569-579` (`admin_canAccessAnyUserOrderHistory_returns200`), and `614-625` (`admin_canUpdateOrderStatus_returns200`).

---

## 3. Backend Verification & Integration Contracts

### 3.1 Verified Source Code References

| Capability | Backend File & Line | Method & Signature | Response Type | Verified Implementation Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **Get Order by ID** | `OrderController.java:112-120` | `getOrderById(@PathVariable Long id)` | `OrderDto` | Calls `orderService.getOrderById(id)`. Admin bypasses ownership. Throws 404 if absent. |
| **Get Orders by User** | `OrderController.java:122-131` | `getOrdersByUser(@PathVariable Long userId, @PageableDefault Pageable pageable)` | `Page<OrderDto>` | Calls `orderService.getOrdersByUserId(userId, pageable)`. Default `size=10, sort="createdAt"`. |
| **Update Order Status**| `OrderController.java:145-154` | `updateStatus(@PathVariable Long id, @RequestParam OrderStatus status)` | `OrderDto` | Requires `hasRole('ADMIN')`. Updates status to specified enum. Query parameter, not body. |
| **Cancel Order** | `OrderController.java:133-143` | `cancelOrder(@PathVariable Long id)` | `OrderDto` | Admin bypasses ownership. Throws 422 `OrderStatusException` if not `PENDING` or `CONFIRMED`. |
| **Security Config** | `OrderSecurityConfig.java:48-50` | `orderSecurityFilterChain()` | N/A | Enforces `PATCH /orders/*/status` has role `ADMIN`. All other endpoints require authentication. |
| **Error Handling** | `GlobalExceptionHandler.java:22-27` | `handleApiException()` | `ErrorResponse` | Maps `ApiException`, `OrderNotFoundException` (404), `OrderStatusException` (422). |

---

### 3.2 Request & Response Data Contracts

#### A. Direct Order Lookup Contract (`GET /api/v1/orders/{id}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/orders/{id}`
* **Path Parameter:** `id: Long` (e.g. `105`)
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Response Status:** `200 OK`
* **Response Payload Schema (`OrderDto`):**
  ```json
  {
    "id": 105,
    "userId": 2,
    "items": [
      {
        "id": 201,
        "productId": 1,
        "productName": "Keychron Q1 Pro Wireless Custom Mechanical Keyboard",
        "productSku": "KEY-Q1P-BLK",
        "unitPrice": 4850000.00,
        "quantity": 1,
        "subtotal": 4850000.00
      },
      {
        "id": 202,
        "productId": 5,
        "productName": "Master & Dynamic MH40 Wireless ANC Headphones",
        "productSku": "MND-MH40-SLV",
        "unitPrice": 7630000.00,
        "quantity": 1,
        "subtotal": 7630000.00
      }
    ],
    "totalAmount": 12480000.00,
    "status": "CONFIRMED",
    "shippingAddress": "123 Ba Trieu, Hoan Kiem, Hanoi",
    "paymentTransactionId": "TXN-9843-2026",
    "notes": "Handle with care / Call before delivery",
    "createdAt": "2026-10-07T10:15:30",
    "updatedAt": "2026-10-07T10:15:35"
  }
  ```

#### B. User Order History Lookup Contract (`GET /api/v1/orders/user/{userId}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/orders/user/{userId}`
* **Path Parameter:** `userId: Long` (e.g. `2`)
* **Query Parameters Supported (`BACKEND-CONFIRMED`):**
  * `page: Integer` (0-indexed, default `0`)
  * `size: Integer` (default `10`)
  * `sort: String` (default `createdAt,desc`)
* **Response Status:** `200 OK`
* **Response Payload Schema (`Page<OrderDto>`):**
  ```json
  {
    "content": [
      {
        "id": 105,
        "userId": 2,
        "items": [ /* OrderItemDto array */ ],
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
    "numberOfElements": 1,
    "first": true,
    "empty": false
  }
  ```

#### C. Update Order Status Contract (`PATCH /api/v1/orders/{id}/status`)
* **HTTP Method:** `PATCH`
* **Path:** `/api/v1/orders/{id}/status`
* **Path Parameter:** `id: Long`
* **Query Parameter:** `status: OrderStatus` (e.g. `?status=SHIPPED`) — **NOT a request body**!
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Response Status:** `200 OK`
* **Response Payload:** Updated `OrderDto`.

#### D. Cancel Order Contract (`POST /api/v1/orders/{id}/cancel`)
* **HTTP Method:** `POST`
* **Path:** `/api/v1/orders/{id}/cancel`
* **Path Parameter:** `id: Long`
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Request Body:** None.
* **Response Status:** `200 OK`
* **Response Payload:** Updated `OrderDto` with `"status": "CANCELLED"`.
* **Error Response (Illegal Cancellation - 422):**
  ```json
  {
    "status": 422,
    "message": "Cannot cancel order in status: SHIPPED",
    "timestamp": "2026-10-08T19:15:00.000"
  }
  ```

---

### 3.3 Field-by-Field Backend Verification Matrix

All rendered fields are audited directly from `OrderDto.java` and `OrderItemDto.java`:

| DTO Field | Java Type | Nullable? | Exact Backend Meaning | UI Presentation & Treatment |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `Long` | No | Primary key ID in `orders` table | `ORDER #105` (Plus Jakarta Sans `22px` Bold) |
| `userId` | `Long` | No | Customer owner foreign key ID | `User ID: #02` (clickable shortcut &rarr; `/admin/users`) |
| `status` | `OrderStatus` | No | Current lifecycle status enum | Badge with exact color token from design system |
| `totalAmount` | `BigDecimal` | No | Final confirmed currency amount | JetBrains Mono `16px` Bold (e.g. `12,480,000 VND`) |
| `shippingAddress` | `String` | No | Destination delivery address | Text string (`13px` `#57534E`) |
| `paymentTransactionId` | `String` | Yes | Payment gateway transaction code | JetBrains Mono (`TXN-9843-2026`) + Link to `/admin/payments` |
| `notes` | `String` | Yes | Customer instruction or delivery memo | Italic text or `—` if empty |
| `createdAt` | `LocalDateTime` | No | Order creation timestamp | Formatted `YYYY-MM-DD HH:mm:ss` (JetBrains Mono `12px`) |
| `updatedAt` | `LocalDateTime` | No | Last modification timestamp | Formatted `YYYY-MM-DD HH:mm:ss` (JetBrains Mono `12px`) |
| **Item Fields** | | | | |
| `items[].id` | `Long` | No | Primary key ID of order item line | Internal row key |
| `items[].productId` | `Long` | No | Product foreign key ID | Product reference ID |
| `items[].productName` | `String` | No | Snapshot of product name at checkout | Primary line title (Plus Jakarta Sans `13px` Bold) |
| `items[].productSku` | `String` | No | Snapshot of product SKU code | JetBrains Mono `12px` text `#78716C` |
| `items[].unitPrice` | `BigDecimal` | No | Snapshot price per unit at checkout | JetBrains Mono `13px` aligned right |
| `items[].quantity` | `Integer` | No | Number of units purchased | JetBrains Mono `13px` aligned center |
| `items[].subtotal` | `BigDecimal` | No | `unitPrice × quantity` | JetBrains Mono `13px` Bold aligned right |

#### Strictly Forbidden Fields (`SCOPE LIMITATION`)
The following fields **DO NOT EXIST** in `OrderDto` or `OrderItemDto` and must **NEVER** be displayed or simulated:
* ❌ `tax` / `vatRate` / `vatAmount`
* ❌ `shippingFee` / `deliveryCharge`
* ❌ `discount` / `couponCode` / `promoApplied`
* ❌ `trackingNumber` / `carrier` / `courierName`
* ❌ `estimatedDeliveryDate` / `deliveryWindow`
* ❌ `invoiceNumber` / `downloadInvoicePdf`
* ❌ `paymentCardLast4` / `cardBrand` / `cvv`

---

## 4. Order Status & Lifecycle Management

### 4.1 Exact Backend `OrderStatus` Enum (`BACKEND-CONFIRMED`)
From `com.ecommerce.orderservice.model.OrderStatus`:
```java
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_UNKNOWN,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
```

* **Color & Visual Tokens Matrix:**

| Status Enum Value | Visual Badge Style | Semantic Meaning in Flow | UI Actions Available |
| :--- | :--- | :--- | :--- |
| `PENDING` | `#B45309` on `#FFFBEB` (Amber) | Order created, stock held, payment pending | Can update status, **Can Cancel** |
| `CONFIRMED` | `#15803D` on `#F0FDF4` (Green) | Payment confirmed, order approved | Can update status, **Can Cancel** |
| `PAYMENT_FAILED` | `#DC2626` on `#FEF2F2` (Red) | Payment failed, stock released | Can update status |
| `PAYMENT_UNKNOWN` | `#78716C` on `#F5F5F4` (Gray) | Payment response unresolvable | Can update status |
| `PROCESSING` | `#1D4ED8` on `#EFF6FF` (Blue) | Warehouse packing / preparation | Can update status (Cannot cancel) |
| `SHIPPED` | `#4338CA` on `#EEF2FF` (Indigo) | In transit to customer | Can update status (Cannot cancel) |
| `DELIVERED` | `#15803D` on `#F0FDF4` (Green) | Handed over to recipient | Can update status (Cannot cancel) |
| `CANCELLED` | `#78716C` on `#F5F5F4` (Muted) | Order cancelled; closed | Can update status (Cannot cancel) |
| `REFUNDED` | `#9333EA` on `#FAF5FF` (Purple)| Transaction refunded; closed | Can update status (Cannot cancel) |

* **Forbidden Status Values:** No `PACKED`, `OUT_FOR_DELIVERY`, `DELIVERY_FAILED`, or `ON_HOLD` exist in the backend.

---

### 4.2 Status Update Semantics (`PATCH /api/v1/orders/{id}/status`)
* **Mechanism:**
  * Administrator selects a new target status from the dropdown containing all 9 valid `OrderStatus` values.
  * Clicking `[ UPDATE STATUS ]` displays a compact confirmation popover:
    `"Change Order #{id} status from {currentStatus} to {newStatus}?"`
  * Upon confirmation, triggers `PATCH /api/v1/orders/{id}/status?status={newStatus}`.
  * **No Optimistic Update:** The UI awaits successful HTTP 200 response before reflecting the updated badge and refreshed order detail.

---

### 4.3 Cancellation Semantics (`POST /api/v1/orders/{id}/cancel`)
* **Strict Backend Constraint:**
  ```java
  if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
      throw new OrderStatusException("Cannot cancel order in status: " + order.getStatus());
  }
  ```
* **UI Enforcement Rule (`UI DESIGN DECISION`):**
  * The `[ CANCEL ORDER ]` button is **active and clickable ONLY** when `status === 'PENDING'` or `status === 'CONFIRMED'`.
  * For all other statuses (`PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`, etc.), the button is rendered in a **disabled state** (`opacity: 0.5; cursor: not-allowed; title: "Cancellation is only permitted for PENDING or CONFIRMED orders"`).
  * This prevents triggering HTTP 422 `OrderStatusException` errors.
* **Confirmation Dialog:**
  ```
  ┌────────────────────────────────────────────────────────┐
  │ CANCEL ORDER #105?                                 [✕] │
  ├────────────────────────────────────────────────────────┤
  │ Are you sure you want to cancel this order?            │
  │ Current Status: CONFIRMED                              │
  │                                                        │
  │ This will transition the order to CANCELLED.           │
  ├────────────────────────────────────────────────────────┤
  │ [ KEEP ORDER ]                       [ CANCEL ORDER ]  │
  └────────────────────────────────────────────────────────┘
  ```

---

## 5. UI Architecture & Dual-Lookup Workflow

The `/admin/orders` screen follows a clear, two-stage structure:
1. **Upper Tier:** Operational Dual-Lookup Console.
2. **Lower Tier:** Dynamic Inspection Result Area.

```
┌────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR // ADMIN                                     admin@techgear.io│
├────────────────────────────────────────────────────────────────────────┤
│ DASHBOARD    PRODUCTS    CATEGORIES    USERS    [ ORDERS ]    PAYMENTS │
├────────────────────────────────────────────────────────────────────────┤
│ ADMIN / ORDERS                                                         │
│ ORDER OPERATIONS                                                       │
│ [ LOOKUP-BASED ARCHITECTURE — NO UNBOUNDED ORDER LIST ]                │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  OPERATIONAL LOOKUP CONSOLE                                            │
│  ┌──────────────────────────────────┬────────────────────────────────┐ │
│  │ VECTOR A: DIRECT ORDER LOOKUP    │ VECTOR B: CUSTOMER ORDER LOOKUP│ │
│  │ ORDER ID: [ 105                ] │ USER ID: [ e.g. 2            ] │ │
│  │ [ INSPECT ORDER ]                │ [ VIEW USER ORDERS ]           │ │
│  └──────────────────────────────────┴────────────────────────────────┘ │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  DYNAMIC RESULT AREA (ONE OF 3 STATES):                                │
│                                                                        │
│  STATE 1: INITIAL / EMPTY (Prompt to enter Order ID or User ID)        │
│  STATE 2: SINGLE ORDER INSPECTION PANEL (When Order ID is queried)     │
│  STATE 3: USER ORDERS PAGINATED TABLE (When User ID is queried)        │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 5.1 Lookup Vector A: Direct Order Lookup (`GET /orders/{id}`)
* **Input Field:** `ORDER ID`
* **Validation:**
  * Must be a positive integer (`^[1-9][0-9]*$`).
  * If empty or non-numeric: Highlights field with red border and displays inline error `"Please enter a valid numeric Order ID"`. API call is blocked.
* **Execution:** Calls `GET /api/v1/orders/{id}`.
* **Result Display:** Renders the **Inspected Order Operations Panel** (Section 6).

---

### 5.2 Lookup Vector B: Customer Order History Lookup (`GET /orders/user/{userId}`)
* **Input Field:** `USER ID`
* **Validation:**
  * Must be a positive integer (`^[1-9][0-9]*$`).
  * If empty or non-numeric: Displays inline error `"Please enter a valid numeric User ID"`. API call is blocked.
* **Execution:** Calls `GET /api/v1/orders/user/{userId}?page={page}&size=10&sort=createdAt,desc`.
* **Result Display:** Renders the **User Orders Paginated Table**:
  ```
  ┌────────────────────────────────────────────────────────────────────────┐
  │ ORDER HISTORY FOR USER #02 · 3 ORDERS FOUND              Page 1 of 1   │
  ├──────┬──────────────┬──────────────┬───────────┬──────────────┬────────┤
  │ ID   │ DATE         │ TOTAL AMOUNT │ STATUS    │ PAYMENT TXN  │ ACTION │
  ├──────┼──────────────┼──────────────┼───────────┼──────────────┼────────┤
  │ #105 │ 2026-10-07   │ 12,480,000 đ │ CONFIRMED │ TXN-9843-2026│[INSPECT]│
  │ #091 │ 2026-09-15   │  4,850,000 đ │ DELIVERED │ TXN-7712-2026│[INSPECT]│
  │ #042 │ 2026-08-01   │  1,650,000 đ │ CANCELLED │ —            │[INSPECT]│
  ├──────┴──────────────┴──────────────┴───────────┴──────────────┴────────┤
  │ [ PREVIOUS ]                                                  [ NEXT ] │
  └────────────────────────────────────────────────────────────────────────┘
  ```
* **Drill-Down Action:** Clicking `[ INSPECT ]` on any row loads that specific order into the Direct Order Inspection view (`/admin/orders?orderId={row.id}`).

---

## 6. Inspected Order Operations Panel (Single Order Detail)

When an order is successfully retrieved, the Result Area displays the full, backend-grounded order inspection panel:

### 6.1 Action & Status Header
* **Order Number:** `ORDER #105` (Plus Jakarta Sans `22px` font-bold `#1C1917`).
* **Current Status Badge:** Displays current enum badge with exact token styling (e.g. `CONFIRMED` in `#15803D` green).
* **Operational Control Group:**
  * `SET STATUS:` Dropdown with all 9 backend enum values + `[ UPDATE STATUS ]` button.
  * `[ CANCEL ORDER ]` destructive button (active if `PENDING` or `CONFIRMED`, disabled otherwise).

### 6.2 4-Column Technical Specification Grid
Bounded in milled `#FAFAF9` container with `1px solid #E7E5E4` border:

```
┌──────────────────────────┬──────────────────────────┬──────────────────────────┬──────────────────────────┐
│ CUSTOMER ENTITY          │ TOTAL AMOUNT             │ PAYMENT TRANSACTION      │ ORDER NOTES              │
│ User ID #02              │ 12,480,000 VND           │ TXN-9843-2026            │ Handle with care / Call  │
│ (Elena Rostova)          │                          │ [ INSPECT PAYMENT → ]    │ before delivery          │
│ ──────────────────────── │ ──────────────────────── │ ──────────────────────── │ ──────────────────────── │
│ PLACED AT                │ LAST MODIFIED            │ SHIPPING DESTINATION     │ STATUS TIMESTAMPS        │
│ 2026-10-07 10:15:30 UTC  │ 2026-10-07 10:15:35 UTC  │ 123 Ba Trieu, Hoan Kiem, │ Audited Spring entity    │
│                          │                          │ Hanoi, Vietnam           │ listeners                │
└──────────────────────────┴──────────────────────────┴──────────────────────────┴──────────────────────────┘
```

### 6.3 Itemized Order Contents Table
High-density tabular specification of items purchased:

* **Headers:** `PRODUCT NAME` &bull; `SKU` &bull; `UNIT PRICE` &bull; `QTY` &bull; `SUBTOTAL` (JetBrains Mono `11px` uppercase stone neutral `#78716C`).
* **Row Elements:**
  * Product Name: Plus Jakarta Sans `13px` Semi-bold `#1C1917`.
  * SKU: JetBrains Mono `12px` `#78716C` (e.g. `KEY-Q1P-BLK`).
  * Unit Price: JetBrains Mono `13px` tabular figures aligned right.
  * Quantity: JetBrains Mono `13px` centered.
  * Subtotal: JetBrains Mono `13px` Semi-bold aligned right.
* **Table Summary Footer:**
  * Left: `TOTAL ITEMS: 2 (2 UNITS)` (JetBrains Mono `11px` uppercase).
  * Right: `ORDER TOTAL: 12,480,000 VND` (JetBrains Mono `16px` Bold `#1C1917`).
  * **Strict Constraint:** Zero artificial tax, shipping fee, or discount rows.

---

## 7. Payment Module Integration Contract

* **Payment Reference:**
  * If `order.paymentTransactionId` is present:
    * Displays the transaction code (e.g. `TXN-9843-2026`).
    * Displays clickable shortcut link: `[ INSPECT PAYMENT → ]`.
    * Route Destination: `/admin/payments?orderId={order.id}` (or by payment ID if available).
  * If `order.paymentTransactionId` is `null`:
    * Displays em-dash `—` and muted text `NO TRANSACTION RECORDED (Order unpaid or cancelled)`.
* **Zero Payment Widgets in Orders:**
  * Credit card inputs, CVV fields, payment processor badges, or refund action forms are **strictly omitted** from `/admin/orders`. All refund capabilities reside exclusively inside `/admin/payments` (ADMIN-07).

---

## 8. Error Handling & State Machine

```
┌─────────────────┬────────────────────────────────────────────────────────────┐
│ State           │ UX Presentation & Component Behavior                       │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 1. Initial      │ Lookup Console rendered; Result Area prompts:              │
│                 │ "ENTER AN ORDER ID OR USER ID TO RETRIEVE RECORDS."       │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 2. Loading      │ Shimmer skeleton matching 4-column spec grid and           │
│                 │ 2-row item table; inputs disabled during flight.           │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 3. 404 Not Found│ "ORDER NOT FOUND: No record found for ID #{id}."          │
│                 │ Action button: [ CLEAR LOOKUP ] resets focus to input.     │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 4. User Empty   │ "NO ORDERS FOUND: User #{userId} has placed zero orders." │
│                 │ Clean vitrine card with neutral border.                    │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 5. 422 Conflict │ Toast message: "Cannot cancel order in status: {status}."  │
│                 │ Triggers automatic re-fetch to sync fresh backend status.  │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 6. 403 Forbidden│ Inline "ACCESS DENIED" vitrine; active session preserved.   │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 7. 500 Network  │ "AN UNEXPECTED SERVICE ERROR OCCURRED." [ RETRY ] button.  │
└─────────────────┴────────────────────────────────────────────────────────────┘
```

---

## 9. Visual System & Design Tokens

Adhering strictly to **Obsidian & Paper** guidelines:

| Visual Element | Design Token | Usage in ADMIN-06 |
| :--- | :--- | :--- |
| **Page Canvas** | `#FAFAF9` (`--color-bg-canvas`) | Primary background |
| **Surface Base** | `#FFFFFF` (`--color-surface-base`) | Lookup console card, order inspection card |
| **Surface Raised**| `#F5F5F4` (`--color-surface-raised`) | Metadata spec grid, table header, footer bar |
| **Primary Text** | `#1C1917` (`--color-text-primary`) | Headings, order ID, product titles, total amount |
| **Secondary Text**| `#57534E` (`--color-text-secondary`) | Field labels, addresses, timestamps, table text |
| **Muted Metadata**| `#78716C` (`--color-text-tertiary`) | SKUs, unit descriptors, breadcrumbs |
| **Borders** | `1px solid #E7E5E4` | Hairline dividers, card framing, input borders |
| **Accent Signal** | `#D97706` (Signal Amber) | Interactive link `[ INSPECT PAYMENT → ]`, focus rings |
| **Destructive** | `#DC2626` / `#FEF2F2` bg | `[ CANCEL ORDER ]` button and danger dialogs |
| **Success Status**| `#15803D` / `#F0FDF4` bg | `CONFIRMED` / `DELIVERED` status badges |
| **Typography** | Plus Jakarta Sans & JetBrains Mono | Geometric grotesque headlines + monospace telemetry |
| **Border Radius**| `4px` controls, `6px` cards | Cold-rolled industrial instrument feel |

---

## 10. Responsive Architecture

### 10.1 Desktop (&ge; 1280px)
* Dual-lookup console displayed in a side-by-side 2-column layout.
* Inspected order metadata rendered in a 4-column horizontal spec grid.
* Item contents rendered in a full 5-column tabular matrix.

### 10.2 Tablet (768px – 1279px)
* Dual-lookup console stacks into 2 rows with full-width inputs.
* Metadata grid shifts to a 2-column &times; 2-row layout.
* Item table retains all columns with reduced horizontal padding.

### 10.3 Mobile (< 768px)
* Lookup inputs and action buttons stack vertically.
* Metadata grid collapses to a single-column key-value stack.
* Item table transforms into stacked product line cards:
  ```
  ┌────────────────────────────────────────────────────────┐
  │ KEYCHRON Q1 PRO WIRELESS KEYBOARD                      │
  │ SKU: KEY-Q1P-BLK                 Qty: 1                │
  │ Unit: 4,850,000 VND              Subtotal: 4,850,000 đ │
  └────────────────────────────────────────────────────────┘
  ```
* All touch targets meet or exceed `44px × 44px`.

---

## 11. Canonical Stitch Screen Reference

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `291d3261062944d080d460cfd5b8b453`
* **Title:** `TECHGEAR — Admin Orders`
* **Device Type:** `DESKTOP`
* **Design System Asset:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (Obsidian & Paper)
* **Screen Features Captured:**
  1. 64px sticky administrative top navigation (`TECHGEAR // ADMIN`, nav rail with active `ORDERS` underline indicator, identity pill `admin@techgear.io [ADMIN]`, and `LOG OUT` trigger).
  2. Page header with breadcrumb `ADMIN / ORDERS`, title `ORDER OPERATIONS`, and architectural disclosure pill `LOOKUP-BASED ARCHITECTURE — NO UNBOUNDED ORDER LIST`.
  3. Dual Lookup Console showing Direct Order Lookup input with filled value `105` + `[ INSPECT ORDER ]` action, and Customer Order History Lookup input + `[ VIEW USER ORDERS ]` trigger.
  4. Active inspected order panel for `ORDER #105`:
     - Operational header with green `CONFIRMED` badge, status dropdown, `[ UPDATE STATUS ]` button, and active `[ CANCEL ORDER ]` button.
     - 4-column milled spec grid with verified Customer (`User ID #02 Elena Rostova`), Total Amount (`12,480,000 VND`), Payment Txn (`TXN-9843-2026`) with link, Shipping Address (`123 Ba Trieu, Hoan Kiem, Hanoi`), and Delivery Notes.
     - Itemized table with Keychron Q1 Pro and Master & Dynamic MH40, exact VND pricing, SKUs, and consolidated total footer.
     - Administrative telemetry footer (`OpenAPI 3.1`, `GET /api/v1/orders/{id}`).

---

## 12. Scope Classification Summary Table

| Category / Capability | Exact Backend / UI State | Classification Tag | Source Reference / Rationale |
| :--- | :--- | :--- | :--- |
| **Absence of GET All Orders**| No `GET /api/v1/orders` endpoint | `BACKEND-CONFIRMED` | Audited `OrderController.java` — zero unbounded list |
| **Direct Order Lookup** | `GET /api/v1/orders/{id}` | `BACKEND-CONFIRMED` | `OrderController.java:112-120` |
| **User Orders History** | `GET /api/v1/orders/user/{userId}` | `BACKEND-CONFIRMED` | `OrderController.java:122-131` |
| **Admin Ownership Bypass** | `principal.isAdmin()` bypasses check | `BACKEND-CONFIRMED` | `OrderController.java:116, 127, 136` |
| **Update Order Status** | `PATCH /api/v1/orders/{id}/status` | `BACKEND-CONFIRMED` | `OrderController.java:145-154` (Query param `status`) |
| **Cancel Order Endpoint** | `POST /api/v1/orders/{id}/cancel` | `BACKEND-CONFIRMED` | `OrderController.java:133-143` |
| **Cancellation Restriction** | Only allowed if `PENDING` or `CONFIRMED` | `BACKEND-CONFIRMED` | Throws 422 `OrderStatusException` (`OrderService:418`) |
| **Cancel Button Guard** | Disabled in UI if status not PENDING/CONFIRMED | `UI DESIGN DECISION` | Prevents unnecessary 422 error states |
| **OrderStatus Enum** | Exactly 9 enum values | `BACKEND-CONFIRMED` | `OrderStatus.java:3-13` |
| **OrderDto Fields** | `id, userId, items, totalAmount, status, ...` | `BACKEND-CONFIRMED` | `OrderDto.java:14-25` |
| **Payment Link** | Link to `/admin/payments?orderId={id}` | `UI DESIGN DECISION` | Cross-module navigation aligned with `ADMIN-01` |
| **Inspected Order #105** | Elena Rostova, 12,480,000 VND | `DESIGN MOCK DATA` | Representative seed order data |
| **Dual Lookup Console** | Side-by-side inputs for orderId & userId | `UI DESIGN DECISION` | Clean ergonomic separation of query vectors |
| **Forbidden Analytics/Carriers**| No charts, no tracking, no invoices | `SCOPE LIMITATION` | Strict YAGNI — zero backend support |

---

## 13. Final Audit Checklist

- [x] **GET order by ID verified:** Audited against `OrderController.java:112-120`.
- [x] **GET orders by user verified:** Audited against `OrderController.java:122-131`.
- [x] **No GET all orders assumed:** Verified that `GET /api/v1/orders` does not exist; no fake global table created.
- [x] **Request DTOs verified:** Status update uses `@RequestParam OrderStatus status`; Cancel has no body.
- [x] **Response DTOs verified:** Returns `OrderDto` with itemized `OrderItemDto` array.
- [x] **Pagination verified:** User order lookup uses `Page<OrderDto>` with `page, size, sort`.
- [x] **Sorting verified:** Standard Spring Data sort on `findByUserId`.
- [x] **Status enum verified:** Exactly 9 values (`PENDING`, `CONFIRMED`, `PAYMENT_FAILED`, `PAYMENT_UNKNOWN`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`).
- [x] **OrderStatus enum verified.** Admin status update accepts an OrderStatus value via `PATCH /api/v1/orders/{id}/status`. No frontend transition matrix is invented. Backend remains the authority.
- [x] **Cancel behavior verified:** Only permitted for `PENDING` and `CONFIRMED`; disabled on all other states to prevent 422.
- [x] **Admin authorization verified:** Guarded by `hasRole('ADMIN')` and `OrderSecurityUtils`.
- [x] **Ownership bypass verified:** Verified in source code and integration tests (`OrderSecurityBoundaryTest.java`).
- [x] **Payment relationship verified:** Displays `paymentTransactionId` with link to `/admin/payments?orderId={id}`.
- [x] **No fake global order table:** Entire interface built around targeted lookups.
- [x] **No fake filters:** No date, status, or customer-segment filter parameters exist on the audited order lookup endpoints.
- [x] **No shipping/invoice scope:** Zero couriers, tracking codes, or invoice PDFs simulated.
- [x] **Exactly 1 canonical Stitch screen:** Created screen `291d3261062944d080d460cfd5b8b453` in project `113306237264437644`.
- [x] **Documentation created:** Written to [ADMIN-06-ORDERS-DESIGN.md](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-06-ORDERS-DESIGN.md).
- [x] **Existing docs/screens unchanged:** `ADMIN-01` through `ADMIN-05` and `FE-01` through `FE-15` untouched.
- [x] **No code/package/backend/Git changes:** Zero implementation code or configuration modifications made.

---

**ADMIN-06 STATUS: READY FOR REVIEW**
