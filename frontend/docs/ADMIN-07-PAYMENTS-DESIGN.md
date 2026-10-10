# TECHGEAR — ADMIN-07 Admin Payments Operations Design Specification

> **Document ID:** ADMIN-07-PAYMENTS-DESIGN  
> **Route:** `/admin/payments`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `de81cb8973e144bd8e966a2707053689`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [ADMIN-06](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-06-ORDERS-DESIGN.md), [FE-07](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-07-PAYMENT-RESULT-DESIGN.md), [FE-09](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-09-ORDER-DETAIL-DESIGN.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md)  
> **Backend Authority:** Spring Boot 3.2 `payment-service` & `api-gateway`  
> **Status:** APPROVED & SPECIFIED — Final Admin Screen — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Core Architectural Reality

The **Admin Payments** screen (`/admin/payments`) provides targeted transaction lookup, financial status verification, and full payment refund processing for TECHGEAR administrators. This is the **final screen** of the administrative suite.

```
┌────────────────────────────────────────────────────────────────────────┐
│              ARCHITECTURAL REALITY: NO UNBOUNDED PAYMENT LIST          │
│                                                                        │
│   The backend exposes NO global "GET /api/v1/payments" endpoint.       │
│   All admin operations are executed through three targeted vectors:    │
│                                                                        │
│   1. VECTOR A: DIRECT PAYMENT LOOKUP   GET /api/v1/payments/{id}       │
│   2. VECTOR B: ORDER PAYMENTS QUERY    GET /api/v1/payments/order/{id} │
│   3. VECTOR C: USER PAYMENT LEDGER     GET /api/v1/payments/user/{id}  │
└────────────────────────────────────────────────────────────────────────┘
```

### Strict YAGNI & Anti-Slop Boundaries
* **Core Capabilities Supported (`BACKEND-CONFIRMED`):**
  1. **LOOKUP BY PAYMENT ID:** Query a single transaction record by numeric primary key (`GET /api/v1/payments/{id}`). Admin bypasses customer ownership check.
  2. **LOOKUP BY ORDER ID:** Query all payment attempts associated with a specific order (`GET /api/v1/payments/order/{orderId}`). Returns `List<PaymentResponse>`.
  3. **LOOKUP BY USER ID:** Query all payment attempts made by a specific customer account (`GET /api/v1/payments/user/{userId}`). Returns `List<PaymentResponse>`.
  4. **PROCESS REFUND:** Revert a successful transaction (`POST /api/v1/payments/refund`). Restricted strictly to administrators (`hasRole('ADMIN')`). Allowed **only** when payment status is `SUCCESS`.
* **Critical Absence of Global Payment Listing (`BACKEND-CONFIRMED`):**
  * `PaymentController` does **NOT** expose `GET /api/v1/payments`.
  * `PaymentRepository` has **zero** global pagination or unbounded list methods.
  * **Strict Exclusion:** The UI **never** renders a fake global transaction table, fake pagination ("Showing 1–20 of 1,200 payments"), simulated revenue charts, or recent activity tickers.
* **Explicit Scope Exclusions (`SCOPE LIMITATION`):**
  * **No Partial Refunds:** Backend `RefundRequest` only takes `paymentId` and `reason`. Zero custom amount or partial refund capabilities exist.
  * **No Payment Method Configuration:** No gateway settings, API key inputs, or webhook management.
  * **No Card / Bank Details:** No credit card numbers, CVVs, expiration dates, or bank account identifiers are stored or exposed.
  * **No Fraud Detection / Risk Scoring:** No artificial risk indicators or telemetry meters.
  * **No Chargeback / Dispute Management:** No disputed transaction workflows.
  * **No Invoices / Tax / Accounting Reports:** No VAT invoices, tax line items, or PDF downloads.
  * **No Bulk Refund Actions:** Refunds are submitted strictly per individual transaction.

---

## 2. Route & Access Control

