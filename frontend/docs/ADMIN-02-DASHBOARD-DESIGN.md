# TECHGEAR — ADMIN-02 Admin Dashboard Design Specification

> **Document ID:** ADMIN-02-DASHBOARD-DESIGN  
> **Route:** `/admin`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `1715c1a3aabf40d0bc69ee17987a5e73`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md), [FE-15](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-15-AUTH-GUARD-LOGOUT-DESIGN.md)  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Admin Dashboard** (`/admin`) serves as the operational overview and central module navigation landing hub for TECHGEAR administrators. It provides a real-time operational overview, high-density entity vitrine counters, and direct navigation pathways to all administrative domain modules.

### Strict Operational Overview Definition & YAGNI Boundaries
* **Operational Overview & Module Navigation Only:** The dashboard acts strictly as an operational overview and navigation entrypoint to Admin Modules. It is **not** an analytics platform and **does not contain ID lookup forms**.
* **Zero Lookup Forms on Dashboard:** Tra cứu Order và Payment ID được thực hiện trực tiếp bên trong module tương ứng (`/admin/orders` và `/admin/payments`). Dashboard tuyệt đối không chứa form tra cứu ID.
* **Strict Anti-Slop & Real-Data Constraints:**
  * **No polling:** Metrics are loaded strictly once upon initial page mount / navigation.
  * **No periodic refresh:** Zero interval timers, polling loops, or WebSocket connections.
  * **No charts:** Zero line graphs, bar charts, pie charts, sparklines, or SVG telemetry curves.
  * **No historical analytics:** Zero date-range pickers, day/month filters, or timeframe comparisons.
  * **No trend calculation:** Zero artificial trend percentages or delta badges (e.g. "+5.4% this week").
  * **No analytics aggregation:** Zero client-side computation loops across bulk records.
* **Registered User Metric Grounding:**
  * `GET /api/v1/users` is used **solely to obtain `size()`** (`List<UserProfileDto>.size()`) because the backend currently does not expose a dedicated count or aggregate user endpoint.
  * **Không tạo aggregate API mới.**
* **Zero Speculative Infrastructure:** No background reporting workers, no timeseries databases, and no dedicated dashboard microservices.

---

## 2. Route, Access Control & Security Boundaries

### 2.1 Route Architecture
* **Canonical Route:** `/admin`
* **Route Type:** Protected Administrative Route.
* **Access Requirement:** Authenticated user with JWT claim `"role": "ADMIN"`.

### 2.2 Security & Guard Enforcement (FE-15 Alignment)
```
                     ┌──────────────────────────────┐
                     │ Access Route: /admin         │
                     └──────────────┬───────────────┘
                                    │
                       Is user authenticated (JWT)?
                                    │
                      ┌─────────────┴─────────────┐
                     YES                          NO
                      │                            │
           Is role === 'ADMIN'?             Navigate to /login
                      │
           ┌──────────┴──────────┐
          YES                    NO
           │                      │
     Render Dashboard       Render Inline
     (/admin View)          "ACCESS DENIED"
                            (Do NOT log out)
```

1. **Unauthenticated Access:** Any unauthenticated guest navigating to `/admin` is immediately intercepted and routed to `/login`.
2. **Customer Access (Non-Admin):** Authenticated users with role `CUSTOMER` navigating to `/admin` receive an inline **Access Denied** vitrine (*"Access Denied: Administrative privileges required."* with action link `← Back to Store`). The client **must not log the customer out**.
3. **HTTP 401 Unauthorized:** If the admin's JWT expires during an API request, the auth interceptor clears local token storage and navigates to `/login`.
4. **HTTP 403 Forbidden:** If an API returns 403, the client retains session state and displays the error message in-context.

---

## 3. Backend Verification & Data Binding Contracts

Every data point rendered on the Admin Dashboard maps directly to verified Spring Boot 3.2 controller endpoints:

