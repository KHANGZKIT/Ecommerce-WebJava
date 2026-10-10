# TECHGEAR — Payment Result Design Specification

> **Document ID:** FE-07-PAYMENT-RESULT-DESIGN  
> **Route:** `/payment-result` (Router alias: `/payment/result`)  
> **Version:** 1.0.1  
> **Screen:** Customer Payment / Checkout Result  
> **Visual Reference:** Stitch Project (`projectId: 113306237264437644`, `screenId: 7be90e88daf64adf8c43eb1b2277e8be`)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Backend Verification:** Spring Boot 3.2 `order-service` (`OrderController.java:76-101`, `OrderService.java:170-289`) & `payment-service` (`PaymentService.java:33-58`) Verified  
> **Skills Applied:** `ui-ux-pro-max` (clarity of outcome & accessible hierarchy) & `design-taste-frontend` (restrained Swiss vitrine, zero AI-slop)  
> **Status:** READY FOR REVIEW

---

## 1. Purpose & User Goal

*   **Purpose:** The Payment Result screen is the dedicated confirmation vitrine following checkout submission (`POST /api/v1/orders/checkout`). It translates the backend orchestration response into an authoritative, customer-facing resolution without exposing internal distributed transactions or database implementation details.
*   **User Goal:**
    1.  *Confirm immediate outcome:* Understand with absolute certainty whether the order was placed or declined.
    2.  *Capture order reference:* Review the confirmed Order ID (`Order #105`) and final total amount in VND.
    3.  *Clear next step:* Seamlessly navigate to Order Details (`/orders/{orderId}`), continue browsing the catalog (`/shop`), or return to checkout/cart if the transaction failed.
*   **Customer Journey Placement:**
    ```
    Home (FE-02) 
      → Shop (FE-03) 
      → Product Detail (FE-04) 
      → Cart (FE-05) 
      → Checkout (FE-06) 
      → PAYMENT RESULT (FE-07: /payment-result)
           ├─ Success ─────────→ Order Detail (/orders/:id) or Storefront (/shop)
           ├─ Payment Failed ──→ Return to Checkout (/checkout) or Cart (/cart)
           └─ Payment Unknown ─→ Order History (/orders)
    ```

---

## 2. Route & Navigation Contracts