* **Canonical Route:** `/admin/payments`
* **Query Parameters Supported (URL-Driven Lookup):**
  * `/admin/payments?paymentId={id}`: Automatically executes Direct Payment Lookup for the specified ID.
  * `/admin/payments?orderId={orderId}`: Automatically executes Order Payments Query (e.g. arriving from `/admin/orders` via `[ INSPECT PAYMENT → ]`).
  * `/admin/payments?userId={userId}`: Automatically executes Customer Payment Ledger query.
* **Route Type:** Protected Administrative Route.
* **Actor & Permissions Matrix:**

| Actor | Action / Method | Behavior & UX Response | Enforcement Layer | Classification |
| :--- | :--- | :--- | :--- | :--- |
| **Guest (Unauthenticated)** | Any | Intercepted by auth guard; navigates to `/login` | `FE-15 Auth Guard` | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Any | Renders inline **Access Denied** vitrine; does **NOT** log out | Client Route Guard | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Direct `POST /refund` | HTTP 403 Forbidden | `PaymentSecurityConfig.java:48` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /payments/{id}` | HTTP 200 OK — ownership check **bypassed** | `PaymentController.java:54` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /payments/order/{id}` | HTTP 200 OK — ownership check **bypassed**; returns `List<PaymentResponse>` | `PaymentController.java:64` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /payments/user/{id}` | HTTP 200 OK — ownership check **bypassed**; returns `List<PaymentResponse>` | `PaymentController.java:77` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `POST /payments/refund` | HTTP 200 OK — executes refund; returns updated `PaymentResponse` | `PaymentController.java:44-48` | `BACKEND-CONFIRMED` |

### Security & Ownership Bypass Verification (`BACKEND-CONFIRMED`)
In `PaymentController.java:51-58`:
```java
PaymentPrincipal principal = getAuthenticatedPrincipal();
PaymentResponse payment = paymentService.getPaymentById(id);
if (!principal.isAdmin() && (payment.getUserId() == null || !payment.getUserId().equals(principal.userId()))) {
    throw new ApiException("Access denied to payment", HttpStatus.FORBIDDEN);
}
```
* **Verification:** When `principal.isAdmin()` is `true`, the condition evaluates to `false`. Administrators bypass the ownership check across all query vectors (`/payments/{id}`, `/order/{orderId}`, `/user/{userId}`).
* **Verified in Tests:** Tested in `PaymentSecurityBoundaryTest.java:263-277` (`getByUserId_adminAccessingOtherUserPayments_returns200`), `316-331` (`getByOrderId_adminAccessingOtherUserOrderPayments_returns200`), and `367-391` (`refund_byAdmin_returns200Ok`).

---

## 3. Backend Verification & Integration Contracts

### 3.1 Verified Source Code References

| Capability | Backend File & Line | Method & Signature | Response Type | Verified Implementation Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **Get Payment by ID** | `PaymentController.java:50-58` | `getPaymentById(@PathVariable Long id)` | `PaymentResponse` | Calls `paymentService.getPaymentById(id)`. Admin bypasses ownership. Throws 404 if absent. |
| **Get Payments by Order**| `PaymentController.java:60-72` | `getByOrderId(@PathVariable Long orderId)` | `List<PaymentResponse>` | Calls `paymentService.getPaymentsByOrderId(orderId)`. Returns `List` (unpaginated). Admin bypasses ownership. |
| **Get Payments by User** | `PaymentController.java:74-81` | `getByUserId(@PathVariable Long userId)` | `List<PaymentResponse>` | Calls `paymentService.getPaymentsByUserId(userId)`. Returns `List` (unpaginated). Admin bypasses ownership. |
| **Process Refund** | `PaymentController.java:40-48` | `processRefund(@Valid @RequestBody RefundRequest req)` | `PaymentResponse` | Strictly requires `hasRole('ADMIN')`. Throws 422 `PaymentProcessingException` if not `SUCCESS`. |
| **Security Config** | `PaymentSecurityConfig.java:48-50` | `paymentSecurityFilterChain()` | N/A | Enforces `POST /payments/refund` requires `ADMIN`. All other endpoints require authentication. |
| **Error Handling** | `GlobalExceptionHandler.java:22-27` | `handleApiException()` | `ErrorResponse` | Maps `ApiException`, `PaymentNotFoundException` (404), `PaymentProcessingException` (422). |

---

### 3.2 Request & Response Data Contracts

#### A. Direct Payment Lookup Contract (`GET /api/v1/payments/{id}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/payments/{id}`
* **Path Parameter:** `id: Long` (e.g. `42`)
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Response Status:** `200 OK`
* **Response Payload Schema (`PaymentResponse`):**
  ```json
  {
    "id": 42,
    "transactionId": "TXN-9843-2026",
    "orderId": 105,
    "userId": 2,
    "amount": 12480000.00,
    "paymentMethod": "CREDIT_CARD",
    "status": "SUCCESS",
    "message": "Payment processed successfully via CREDIT_CARD",
    "createdAt": "2026-10-07T10:15:32"
  }
  ```

#### B. Order Payments Query Contract (`GET /api/v1/payments/order/{orderId}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/payments/order/{orderId}`
* **Path Parameter:** `orderId: Long` (e.g. `105`)
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Pagination & Sorting:** **None.** Returns raw `List<PaymentResponse>`.
* **Response Status:** `200 OK`
* **Response Payload:** Array of `PaymentResponse` objects representing all payment attempts for that order.

#### C. User Payment Ledger Contract (`GET /api/v1/payments/user/{userId}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/payments/user/{userId}`
* **Path Parameter:** `userId: Long` (e.g. `2`)
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Pagination & Sorting:** **None.** Returns raw `List<PaymentResponse>`.
* **Response Status:** `200 OK`
* **Response Payload:** Array of `PaymentResponse` objects representing all payment attempts made by that customer.

#### D. Refund Payment Contract (`POST /api/v1/payments/refund`)
* **HTTP Method:** `POST`
* **Path:** `/api/v1/payments/refund`
* **Headers:**
  * `Authorization: Bearer <admin_jwt_token>`
  * `Content-Type: application/json`
* **Request Payload Schema (`RefundRequest`):**
  ```json
  {
    "paymentId": 42,
    "reason": "Customer requested cancellation prior to warehouse fulfillment"
  }
  ```
* **Validation Rules (`BACKEND-CONFIRMED`):**
  * `paymentId`: `@NotNull(message = "Payment ID is required")`
  * `reason`: `@NotBlank(message = "Reason is required")`
* **Response Status:** `200 OK`
* **Response Payload:** Updated `PaymentResponse` with `"status": "REFUNDED"` and `"message": "Refunded. Reason: Customer requested cancellation..."`.
* **Error Response (Ineligible Refund - 422):**
  ```json
  {
    "status": 422,
    "message": "Cannot refund payment in status: FAILED",
    "timestamp": "2026-10-08T19:30:00.000"
  }
  ```

---

### 3.3 Field-by-Field Backend Verification Matrix

Audited directly from `PaymentResponse.java:13-25`:

| Field Name | Java Type | Nullable? | Exact Backend Meaning | UI Presentation & Treatment |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `Long` | No | Primary key ID in `payments` table | `PAYMENT #42` (Plus Jakarta Sans `22px` Bold) |
| `transactionId` | `String` | No | Unique gateway reference UUID/code | JetBrains Mono `13px` Bold `#1C1917` (e.g. `TXN-9843-2026`) |
| `orderId` | `Long` | No | Associated order foreign key ID | `Order ID #105` + link `[ VIEW ORDER → ]` |
| `userId` | `Long` | No | Customer foreign key ID | `User ID #02` + link to customer context |
| `amount` | `BigDecimal` | No | Captured transaction currency amount | JetBrains Mono `16px` Bold (e.g. `12,480,000 VND`) |
| `paymentMethod` | `PaymentMethod`| No | Enum: `CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`, `WALLET` | JetBrains Mono `13px` `#1C1917` |
| `status` | `PaymentStatus`| No | Enum: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED` | Color-calibrated status badge |
| `message` | `String` | Yes | Gateway outcome / refund reason text | Formatted descriptive text |
| `createdAt` | `LocalDateTime`| No | Transaction timestamp | Formatted `YYYY-MM-DD HH:mm:ss UTC` (JetBrains Mono) |

#### Strictly Forbidden Fields (`SCOPE LIMITATION`)
The following fields **DO NOT EXIST** in `PaymentResponse` and must **NEVER** be displayed or simulated:
* ❌ `cardNumber` / `cardLast4` / `cardBrand`
* ❌ `cvv` / `expirationDate`
* ❌ `bankName` / `bankAccountNumber` / `routingNumber`
* ❌ `taxAmount` / `vatRate`
* ❌ `refundFee` / `processingFee`
* ❌ `exchangeRate` / `settlementCurrency`
* ❌ `fraudRiskScore` / `riskLevel`
* ❌ `invoiceNumber` / `downloadInvoicePdf`

---

## 4. Payment Status & Lifecycle Management

### 4.1 Exact Backend `PaymentStatus` Enum (`BACKEND-CONFIRMED`)
From `com.ecommerce.paymentservice.model.PaymentStatus`:
```java
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
```

* **Color & Visual Tokens Matrix:**

| Status Enum Value | Visual Badge Style | Semantic Meaning | Admin Action Available |
| :--- | :--- | :--- | :--- |
| `PENDING` | `#B45309` on `#FFFBEB` (Amber) | Payment initiated, awaiting gateway webhook/result | Read-only (Cannot refund) |
| `SUCCESS` | `#15803D` on `#F0FDF4` (Green) | Payment captured and verified | **Eligible for `[ ISSUE REFUND ]`** |
| `FAILED` | `#DC2626` on `#FEF2F2` (Red) | Payment declined or gateway error | Read-only (Cannot refund) |
| `REFUNDED` | `#9333EA` on `#FAF5FF` (Purple)| Transaction permanently reversed | Closed (Cannot re-refund) |