| Component | Metric / Action | HTTP Method | Endpoint | Source Reference | Response Mapping | Operational Notes |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- |
| **Metric 1** | Total Products | `GET` | `/api/v1/products?size=1` | `ProductController.java:24-28` | `Page<ProductDto>.totalElements` | Reads `totalElements` field from Spring Data `Page`. Minimal network payload (`size=1`). |
| **Metric 2** | Active Categories | `GET` | `/api/v1/categories` | `CategoryController.java:21-24` | `List<CategoryDto>.size()` | Reads `list.size()` from active category list. |
| **Metric 3** | Registered Users | `GET` | `/api/v1/users` | `UserController.java:40-44` | `List<UserProfileDto>.size()` | Reads `list.size()` from user directory. Used solely for count calculation. No aggregate API created. |
| **Module Link 1** | Products Module | Direct Nav | `/admin/products` | UI Route | Navigates to `/admin/products` | Catalog & inventory management. |
| **Module Link 2** | Categories Module | Direct Nav | `/admin/categories` | UI Route | Navigates to `/admin/categories` | Taxonomy management. |
| **Module Link 3** | Users Module | Direct Nav | `/admin/users` | UI Route | Navigates to `/admin/users` | User directory & account management. |
| **Module Link 4** | Orders Module | Direct Nav | `/admin/orders` | UI Route | Navigates to `/admin/orders` | Order management & targeted lookup. |
| **Module Link 5** | Payments Module | Direct Nav | `/admin/payments` | UI Route | Navigates to `/admin/payments` | Payment lookup, details & refund. |

> **Quy định Tra Cứu:** Tra cứu Order và Payment được thực hiện trực tiếp trong module tương ứng (`/admin/orders` và `/admin/payments`). Dashboard tuyệt đối không chứa form tra cứu ID.

---

## 4. UI Architecture & Screen Anatomy