*   **Primary Route:** `/payment-result`
*   **Sitemap Compatibility:** Also handles `/payment/result` (as registered in [FE-01-CATALOG-SITEMAP-FLOW.md](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-01-CATALOG-SITEMAP-FLOW.md#L101)) via client router redirection.
*   **Access Guard:** Accessible only following a checkout execution containing response data in router memory/session state. Direct URL visits without active checkout context redirect safely to `/orders` or `/shop`.

---

## 3. Backend API Grounding & Response Data Contract

FE-07 is strictly coupled to the response payload returned by `POST /api/v1/orders/checkout` (`OrderController.java:76-101`):

```json
{
  "authenticatedUserId": 42,
  "idempotencyKey": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "orderId": 105,
  "status": "CONFIRMED",
  "totalAmount": 12480000.00,
  "requestFingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "cartClearStatus": "CLEARED"
}
```

### 3.1 Response Fields Utilized by FE-07

| Response Field | Type | Role in FE-07 UI |
| :--- | :---: | :--- |
| `orderId` | `Long` | Primary order reference shown as `Order #105`. Used for primary navigation link `/orders/{orderId}`. |
| `status` | `String` | Determines which of the 3 UI presentations is rendered (`CONFIRMED`, `PAYMENT_FAILED`, `PAYMENT_UNKNOWN`). |
| `totalAmount` | `BigDecimal` | Final confirmed amount formatted in VND with tabular monospace font: `12.480.000 ₫`. |
| `cartClearStatus` | `String` | Internal queue indicator (`CLEARED`, `PENDING`, `NOT_REQUIRED`). Evaluated neutrally without disrupting customer assurance. |

> [!IMPORTANT]
> **Strict YAGNI & Field Restraint:**
> The checkout response does **not** return payment gateway transaction IDs, estimated shipping dates, delivery tracking numbers, tax breakdown, or payment provider logos. FE-07 renders strictly the verified fields (`orderId`, `status`, `totalAmount`).

---

## 4. State Mapping & Presentation Specifications

FE-07 maps the 4 backend-confirmed checkout statuses into 3 distinct, focused visual presentations:

```
Backend Response Status
├── "CONFIRMED"                      ──► STATE 1: ORDER CONFIRMED (Success Vitrine)
├── "CONFIRMED_CART_CLEAR_PENDING"   ──► STATE 1: ORDER CONFIRMED (Identical Success Vitrine)
├── "PAYMENT_FAILED"                 ──► STATE 2: PAYMENT FAILED (Contextual Recovery Vitrine)
└── "PAYMENT_UNKNOWN"                ──► STATE 3: PAYMENT STATUS UNKNOWN (Neutral Advisory Vitrine)
```

---

### 4.1 State 1: Order Confirmed (`CONFIRMED` & `CONFIRMED_CART_CLEAR_PENDING`)

*   **Backend Verification & Status Meaning:**
    *   `CONFIRMED` (`HTTP 200`): The order is confirmed and the checkout operation succeeded.
    *   `CONFIRMED_CART_CLEAR_PENDING` (`HTTP 202 ACCEPTED`): `CONFIRMED_CART_CLEAR_PENDING` is treated as a successful checkout. The order is confirmed and the cart-clear operation is pending.
*   **Unified Treatment:** Both statuses indicate a successful order. They share the identical success vitrine UI. `CONFIRMED_CART_CLEAR_PENDING` uses `orderId` from the response to proceed directly to Order Detail (`/orders/{orderId}`). No database or internal implementation details are exposed to the user.
*   **Visual Anatomy (Obsidian & Paper):**
    1.  **Status Indicator:** Minimal circular glyph (`w-12 h-12`, `#F5F5F4` background, `1px solid #E7E5E4` border) containing a clean obsidian checkmark (`#1C1917`).
    2.  **Headline:** `ORDER CONFIRMED` (Plus Jakarta Sans `28px` bold, uppercase, tracking-tight, `#1C1917`).
    3.  **Factual Subtitle:** "Your order has been placed successfully." (Plus Jakarta Sans `15px`, `#57534E`).
    4.  **Machined Order Details Inset:**
        *   Container: Surface `#F5F5F4`, border `1px solid #E7E5E4`, radius `4px`, padding `p-5`, space-y-3.
        *   Row 1: Label `ORDER NUMBER` (JetBrains Mono `11px`, `#78716C`, uppercase), Value `Order #105` (JetBrains Mono `14px` SemiBold, `#1C1917`).
        *   Row 2: Label `STATUS` (JetBrains Mono `11px`, `#78716C`, uppercase), Badge `CONFIRMED` (JetBrains Mono `11px`, `#1C1917`, bg `#FFFFFF`, 1px border `#E7E5E4`, radius `2px`, px-2 py-0.5).
        *   Divider: Hairline rule `border-t border-[#E7E5E4] my-2`.
        *   Row 3: Label `TOTAL` (Plus Jakarta Sans `15px` SemiBold, `#1C1917`), Value `12.480.000 ₫` (JetBrains Mono `22px` Bold, `#1C1917`, tabular numbers).
    5.  **Notice:** "A record of this purchase has been registered to your account." (Plus Jakarta Sans `13px`, `#78716C`).
    6.  **Action CTAs:**
        *   Primary Button: Solid **Signal Amber** `VIEW ORDER` (`href="/orders/{orderId}"`, height `48px`, full width, uppercase, font-semibold `14px`, 4px radius, hover `#B45309`).
        *   Secondary Button: Machined outline `CONTINUE SHOPPING` (`href="/shop"`, height `44px`, full width, bg `#FFFFFF`, border `#E7E5E4`, text `#1C1917`, hover `#F5F5F4`).

---

### 4.2 State 2: Payment Failed (`PAYMENT_FAILED`)

*   **Backend Verification:** Mock payment simulation declined charge (10% decline probability in `PaymentService.java:45-55`). `OrderService.java:236-276` marked order as `PAYMENT_FAILED` and automatically executed compensating stock release via `productClient.releaseStock(...)`.
*   **Strict Scope Restraint:** Backend has **no dedicated payment retry API** or retry service. The UI strictly avoids advertising a fake "Retry Payment" feature.
*   **Minimal Factual Wording:**
    1.  **Status Indicator:** Minimal circular glyph (`w-12 h-12`, `#FEF2F2` background, `1px solid #FECACA` border) containing an understated red/stone exclamation symbol (`#B91C1C`).
    2.  **Headline:** `PAYMENT FAILED` (Plus Jakarta Sans `28px` bold, uppercase, `#1C1917`).
    3.  **Factual Subtitle:** "Your order was not completed." (Plus Jakarta Sans `15px`, `#57534E`).
    4.  **Explanatory Detail Inset:**
        *   Container: Background `#FAFAF9`, border `1px solid #E7E5E4`, radius `4px`, padding `p-5`, text-left.
        *   Factual Copy: *"Payment failed. Your order was not completed. Reserved stock has been released."* (`13px`, `#57534E`, leading-relaxed).
        *   *Restraint Guarantee:* Strictly omits claims about charges, refunds, financial provider behavior, bank authorization, or payment security.
    5.  **Action CTAs:**
        *   Primary Action: Solid obsidian button `RETURN TO CHECKOUT` (`href="/checkout"`, height `48px`, full width, bg `#1C1917`, text `#FAFAF9`, hover `#292524`).
            *   *Behavior:* Navigates back to `/checkout`, allowing customer to select an alternative payment method and submit a fresh checkout request with a newly generated `Idempotency-Key`.
        *   Secondary Action: Machined outline `BACK TO CART` (`href="/cart"`, height `44px`, full width, bg `#FFFFFF`, border `#E7E5E4`, text `#1C1917`, hover `#F5F5F4`).

---

### 4.3 State 3: Payment Status Unknown (`PAYMENT_UNKNOWN`)

*   **Backend Verification:** Downstream `PaymentClient` timed out or returned an indeterminate exception (`OrderService.java:206-216`). The order is saved in `PAYMENT_UNKNOWN` state for manual ops reconciliation. Stock remains temporarily held.
*   **Strict Scope Restraint:** No automatic polling loop, no WebSocket connection, and no duplicate charge submission.
*   **Visual Anatomy:**
    1.  **Status Indicator:** Minimal circular glyph (`w-12 h-12`, `#FFFBEB` background, `1px solid #FDE68A` border) containing a neutral amber inquiry icon (`#D97706`).
    2.  **Headline:** `PAYMENT STATUS UNKNOWN` (Plus Jakarta Sans `26px` bold, uppercase, `#1C1917`).
    3.  **Factual Subtitle:** "We couldn't confirm the payment result." (Plus Jakarta Sans `15px`, `#57534E`).
    4.  **Advisory Detail Inset:**
        *   Container: Background `#FFFBEB`, border `1px solid #FDE68A`, radius `4px`, padding `p-5`, text-left space-y-2.
        *   Order Reference: `Order #{orderId}` (JetBrains Mono `13px` SemiBold, `#1C1917`).
        *   Guidance Copy: *"Please check your order history before trying again."* (`13px`, `#92400E`, leading-relaxed).
    5.  **Action CTAs:**
        *   Primary Action: Solid obsidian button `VIEW MY ORDERS` (`href="/orders"`, height `48px`, full width, bg `#1C1917`, text `#FAFAF9`, hover `#292524`).
        *   Secondary Action: Machined outline `CONTINUE SHOPPING` (`href="/shop"`, height `44px`, full width, bg `#FFFFFF`, border `#E7E5E4`, text `#1C1917`, hover `#F5F5F4`).

---

## 5. Visual Hierarchy & ASCII Layout Architecture

FE-07 uses a **Focused Result Vitrine** layout centered within the standard TECHGEAR shell:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Shared Sticky 64px Nav — TECHGEAR STORE, Dynamic Cart Badge)│
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                       [ Canvas: #FAFAF9 ]                              │
│                                                                        │
│               ┌──────────────────────────────────────┐                 │
│               │ 2. FOCUSED RESULT VITRINE            │                 │
│               │    (Max-width 576px / max-w-xl)      │                 │
│               │                                      │                 │
│               │               ( ✓ )                  │                 │
│               │          ORDER CONFIRMED             │                 │
│               │  Your order has been placed          │                 │
│               │            successfully.             │                 │
│               │                                      │                 │
│               │ ┌──────────────────────────────────┐ │                 │
│               │ │ ORDER NUMBER          Order #105 │ │                 │
│               │ │ STATUS               [CONFIRMED] │ │                 │
│               │ │ ──────────────────────────────── │ │                 │
│               │ │ TOTAL              12.480.000 ₫  │ │                 │
│               │ └──────────────────────────────────┘ │                 │
│               │                                      │                 │
│               │  A record of this purchase has been  │                 │
│               │    registered to your account.       │                 │
│               │                                      │                 │
│               │   [       VIEW ORDER       ]         │                 │
│               │   [   CONTINUE SHOPPING    ]         │                 │
│               └──────────────────────────────────────┘                 │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ 3. FOOTER (Shared Customer Blueprint: Categories, Portal, Legal)       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Design System & Token Conformance

FE-07 adheres 100% to **Obsidian & Paper** (`assets/592f25a1d5524b0091e02d25cfc6dd89`):

*   **Background Canvas:** `#FAFAF9` (warm alabaster archival unbleached paper)
*   **Card Surface:** `#FFFFFF` (pure spec surface with crisp `1px solid #E7E5E4` framing)
*   **Inset Milled Bay:** `#F5F5F4` (tactile neutral for tabular data breakdown)
*   **Dividers & Hairlines:** `#E7E5E4` (uniform 1px borders, zero diffuse drop shadows)
*   **Typography:**
    *   *Plus Jakarta Sans:* Headlines (`ORDER CONFIRMED`), body guidance, button labels.
    *   *JetBrains Mono:* Tabular prices (`12.480.000 ₫`), Order IDs (`Order #105`), status badges (`CONFIRMED`).
*   **Primary Accent:** `#D97706` / `#B45309` (Signal Amber) applied exclusively to the primary conversion anchor `VIEW ORDER`.
*   **Control Radii:** 4px for buttons and badges; 6px for the outer card enclosure.

---

## 7. Responsive Behavior & Cart State Contract

*   **Desktop (≥ 1024px):**
    *   Vitrine container centered with `max-w-xl` (576px) and `py-16` vertical breathing room.
*   **Tablet (768px – 1023px):**
    *   Maintains centered `max-w-xl` container with adjusted outer padding (`px-6`).
*   **Mobile (< 768px):**
    *   Vitrine spans full width with `mx-4` margins and `p-6` internal padding.
    *   Buttons stack vertically with touch-friendly 48px height.
    *   Zero horizontal layout scrolling.

### 7.1 Header Cart Badge State Contract
*   **Dynamic Cart State:** The frontend must bind the header cart badge count dynamically to actual cart state (e.g. from the client's reactive cart store or `cart.itemCount`).
*   **No Hard-Coded Business Rule:** Cart count = 0 must **NOT** be hard-coded as a business rule on this page.
*   **`CONFIRMED_CART_CLEAR_PENDING` Handling:** The frontend must not assume the cart has already been cleared when `CONFIRMED_CART_CLEAR_PENDING` is received, as the cart-clear operation is pending.
*   **Stitch Visual Mock Clarification:** The `[0]` badge rendered in the Stitch screen (`screenId: 7be90e88daf64adf8c43eb1b2277e8be`) is static visual placeholder / mock data only and does not define frontend runtime state behavior.

---

## 8. Motion & Micro-interaction Specifications

Following `design-taste-frontend` dial settings:
*   `DESIGN_VARIANCE: 7`
*   `MOTION_INTENSITY: 3` (Understated, functional, non-distracting)
*   `VISUAL_DENSITY: 3` (Airy, museum vitrine aesthetic)
*   **Permitted Micro-animations:**
    *   Vitrine container: Subtle entrance fade-in (`opacity: 0 → 1`, `translateY: 4px → 0px`, duration `200ms ease-out`).
    *   Status indicator: Gentle scale-in (`scale: 0.95 → 1.0`, duration `150ms ease-out`).
    *   CTA hover: Standard background color transition (`150ms ease`).
*   **Strictly Prohibited:** Confetti explosions, full-page screen shakes, celebratory particle cannons, bouncing checkmark icons.

---

## 9. Accessibility & Standards Compliance (WCAG 2.1 AA)

*   **Document Structure:** Single semantic `<main>` element with proper heading hierarchy (`<h1>` for result status).
*   **Color Contrast:**
    *   Charcoal `#1C1917` on White `#FFFFFF` = `16.5:1` (exceeds AAA).
    *   Signal Amber `#D97706` button text with white = `3.6:1` (passes UI component / large text AA standard).
    *   Secondary stone text `#57534E` on White `#FFFFFF` = `7.3:1` (passes AAA).
*   **Screen Reader Semantics:** Status indicator icon wrapped with `aria-hidden="true"`; live assertive region announces order outcome immediately upon page mount (`aria-live="polite"`).
*   **Keyboard Navigation:** All CTA links focusable with clear, high-contrast `:focus-visible` outline rings.

---

## 10. Explicit Scope Exclusions (Strict YAGNI)

In adherence to strict YAGNI constraints, the following features are **explicitly excluded**:

*   ❌ **No Payment Gateway Redirects / Webhooks:** No Stripe Elements, VNPay, PayPal, or bank redirection popups.
*   ❌ **No Card Form / Payment Details Re-entry:** No card number, CVV, expiration date, or bank credential forms.
*   ❌ **No Dedicated Payment Retry API / Feature:** Backend does not provide a retry endpoint; failed payments simply allow the user to return to checkout.
*   ❌ **No Claims About Charges or Financial Provider Behavior:** Strictly no unconfirmed claims regarding bank processing, charges, or refunds in failure states.
*   ❌ **No Automatic Polling / WebSockets:** No background timer or persistent connection attempting to auto-resolve `PAYMENT_UNKNOWN`.
*   ❌ **No Shipping Tracking / Carrier Delivery Guarantees:** No 24h dispatch badges, carrier tracking numbers, or estimated delivery timelines.
*   ❌ **No Invoice / Receipt PDF Generation:** No billing invoice creation or PDF export controls.
*   ❌ **No Loyalty / Reward Points / Upsell Carousels:** No "Earned 50 points!" or "You may also like" product recommendations.
*   ❌ **No Pseudo-technical Architecture Jargon:** No transaction hash telemetry, database status codes, or distributed rollback descriptions.

---

## 11. Stitch Visual Screen Reference

*   **Project ID:** `113306237264437644`
*   **Screen ID:** `7be90e88daf64adf8c43eb1b2277e8be`
*   **Screen Title:** `TECHGEAR — Payment Result`
*   **Route:** `/payment-result`
*   **Design System:** *Obsidian & Paper* (`assets/592f25a1d5524b0091e02d25cfc6dd89`)
*   **Viewport:** Desktop (`2560 × 2262`)

---

## 12. Final Quality Gate Checklist

- [x] **CONFIRMED Handled:** Complete success vitrine with `Order #105`, total amount, and links to `/orders/{id}` and `/shop`.
- [x] **CONFIRMED_CART_CLEAR_PENDING Handled:** Documented with neutral frontend wording without exposing database/internal implementation details.
- [x] **PAYMENT_FAILED Minimal Wording:** Limited to factual *"Payment failed. Your order was not completed. Reserved stock has been released."* without unconfirmed claims about charges, refunds, or financial providers.
- [x] **Cart Count Contract Defined:** Cart count is treated as actual dynamic cart state; `[0]` in Stitch header is explicitly documented as visual mock data only.
- [x] **PAYMENT_UNKNOWN Handled:** Neutral advisory preventing duplicate submissions and guiding customer to `/orders`.
- [x] **No Fake Payment Form:** Completely free of card numbers, CVVs, and banking credentials.
- [x] **No Unsupported Commercial Claims:** Zero shipping speed promises or 24H dispatch stamps.
- [x] **No Unsupported Security Claims:** Zero fake banking encryption or verification seals.
- [x] **No Backend Changes:** Zero modifications to Java or Spring Boot services.
- [x] **No New Feature / API:** Strictly grounded in existing `POST /api/v1/orders/checkout` response.
- [x] **Obsidian & Paper Preserved:** 100% token conformance without generating a new design system.
- [x] **Consistency with FE-02 → FE-06:** Seamless continuation of the established aesthetic.
- [x] **No New Stitch Screen Generated:** Reused canonical screen `7be90e88daf64adf8c43eb1b2277e8be`.
- [x] **Responsive Desktop/Mobile:** Centered vitrine on desktop, clean full-width stacking on mobile.
- [x] **Subtle Purposeful Animation:** Controlled micro-transitions with zero flashy confetti or bouncing effects.
- [x] **No Pseudo-technical Jargon:** Free of developer console terms and operational metrics.
- [x] **No Git Commit / Push.**

---

**FE-07 STATUS: READY FOR REVIEW**