* **Forbidden Status Values:** No `PROCESSING_PAYMENT`, `PARTIALLY_REFUNDED`, `CHARGEBACK`, or `DISPUTED` exist in the backend.

---

### 4.2 Refund Eligibility & Enforcement Rules (`BACKEND-CONFIRMED`)
In `PaymentService.java:65-68`:
```java
if (payment.getStatus() != PaymentStatus.SUCCESS) {
    throw new PaymentProcessingException(
        "Cannot refund payment in status: " + payment.getStatus());
}
```
* **Strict Rule:** **ONLY payments in `SUCCESS` status can be refunded.**
* **UI Enforcement Rule (`UI DESIGN DECISION`):**
  * The `[ ISSUE REFUND ]` button is active and accessible **strictly when `payment.status === 'SUCCESS'`**.
  * When `status !== 'SUCCESS'` (i.e. `PENDING`, `FAILED`, or `REFUNDED`), the refund button is **disabled** (`opacity: 0.5; cursor: not-allowed; title: "Only successful payments can be refunded"`).
  * This prevents triggering HTTP 422 `PaymentProcessingException` errors.

---

## 5. UI Architecture & Operational Workflows

The `/admin/payments` screen consists of two primary operational zones:
1. **Upper Zone:** Multi-Vector Operational Lookup Console.
2. **Lower Zone:** Dynamic Payment Results Area.