### 4.1 Canonical Stitch Screen Identification
* **Screen ID:** `1715c1a3aabf40d0bc69ee17987a5e73`
* **Title:** `TECHGEAR — Admin Dashboard`
* **Screenshot Artifact:** `projects/113306237264437644/files/e45862081e0b4f6ca74ddb6da171eb08`
* **HTML Source Artifact:** `projects/113306237264437644/files/89bdc6cade9e4faba87710ba46a493c5`
* **Target Viewport:** Desktop `1440px` (responsive 12-column grid, max canvas `max-w-7xl`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR // ADMIN  [OPERATIONAL PORTAL]   DASHBOARD  PRODUCTS  CATEGORIES  USERS  ORDERS  PAYMENTS │ admin@techgear.io [ADMIN]  LOG OUT │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PORTAL // OPERATIONAL OVERVIEW                                                                    │
│ ADMIN DASHBOARD                                                 [● SYSTEM ONLINE // LIVE APIS]   │
│ Operational overview, live entity totals, and module navigation.                                 │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 3 OPERATIONAL METRICS (ENTITY COUNT VITRINE)                                                     │
│ ┌─────────────────────────────┐ ┌─────────────────────────────┐ ┌──────────────────────────────┐ │
│ │ TOTAL PRODUCTS              │ │ ACTIVE CATEGORIES           │ │ REGISTERED USERS             │ │
│ │ 48                          │ │ 6                           │ │ 142                          │ │
│ │ Live items via /products    │ │ Active taxonomies via /cat  │ │ Total accounts via /users    │ │
│ └─────────────────────────────┘ └─────────────────────────────┘ └──────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ADMIN MODULES                                                                                    │
│ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ ┌───────────┐│
│ │ PRODUCTS         │ │ CATEGORIES       │ │ USERS            │ │ ORDERS           │ │ PAYMENTS  ││
│ │ Catalog & stock  │ │ Taxonomy trees   │ │ Directory accounts│ │ Targeted lookup  │ │ Details   ││
│ │ OPEN MODULE →    │ │ OPEN MODULE →    │ │ OPEN MODULE →    │ │ OPEN MODULE →    │ │ OPEN MOD →││
│ └──────────────────┘ └──────────────────┘ └──────────────────┘ └──────────────────┘ └───────────┘│
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ TECHGEAR OPERATIONAL ADMINISTRATION // V1.0                  ENDPOINT SCOPE: SPRING BOOT 3.2     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Detailed Component Specifications

### 5.1 Top Admin Navigation Bar (Sticky 64px)
* **Container:** `h-16 bg-white border-b border-[#E7E5E4] sticky top-0 z-40 px-8 flex items-center justify-between shadow-none`.
* **Left Brand Anchor:**
  * Wordmark: `TECHGEAR // ADMIN` (Plus Jakarta Sans `15px`, bold `#1C1917`, uppercase with JetBrains Mono `11px` `#78716C` separator).
  * Environment Badge: `OPERATIONAL PORTAL` (JetBrains Mono `10px`, font-medium, bg `#F5F5F4`, text `#57534E`, border `1px solid #E7E5E4`, px-2 py-0.5, rounded `[4px]`).
* **Center Navigation Links:**
  * `DASHBOARD` (`/admin`): **Active State** — font-semibold text `#1C1917`, border-b-2 border-[#1C1917] h-16 flex items-center px-3.
  * `PRODUCTS` (`/admin/products`): Inactive — text `#57534E` hover:text-[#1C1917] text-xs font-medium uppercase tracking-wider px-3.
  * `CATEGORIES` (`/admin/categories`): Inactive — text `#57534E` hover:text-[#1C1917] text-xs font-medium uppercase tracking-wider px-3.
  * `USERS` (`/admin/users`): Inactive — text `#57534E` hover:text-[#1C1917] text-xs font-medium uppercase tracking-wider px-3.
  * `ORDERS` (`/admin/orders`): Inactive — text `#57534E` hover:text-[#1C1917] text-xs font-medium uppercase tracking-wider px-3.
  * `PAYMENTS` (`/admin/payments`): Inactive — text `#57534E` hover:text-[#1C1917] text-xs font-medium uppercase tracking-wider px-3.
* **Right Utility Rail:**
  * Admin Email: `admin@techgear.io` in JetBrains Mono `12px` text `#1C1917`.
  * Role Badge: `[ADMIN]` in JetBrains Mono `10px` bg `#1C1917` text `#FAFAF9` px-1.5 py-0.5 rounded `[2px]`.
  * Log Out Action: `LOG OUT` button linking to `/login`, text `#78716C` hover:text-[#DC2626] text-xs font-mono uppercase transition-colors.

---

### 5.2 Header Station & Operational Telemetry
* **Breadcrumb:** `PORTAL // OPERATIONAL OVERVIEW` in JetBrains Mono `11px` uppercase tracking-wider text `#78716C` mb-2.
* **Title:** `ADMIN DASHBOARD` in Plus Jakarta Sans `28px` font-bold text `#1C1917` uppercase tracking-tight.
* **Subtitle:** `Live entity totals, operational jump stations, and system module navigation.` in Plus Jakarta Sans `14px` text `#57534E`.
* **System Status Vitrine:**
  * Pill container: bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[4px]`, px-3 py-1.5.
  * Live Status Indicator: 6px circular dot filled with Signal Amber `#D97706`.
  * Status Label: `SYSTEM ONLINE // BOUND TO LIVE APIS` in JetBrains Mono `11px` text `#1C1917` font-medium.

---

### 5.3 3 Operational Metrics (Entity Count Vitrine)

The metrics vitrine displays exactly 3 cards arranged in a 3-column responsive grid (`grid grid-cols-1 md:grid-cols-3 gap-6 mb-8`):

#### Card 1: Total Products
* **Container:** bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`, p-6, shadow-none.
* **Metric Header:**
  * Title: `TOTAL PRODUCTS` (JetBrains Mono `11px` uppercase tracking-wider text `#78716C`).
  * Endpoint Pill: `GET /products` (JetBrains Mono `10px` text `#78716C` bg `#F5F5F4` px-1.5 py-0.5 rounded `[2px]`).
* **Value Display:**
  * Tabular Numeral: `48` *(live bound from `Page<ProductDto>.totalElements`)*.
  * Typography: JetBrains Mono `36px` font-bold text `#1C1917` tracking-tight tabular-nums.
* **Operational Scope Note:**
  * `Active & archived catalog items via GET /products` in Plus Jakarta Sans `12px` text `#57534E` mt-2.

#### Card 2: Active Categories
* **Container:** bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`, p-6, shadow-none.
* **Metric Header:**
  * Title: `ACTIVE CATEGORIES` (JetBrains Mono `11px` uppercase tracking-wider text `#78716C`).
  * Endpoint Pill: `GET /categories` (JetBrains Mono `10px` text `#78716C` bg `#F5F5F4` px-1.5 py-0.5 rounded `[2px]`).
* **Value Display:**
  * Tabular Numeral: `6` *(live bound from `List<CategoryDto>.size()`)*.
  * Typography: JetBrains Mono `36px` font-bold text `#1C1917` tracking-tight tabular-nums.
* **Operational Scope Note:**
  * `Taxonomy classifications via GET /categories` in Plus Jakarta Sans `12px` text `#57534E` mt-2.

#### Card 3: Registered Users
* **Container:** bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`, p-6, shadow-none.
* **Metric Header:**
  * Title: `REGISTERED USERS` (JetBrains Mono `11px` uppercase tracking-wider text `#78716C`).
  * Endpoint Pill: `GET /users` (JetBrains Mono `10px` text `#78716C` bg `#F5F5F4` px-1.5 py-0.5 rounded `[2px]`).
* **Value Display:**
  * Tabular Numeral: `142` *(live bound from `List<UserProfileDto>.size()`)*.
  * Typography: JetBrains Mono `36px` font-bold text `#1C1917` tracking-tight tabular-nums.
* **Operational Scope Note:**
  * `Total customer & admin accounts via GET /users` in Plus Jakarta Sans `12px` text `#57534E` mt-2.
  * *Notice: Bound to `GET /api/v1/users` solely for count extraction. No backend aggregate API is created.*

---

### 5.4 Admin Modules

* **Section Header:**
  * Label: `ADMIN MODULES` in JetBrains Mono `11px` uppercase tracking-wider text `#78716C` mb-3.
* **Architecture:** Cung cấp đường dẫn điều hướng cố định đến 5 domain module nghiệp vụ của hệ thống (thay thế cho section lookup và recent activity).
* **Grid Layout:** 5 high-density cards in a responsive grid (`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-12`):

#### Products
* **Title:** `PRODUCTS` in Plus Jakarta Sans `16px` font-bold text `#1C1917`.
* **Description:** `Catalog inventory, stock adjustments (mode ABSOLUTE / DELTA), pricing, and active status toggles.` (Plus Jakarta Sans `13px` text `#57534E` leading-relaxed).
* **Endpoint Scope:** `GET /products | POST /products | PATCH /products/{id}` in JetBrains Mono `10px` text `#78716C` bg-[#F5F5F4] p-1.5 rounded `[2px]`.
* **Action Trigger:** `OPEN MODULE →` in JetBrains Mono `11px` font-semibold text `#1C1917` hover:text-[#D97706] transition-colors mt-4.
* **Target Route:** `/admin/products`.

#### Categories
* **Title:** `CATEGORIES` in Plus Jakarta Sans `16px` font-bold text `#1C1917`.
* **Description:** `Taxonomy management, metadata creation, and safe deletion integrity guarding against active product conflict.` (Plus Jakarta Sans `13px` text `#57534E` leading-relaxed).
* **Endpoint Scope:** `GET /categories | POST /categories | DELETE /categories/{id}` in JetBrains Mono `10px` text `#78716C` bg-[#F5F5F4] p-1.5 rounded `[2px]`.
* **Action Trigger:** `OPEN MODULE →` in JetBrains Mono `11px` font-semibold text `#1C1917` hover:text-[#D97706] transition-colors mt-4.
* **Target Route:** `/admin/categories`.

#### Users
* **Title:** `USERS` in Plus Jakarta Sans `16px` font-bold text `#1C1917`.
* **Description:** `User directory table, profile detail inspection, role verification, and permanent account removal safeguard.` (Plus Jakarta Sans `13px` text `#57534E` leading-relaxed).
* **Endpoint Scope:** `GET /users | GET /users/{id} | DELETE /users/{id}` in JetBrains Mono `10px` text `#78716C` bg-[#F5F5F4] p-1.5 rounded `[2px]`.
* **Action Trigger:** `OPEN MODULE →` in JetBrains Mono `11px` font-semibold text `#1C1917` hover:text-[#D97706] transition-colors mt-4.
* **Target Route:** `/admin/users`.

#### Orders
* **Title:** `ORDERS` in Plus Jakarta Sans `16px` font-bold text `#1C1917`.
* **Description:** `Targeted order lookup by ID or User ID, itemized purchase audit, order cancellation, and status transitions.` (Plus Jakarta Sans `13px` text `#57534E` leading-relaxed).
* **Endpoint Scope:** `GET /orders/{id} | GET /orders/user/{userId} | PATCH /orders/{id}/status` in JetBrains Mono `10px` text `#78716C` bg-[#F5F5F4] p-1.5 rounded `[2px]`.
* **Action Trigger:** `OPEN MODULE →` in JetBrains Mono `11px` font-semibold text `#1C1917` hover:text-[#D97706] transition-colors mt-4.
* **Target Route:** `/admin/orders`.
* **Operational Scope Note:** Toàn bộ tính năng tra cứu đơn hàng theo ID (Order Lookup) được thực hiện bên trong module này.

#### Payments
* **Title:** `PAYMENTS` in Plus Jakarta Sans `16px` font-bold text `#1C1917`.
* **Description:** `Payment lookup by payment ID, order ID, or user ID, payment details inspection, and refund processing with required reason.` (Plus Jakarta Sans `13px` text `#57534E` leading-relaxed).
* **Endpoint Scope:** `GET /payments/{id} | GET /payments/order/{orderId} | GET /payments/user/{userId} | POST /payments/refund` in JetBrains Mono `10px` text `#78716C` bg-[#F5F5F4] p-1.5 rounded `[2px]`.
* **Action Trigger:** `OPEN MODULE →` in JetBrains Mono `11px` font-semibold text `#1C1917` hover:text-[#D97706] transition-colors mt-4.
* **Target Route:** `/admin/payments`.
* **Operational Scope Note:** Toàn bộ tính năng tra cứu giao dịch theo ID (Payment Lookup) được thực hiện bên trong module này. Tuyệt đối không tạo giao diện hoặc endpoint cho "hóa đơn thanh toán" (không có invoice UI).

---

### 5.5 Footer Archival Bar
* **Container:** `border-t border-[#E7E5E4] pt-6 flex flex-col sm:flex-row justify-between items-center text-[#78716C] font-mono text-[11px]`.
* **Left Specification:** `TECHGEAR OPERATIONAL ADMINISTRATION // V1.0`.
* **Right Specification:** `ENDPOINT SCOPE: FROZEN SPRING BOOT 3.2 SERVICES`.

---

## 6. Client-Side State, Validation & Error Handling

### 6.1 Initial Load Lifecycle
1. Upon page mount, client triggers parallel asynchronous requests:
   * `GET /api/v1/products?size=1`
   * `GET /api/v1/categories`
   * `GET /api/v1/users`
2. **Skeleton Shimmer:** While pending, the 3 metric numeric displays render a minimal `h-9 w-16 bg-[#F5F5F4] rounded-[4px] animate-pulse` placeholder.
3. **Graceful Fallback:** If one service fails (e.g. `order-service` or `user-service` is temporarily down), the remaining metric cards display their numbers while the failing card displays `—` with an inline error tooltip (*"Service unreachable"*). The entire dashboard does not crash.

### 6.2 Phân Định Phạm Vi Tra Cứu (No Lookup Forms on Dashboard)
* **Loại bỏ Form Tra Cứu trên Dashboard:** Dashboard không chứa bất kỳ ô input tra cứu Order ID hay Payment ID nào.
* **Tra Cứu tại Module Tương Ứng:** Toàn bộ form tra cứu, input tìm kiếm và validation chuỗi ID được thực hiện độc lập bên trong `/admin/orders` và `/admin/payments`.

---

## 7. Design System Token Alignment

| Token Dimension | Value | Application Context |
| :--- | :--- | :--- |
| **Canvas Background** | `#FAFAF9` | Primary page underlay (Warm Alabaster). |
| **Surface Background** | `#FFFFFF` | Vitrine cards, header bar, action container (Pure Spec Surface). |
| **Milled Neutral Pad**| `#F5F5F4` | Inset endpoint pills, code blocks, secondary tags. |
| **Hairline Dividers** | `#E7E5E4` | 1px border on all cards, rules, and table dividers. |
| **Primary Solids** | `#1C1917` | Plus Jakarta Sans headings, primary button, tabular numerals. |
| **Secondary Typography**| `#57534E` | Module descriptions, subtitle prose. |
| **Metadata Typography** | `#78716C` | JetBrains Mono labels, endpoint paths, timestamps. |
| **Functional Signal** | `#D97706` | Signal Amber 6px online dot, focus outlines. |
| **Destructive Action** | `#DC2626` | Log out action hover state. |
| **Corner Radii** | `4px` / `6px` | `4px` on buttons, inputs, pills; `6px` on card containers. |
| **Elevation** | `shadow-none` | Absolutely flat surfaces; zero floating drop shadows. |

---

## 8. Anti-Slop & Quality Verification Checklist

- [x] **Canonical Route:** `/admin` verified.
- [x] **Actor Role:** Verified strictly as `ADMIN`.
- [x] **3 Operational Metrics:** Retained and grounded in live endpoints:
  - Product total (`Page<ProductDto>.totalElements` via `GET /products?size=1`)
  - Active category total (`List<CategoryDto>.size()` via `GET /categories`)
  - Registered user total (`List<UserProfileDto>.size()` via `GET /users`)
- [x] **Operational Overview Constraints Strictly Documented:**
  - Zero polling.
  - Zero periodic refresh.
  - Zero charts (no line/bar/pie/sparklines).
  - Zero historical analytics.
  - Zero trend calculation (no fake percentage badges).
  - Zero analytics aggregation.
  - `GET /api/v1/users` used solely for `size()`; no new aggregate API created.
- [x] **No Dashboard Lookup Forms:** Loại bỏ hoàn toàn `LOOKUP ORDER` và `LOOKUP PAYMENT` khỏi Dashboard; tra cứu Order/Payment thực hiện trong module tương ứng.
- [x] **Admin Modules Section:** Đổi tên từ `RECENT NAVIGATION` thành `ADMIN MODULES` (danh sách module cố định).
- [x] **No Pseudo-Technical Labels:** Loại bỏ ký hiệu trang trí `MODULE // 01`, sử dụng nhãn đơn giản: `PRODUCTS`, `CATEGORIES`, `USERS`, `ORDERS`, `PAYMENTS`.
- [x] **No Unsupported Payment Claims:** Loại bỏ mọi mô tả về "hóa đơn thanh toán"; chỉ mô tả payment lookup, payment details, và refund đã xác minh. Không tạo invoice UI.
- [x] **Zero Drawer References:** Cross-screen interactions navigate to dedicated pages.
- [x] **Stitch Canonical Screen:** `1715c1a3aabf40d0bc69ee17987a5e73` preserved.
- [x] **Code & Infrastructure Integrity:** Zero frontend code written, zero packages added, zero backend modifications, zero Git commands executed.
- [x] **FE-01 through FE-15 and ADMIN-01 Untouched:** Preserved completely.

---

**ADMIN-02 STATUS: READY FOR REVIEW**