```
┌────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR — ADMIN                                      admin@techgear.io│
├────────────────────────────────────────────────────────────────────────┤
│ DASHBOARD    PRODUCTS    CATEGORIES    USERS    ORDERS    [ PAYMENTS ] │
├────────────────────────────────────────────────────────────────────────┤
│ ADMIN / PAYMENTS                                                       │
│ PAYMENT OPERATIONS                                                     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  OPERATIONAL LOOKUP CONSOLE                                            │
│  ┌──────────────────────┬──────────────────────┬─────────────────────┐ │
│  │ VECTOR 1: PAYMENT ID │ VECTOR 2: ORDER ID   │ VECTOR 3: USER ID   │ │
│  │ ID: [ e.g. 42      ] │ ORDER: [ 105       ] │ UID: [ e.g. 2     ] │ │
│  │ [ INSPECT PAYMENT ]  │ [ VIEW ORDER PMTS ]  │ [ VIEW USER PMTS ]  │ │
│  └──────────────────────┴──────────────────────┴─────────────────────┘ │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  DYNAMIC RESULT AREA (ONE OF 3 STATES):                                │
│                                                                        │
│  STATE 1: INITIAL / EMPTY (Prompt to enter a query vector)             │
│  STATE 2: SINGLE PAYMENT INSPECTION PANEL (When queried by Payment ID) │
│  STATE 3: PAYMENT ATTEMPTS LIST TABLE (When queried by Order or User)  │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

### 5.1 Query Vectors Specification

#### Vector 1: By Payment ID (`GET /api/v1/payments/{id}`)
* **Input Field:** `PAYMENT ID` (e.g. `42`)
* **Validation:** Positive integer (`^[1-9][0-9]*$`).
* **Result Display:** Renders the single **Inspected Payment Detail Panel**.

#### Vector 2: By Order ID (`GET /api/v1/payments/order/{orderId}`)
* **Input Field:** `ORDER ID` (e.g. `105`)
* **Validation:** Positive integer.
* **Result Display:** Renders the **Order Payments Table** (displays all payment attempts for that order).
  * Columns: `PAYMENT ID`, `TRANSACTION ID`, `AMOUNT`, `METHOD`, `STATUS`, `DATE`, `ACTION` (`[ INSPECT ]`).
  * Clicking `[ INSPECT ]` focuses the target payment.

#### Vector 3: By User ID (`GET /api/v1/payments/user/{userId}`)
* **Input Field:** `USER ID` (e.g. `2`)
* **Validation:** Positive integer.
* **Result Display:** Renders the **Customer Payment History Table** (unpaginated list).
  * Columns: `PAYMENT ID`, `ORDER ID`, `TRANSACTION ID`, `AMOUNT`, `METHOD`, `STATUS`, `DATE`, `ACTION` (`[ INSPECT ]`).

---

### 5.2 Inspected Payment Detail Panel (Single Payment View)

Displays comprehensive transaction parameters for a single verified payment (e.g. Payment #42):

* **Top Bar:**
  * Title: `PAYMENT #42` (Plus Jakarta Sans `22px` font-bold `#1C1917`).
  * Status Badge: `SUCCESS` (green `#15803D` on `#F0FDF4`).
  * Action Button: `[ ISSUE REFUND ]` (destructive red styling; enabled for `SUCCESS`).
* **4-Column Technical Specification Grid:**
  * **Col 1:** Transaction Code (`TXN-9843-2026`) &bull; Captured Amount (`12,480,000 VND`).
  * **Col 2:** Order Reference (`Order ID #105` with `[ VIEW ORDER → ]` link pointing to `/admin/orders?orderId=105`) &bull; Customer (`User ID #02 Elena Rostova`).
  * **Col 3:** Payment Method (`CREDIT_CARD`) &bull; Processed Timestamp (`2026-10-07 10:15:32 UTC`).
  * **Col 4:** Gateway Response (`Payment processed successfully via CREDIT_CARD`) &bull; Refund Status (`ELIGIBLE (STATUS: SUCCESS)`).

---

## 6. Refund Flow & Confirmation Dialog

```
┌────────────────────────────────────────────────────────────────────────┐
│ [#42] ISSUE PAYMENT REFUND                                         [✕] │
├────────────────────────────────────────────────────────────────────────┤
│ TRANSACTION SUMMARY BANNER (Milled #F5F5F4):                           │
│ PAYMENT ID: #42  |  ORDER ID: #105                                     │
│ REFUND AMOUNT: 12,480,000 VND  |  TYPE: FULL REFUND (100% REVERSAL)   │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│ REFUND REASON (REQUIRED BY BACKEND CONTRACT):                          │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ Customer requested cancellation prior to warehouse fulfillment     │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│ This action will trigger POST /api/v1/payments/refund and transition   │
│ status to REFUNDED. This action is permanent and cannot be undone.     │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ [ CANCEL ]                                    [ SUBMIT FULL REFUND ]   │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.1 Step-by-Step Refund Execution Workflow
1. **Trigger:** Administrator clicks `[ ISSUE REFUND ]` on an eligible `SUCCESS` payment.
2. **Modal Opens:** Centered high-density modal overlay renders with dim backdrop.
3. **Transaction Summary:** Re-states `paymentId`, `orderId`, and full captured amount (`12,480,000 VND`).
4. **Mandatory Reason Input:**
   * Label: `REFUND REASON (REQUIRED BY BACKEND CONTRACT)`.
   * Input: Text field or textarea.
   * Client Validation: Must not be blank (`reason.trim().length > 0`). If empty, displays `"Reason is required"`.
5. **Submission:**
   * Clicking `[ SUBMIT FULL REFUND ]` issues:
     `POST /api/v1/payments/refund` with body `{"paymentId": 42, "reason": "..."}`.
   * Button enters loading state with spinner.
6. **Success Handling:**
   * Modal closes.
   * Payment details re-render with updated `REFUNDED` purple badge and message `"Refunded. Reason: ..."`.
   * Destructive `[ ISSUE REFUND ]` button becomes disabled.

---

## 7. Order Relationship & Cross-Navigation

* **Cross-Module Link Specification (`ADMIN-01 & ADMIN-06 Alignment`):**
  * When viewing a payment, the Order Reference displays:
    `Order ID #{orderId}` along with an interactive link:
    `[ VIEW ORDER → ]`.
  * **Destination Route:** `/admin/orders?orderId={orderId}`.
  * Clicking this link navigates the administrator directly to the canonical Order Operations screen with the target order pre-queried.
* **Zero Embedded Order UI:**
  * The Payments screen **does NOT embed** the order contents table, line items, or customer shipping forms. Order details live exclusively inside `/admin/orders`.

---

## 8. State Machine & Error Handling

```
┌─────────────────┬────────────────────────────────────────────────────────────┐
│ State           │ UX Presentation & Component Behavior                       │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 1. Initial      │ Lookup Console rendered; Result Area displays neutral      │
│                 │ vitrine: "ENTER A PAYMENT ID, ORDER ID, OR USER ID."       │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 2. Loading      │ High-density skeleton loader matching the 4-column spec    │
│                 │ grid; inputs locked during flight.                         │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 3. 404 Not Found│ "PAYMENT NOT FOUND: No transaction record for ID #{id}."   │
│                 │ Action button: [ CLEAR LOOKUP ] resets focus.              │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 4. Empty List   │ "NO PAYMENTS RECORDED for Order #{id} / User #{id}."       │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 5. 422 Conflict │ Toast message: "Cannot refund payment in status: {status}."│
│                 │ Automatically re-fetches payment record to sync state.     │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 6. 403 Forbidden│ Inline "ACCESS DENIED" card; active session preserved.     │
├─────────────────┼────────────────────────────────────────────────────────────┤
│ 7. 500 Network  │ "SERVICE UNREACHABLE." [ RETRY ] button. No stack leaks.   │
└─────────────────┴────────────────────────────────────────────────────────────┘
```

---

## 9. Visual System & Design Tokens

Adhering strictly to **Obsidian & Paper** guidelines:

| Visual Element | Design Token | Usage in ADMIN-07 |
| :--- | :--- | :--- |
| **Page Canvas** | `#FAFAF9` (`--color-bg-canvas`) | Primary background |
| **Surface Base** | `#FFFFFF` (`--color-surface-base`) | Lookup console card, detail card, modal |
| **Surface Raised**| `#F5F5F4` (`--color-surface-raised`) | Metadata spec grid, transaction summary banner |
| **Primary Text** | `#1C1917` (`--color-text-primary`) | Headings, payment ID, amount |
| **Secondary Text**| `#57534E` (`--color-text-secondary`) | Field labels, timestamps, body text |
| **Muted Metadata**| `#78716C` (`--color-text-tertiary`) | Transaction codes, breadcrumbs |
| **Borders** | `1px solid #E7E5E4` | Hairline dividers, modal framing, input outlines |
| **Accent Signal** | `#D97706` (Signal Amber) | Interactive link `[ VIEW ORDER → ]`, focus rings |
| **Destructive** | `#DC2626` / `#FEF2F2` bg | `[ ISSUE REFUND ]` button and refund submit action |
| **Success Status**| `#15803D` / `#F0FDF4` bg | `SUCCESS` status badge |
| **Refunded Status**| `#9333EA` / `#FAF5FF` bg | `REFUNDED` status badge |
| **Typography** | Plus Jakarta Sans & JetBrains Mono | Headings + monospaced data values |
| **Border Radius**| `4px` controls, `6px` cards | Precision cold-rolled industrial fabrication |

---

## 10. Responsive Architecture

### 10.1 Desktop (&ge; 1280px)
* 3-column horizontal grid for the Operational Lookup Console.
* 4-column horizontal spec grid for single payment inspection.
* Centered 560px modal dialog for the refund confirmation flow.

### 10.2 Tablet (768px – 1279px)
* Lookup vectors stack into 2 rows.
* Specification grid collapses to a 2-column &times; 2-row layout.
* Modal scales to 90% viewport width with full touch accessibility.

### 10.3 Mobile (< 768px)
* Lookup inputs and buttons stack vertically.
* Specification grid collapses to a single-column key-value stack.
* Payment lists convert to individual stacked transaction cards.
* All touch targets meet or exceed `44px × 44px`.

---

## 11. Canonical Stitch Screen Reference

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `de81cb8973e144bd8e966a2707053689`
* **Title:** `TECHGEAR — Admin Payments`
* **Device Type:** `DESKTOP`
* **Design System Asset:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (Obsidian & Paper)
* **Screen Features Captured:**
  1. Sticky 64px administrative navigation rail with active `PAYMENTS` tab indicator and session badge `admin@techgear.io [ADMIN]`.
  2. Page header with breadcrumb `ADMIN / PAYMENTS` and title `PAYMENT OPERATIONS`.
  3. Multi-Vector Lookup Console displaying 3 query vectors (By Payment ID, By Order ID filled with active value `105`, and By User ID).
  4. Inspected Payment Detail card for `PAYMENT #42`:
     - Header with green `SUCCESS` badge and destructive `[ ISSUE REFUND ]` button.
     - 4-column spec grid displaying Transaction ID `TXN-9843-2026`, Captured Amount `12,480,000 VND`, Order Reference `Order ID #105` with `[ VIEW ORDER → ]` link (`/admin/orders?orderId=105`), Customer `User ID #02 Elena Rostova`, Payment Method `CREDIT_CARD`, and Gateway message.
  5. Active **Issue Payment Refund Modal** overlay:
     - Transaction summary banner with Payment ID `#42`, Order ID `#105`, and Amount `12,480,000 VND`.
     - Form input populated with mandatory reason: `"Customer requested cancellation prior to warehouse fulfillment"`.
     - Permanent reversal contract notice and action buttons (`[ CANCEL ]` and `[ SUBMIT FULL REFUND ]`).
  6. Administrative footer with standard TECHGEAR branding.

---

## 12. Scope Classification Summary Table

| Category / Capability | Exact Backend / UI State | Classification Tag | Source Reference / Rationale |
| :--- | :--- | :--- | :--- |
| **Absence of GET All Payments**| No `GET /api/v1/payments` endpoint | `BACKEND-CONFIRMED` | Audited `PaymentController.java` — zero unbounded list |
| **Payment ID Lookup** | `GET /api/v1/payments/{id}` | `BACKEND-CONFIRMED` | `PaymentController.java:50-58` |
| **Order ID Payments Query** | `GET /api/v1/payments/order/{orderId}` | `BACKEND-CONFIRMED` | Returns `List<PaymentResponse>` (unpaginated) |
| **User ID Payments Query** | `GET /api/v1/payments/user/{userId}` | `BACKEND-CONFIRMED` | Returns `List<PaymentResponse>` (unpaginated) |
| **Admin Ownership Bypass** | `principal.isAdmin()` bypasses check | `BACKEND-CONFIRMED` | `PaymentController.java:54, 64, 77` |
| **Refund Endpoint** | `POST /api/v1/payments/refund` | `BACKEND-CONFIRMED` | `PaymentController.java:40-48` |
| **Refund Eligibility** | Only allowed if `status === 'SUCCESS'` | `BACKEND-CONFIRMED` | Throws 422 `PaymentProcessingException` (`PaymentService:65-68`) |
| **Refund Button Guard** | Disabled in UI if status not SUCCESS | `UI DESIGN DECISION` | Prevents unnecessary 422 error states |
| **Refund Request DTO** | `paymentId: Long, reason: String` | `BACKEND-CONFIRMED` | `RefundRequest.java:8-15` — strictly 2 fields |
| **PaymentStatus Enum** | Exactly 4 values (`PENDING, SUCCESS, FAILED, REFUNDED`)| `BACKEND-CONFIRMED` | `PaymentStatus.java:3-8` |
| **PaymentResponse DTO** | `id, transactionId, orderId, userId, amount, ...` | `BACKEND-CONFIRMED` | `PaymentResponse.java:14-24` |
| **Order Link** | Link to `/admin/orders?orderId={id}` | `UI DESIGN DECISION` | Cross-module navigation aligned with `ADMIN-06` |
| **Inspected Payment #42** | Linked to Order #105, 12,480,000 VND | `DESIGN MOCK DATA` | Representative seed payment data |
| **Multi-Vector Console** | 3-column query grid | `UI DESIGN DECISION` | Clean separation of query vectors |
| **Forbidden Fraud/Card Data**| No CVV, no card numbers, no risk score | `SCOPE LIMITATION` | Strict YAGNI — zero backend support |

---

## 13. Final Audit Checklist

- [x] **Payment endpoints verified:** Audited against `PaymentController.java` (`/{id}`, `/order/{orderId}`, `/user/{userId}`, `/refund`).
- [x] **No GET all payments assumed:** Verified that `GET /api/v1/payments` does not exist; no fake global table created.
- [x] **Lookup modes verified:** Verified 3 distinct query vectors (Payment ID, Order ID, User ID).
- [x] **Response DTOs verified:** Returns `PaymentResponse` and `List<PaymentResponse>`.
- [x] **PaymentStatus verified:** Exactly 4 enum values (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`).
- [x] **Pagination verified:** Verified unpaginated (`List<PaymentResponse>`); no fake pagination widgets.
- [x] **Sorting/filter capability verified:** No date, status, or customer-segment filter parameters exist on the audited payment lookup endpoints.
- [x] **RefundRequest verified:** Exactly two fields: `paymentId: Long` and `reason: String`.
- [x] **Refund eligibility verified:** Strictly allowed when `status === SUCCESS`; button disabled on other statuses to prevent 422.
- [x] **Refund response verified:** Returns updated `PaymentResponse` with `status: REFUNDED`.
- [x] **ADMIN permission verified:** Guarded by `hasRole('ADMIN')` in `PaymentSecurityConfig.java:48` and `PaymentController.java:44`.
- [x] **401/403 verified:** 401 navigates to `/login`; 403 displays inline Access Denied without destroying customer session.
- [x] **Order relationship verified:** Displays `orderId` with link to `/admin/orders?orderId={orderId}`.
- [x] **No fake payment fields:** Card numbers, CVV, expiry, bank accounts, exchange rates, fraud scores completely excluded.
- [x] **No fake payment states:** No `PARTIALLY_REFUNDED`, `CHARGEBACK`, or `DISPUTED` created.
- [x] **No fake refund functionality:** No partial refund, refund destination, or refund schedule invented.
- [x] **No analytics:** Zero revenue charts, volume graphs, or telemetry cards.
- [x] **Exactly 1 canonical Stitch screen:** Created screen `de81cb8973e144bd8e966a2707053689` in project `113306237264437644`.
- [x] **Documentation created:** Written to [ADMIN-07-PAYMENTS-DESIGN.md](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-07-PAYMENTS-DESIGN.md).
- [x] **ADMIN-01 → ADMIN-06 unchanged:** Existing specifications untouched.
- [x] **FE-01 → FE-15 unchanged:** Storefront specifications untouched.
- [x] **No code/package/backend/Git changes:** Zero implementation code or configuration modifications made.

---

**ADMIN-07 STATUS: READY FOR REVIEW**
