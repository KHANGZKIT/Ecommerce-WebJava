# FRONTEND ARCHITECTURE — TECHGEAR E-COMMERCE

> **Status:** APPROVED & FOUNDATIONAL  
> **Rule:** Frontend does not mirror the backend microservice structure.

---

## 1. Overview & Stack

The TECHGEAR frontend is a single, unified client application providing both customer-facing e-commerce experiences and administrative back-office management. 

### Technology Stack
- **Framework:** Next.js 16 (App Router)
- **Runtime / UI:** React 19
- **Language:** TypeScript 5 (Strict Mode)
- **Styling:** Tailwind CSS 4 (Custom Obsidian & Paper design token configuration)
- **HTTP Transport:** Native `fetch` API
- **Fonts:** Plus Jakarta Sans (Headings & Body), JetBrains Mono (Codes, IDs, Monospaced Metrics)

### Hardware & Resource Constraints
- Built to run efficiently in low-resource environments (e.g., laptops with 8GB RAM).
- Zero heavy client-side state frameworks (No Redux, Zustand, MobX, React Query, or Axios).
- Zero runtime UI bloatware (No MUI, Ant Design, Chakra UI, Bootstrap).
- Lightweight bundle sizes with native server rendering where appropriate.

---

## 2. Architectural Principles & Boundaries

### 2.1 The Gateway Principle
**Frontend does not mirror the backend microservice structure.**

The backend consists of multiple microservices (`user-service`, `product-service`, `order-service`, `payment-service`) orchestrated by Spring Cloud Gateway. The frontend treats the backend as a single unified REST API exposed exclusively via the API Gateway.

```
+-------------------------------------------------------------+
|                      TECHGEAR Frontend                      |
|                  (Next.js App Router Monolith)              |
+-------------------------------------------------------------+
                               |
                               | (All HTTP traffic via NEXT_PUBLIC_API_BASE_URL)
                               v
+-------------------------------------------------------------+
|              API Gateway (http://localhost:8080)            |
+-------------------------------------------------------------+
         |                     |              |              |
         v                     v              v              v
   user-service         product-service  order-service payment-service
      (4101)                (4102)          (4103)         (4104)
```

- **Forbidden:** Direct calls to internal microservice ports (`:4101`, `:4102`, `:4103`, `:4104`).
- **Required:** All calls go to `NEXT_PUBLIC_API_BASE_URL` (defaults to `http://localhost:8080`).

### 2.2 Backend vs. Frontend Authority Boundary
- **Backend is Authority:** Data persistence, inventory deduction, pricing calculations, role verification, status transitions, and business invariants are strictly enforced by the backend microservices.
- **Frontend is Presentation & Interaction:** Frontend validates form input for user convenience, renders state, handles navigation, and displays backend responses.
- **Backend is Closed:** No backend refactors or modifications are made to accommodate the frontend. When backend APIs impose constraints (e.g., unpaginated categories, active-only filters, lookup-by-ID only for payments), frontend strictly adapts to the audited contracts.

---

## 3. Directory Structure

The codebase is organized cleanly by architectural layers and frontend concern:

```
frontend/
├── app/                          # Next.js App Router (Routes & Layouts)
│   ├── layout.tsx                # Root layout (Fonts, Obsidian canvas, AuthProvider)
│   ├── globals.css               # Design tokens, typography & CSS variables
│   ├── page.tsx                  # Home (/)
│   ├── shop/                     # /shop
│   ├── products/[id]/            # /products/[id]
│   ├── category/[categoryId]/    # /category/[categoryId]
│   ├── search/                   # /search
│   ├── cart/                     # /cart
│   ├── checkout/                 # /checkout
│   ├── payment-result/           # /payment-result
│   ├── orders/                   # /orders
│   │   └── [orderId]/            # /orders/[orderId]
│   ├── profile/                  # /profile
│   ├── login/                    # /login
│   ├── register/                 # /register
│   └── admin/                    # Admin section wrapper (AdminGuard enforced)
│       ├── layout.tsx            # Admin layout shell (Header, Sidebar rail, Footer)
│       ├── page.tsx              # /admin (Dashboard)
│       ├── products/             # /admin/products
│       ├── categories/           # /admin/categories
│       ├── users/                # /admin/users
│       ├── orders/               # /admin/orders
│       └── payments/             # /admin/payments
├── components/                   # Shared UI & Layout components
│   ├── layout/                   # Header, Footer, CustomerShell, AdminHeader, AdminFooter
│   └── ui/                       # Button, Input, StatusBadge, FeedbackStates (Skeleton, Empty, Error)
├── features/                     # Feature-specific hooks, guards, and domain logic
│   └── auth/                     # AuthContext, AuthGuard, ProtectedRoute, AdminGuard
├── lib/                          # Utility & Networking layer
│   └── api/                      # Gateway HTTP client & domain API modules
│       ├── client.ts             # Core fetchApi wrapper (Bearer token, error normalization)
│       ├── auth.ts               # Auth endpoints (login, register, me)
│       ├── catalog.ts            # Products & categories
│       ├── cart.ts               # Redis-backed cart operations
│       ├── checkout.ts           # Order creation & Idempotency
│       ├── orders.ts             # Customer order tracking
│       ├── payments.ts           # Payment verification & lookup
│       ├── profile.ts            # User profile management
│       └── admin.ts              # Administrative CRUD & lookups
├── types/                        # Pure TypeScript API contracts & models
│   ├── auth.ts                   # Login/Register requests & AuthState
│   ├── catalog.ts                # ProductDto, CategoryDto, Page<T>, query params
│   ├── cart.ts                   # CartDto, CartItemDto
│   ├── order.ts                  # OrderDto, OrderItemDto, OrderStatus, Checkout contracts
│   ├── payment.ts                # PaymentResponse, PaymentStatus, RefundRequest
│   └── user.ts                   # UserProfileDto, UserRole
└── docs/                         # Specification & architecture documentation
```

---

## 4. Route Matrix

The application supports exactly 19 approved canonical routes:

### 4.1 Customer Routes (13)
| Route | Screen / Purpose | Auth Guard |
| :--- | :--- | :--- |
| `/` | FE-02 Home: Hero, featured categories, featured products | Public |
| `/shop` | FE-03 Shop: Product catalog with filtering, sorting & pagination | Public |
| `/products/[id]` | FE-04 Product Detail: Gallery, specs, inventory, add to cart | Public |
| `/category/[categoryId]` | FE-11 Category: Category-filtered catalog view | Public |
| `/search` | FE-10 Search: Keyword search results | Public |
| `/cart` | FE-05 Cart: Redis shopping cart view, quantity adjustments | Public / Authenticated |
| `/checkout` | FE-06 Checkout: Shipping details, summary, idempotent order creation | Protected (`CUSTOMER`) |
| `/payment-result` | FE-07 Payment Result: Gateway callback & transaction result status | Public / Authenticated |
| `/orders` | FE-08 My Orders: Customer order history list | Protected (`CUSTOMER`) |
| `/orders/[orderId]` | FE-09 Order Detail: Itemized receipt, shipping address & payment info | Protected (`CUSTOMER`) |
| `/profile` | FE-14 Profile: User profile view & edit | Protected (`CUSTOMER`) |
| `/login` | FE-12 Login: Credential authentication form | Guest only |
| `/register` | FE-13 Register: New account registration form | Guest only |

### 4.2 Admin Routes (6)
| Route | Screen / Purpose | Auth Guard |
| :--- | :--- | :--- |
| `/admin` | ADMIN-02 Dashboard: Metric cards, quick links & overview | Protected (`ADMIN`) |
| `/admin/products` | ADMIN-03 Products: Product table, create/edit modals, stock updates | Protected (`ADMIN`) |
| `/admin/categories` | ADMIN-04 Categories: Category table, create/edit modals | Protected (`ADMIN`) |
| `/admin/users` | ADMIN-05 Users: User table, detail modal, user deletion | Protected (`ADMIN`) |
| `/admin/orders` | ADMIN-06 Orders: Lookup by orderId/userId, detail & status updates | Protected (`ADMIN`) |
| `/admin/payments` | ADMIN-07 Payments: Lookup by paymentId/orderId/userId, refund actions | Protected (`ADMIN`) |

### 4.3 Strictly Forbidden Routes
The following routes must NOT be implemented:
- `/payment/result` (Use `/payment-result`)
- `/admin/analytics`
- `/admin/reports`
- `/admin/settings`
- `/admin/audit-log`

---

## 5. API Client & Networking Layer

### 5.1 Core Client (`lib/api/client.ts`)
- Implements `fetchApi<T>(endpoint, options)`.
- Base URL resolved from `process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'`.
- Automatically retrieves JWT token from `localStorage` (`techgear_token`) and injects `Authorization: Bearer <token>`.
- Converts query parameters into proper query strings.
- Normalizes HTTP error responses into structured `ApiError` instances:
  ```ts
  interface ApiError {
    message: string;
    status: number;
    errors?: Record<string, string>;
  }
  ```
- Gracefully handles 401 Unauthorized by removing expired tokens and dispatching an auth state notification.

### 5.2 Domain API Modules
All domain operations are encapsulated in thin helper modules without unnecessary abstraction:
- `lib/api/auth.ts`: Authentication (`/api/v1/auth/*`, `/api/v1/users/me`)
- `lib/api/catalog.ts`: Products and Categories (`/api/v1/products`, `/api/v1/categories`)
- `lib/api/cart.ts`: Cart endpoints (`/api/v1/cart`)
- `lib/api/checkout.ts`: Checkout endpoint (`/api/v1/orders/checkout`) forwarding `Idempotency-Key` when provided
- `lib/api/orders.ts`: Order operations (`/api/v1/orders`)
- `lib/api/payments.ts`: Payment verification & lookups (`/api/v1/payments`)
- `lib/api/profile.ts`: User profile updates (`/api/v1/users/*`)
- `lib/api/admin.ts`: Admin operations across all backend services

---

## 6. Authentication & Authorization Strategy

### 6.1 Token Storage (FRONTEND IMPLEMENTATION DECISION)
- **Implementation:** React Context (`features/auth/AuthContext.tsx`) + browser `localStorage` (`techgear_token` and `techgear_user_profile`).
- **Classification:** **FRONTEND IMPLEMENTATION DECISION**.
- **Important Distinction:** `localStorage` is chosen strictly as a pragmatic frontend implementation decision to maintain a lightweight, zero-dependency client footprint on local hardware. `localStorage` is **NOT** classified as a secure storage solution against physical or XSS vectors.
- **Authority:** The backend Spring Security filter chain and verified JWT verification remain the sole authoritative security boundary. No external auth library (e.g. NextAuth, Redux Auth) is installed.

### 6.2 Reactive Context (`features/auth/AuthContext.tsx`)
- Provides `useAuth()` hook.
- Exposes: `user`, `token`, `isAuthenticated`, `isAdmin`, `isLoading`, `login(credentials)`, `register(data)`, `logout()`, `refreshUser()`.
- Synchronizes changes to `localStorage` and cleans up state on 401 response.

### 6.3 Route Guards (`features/auth/AuthGuard.tsx`)
- `<ProtectedRoute>`: Restricts access to authenticated users (`/checkout`, `/orders`, `/orders/[id]`, `/profile`). Unauthenticated visitors are redirected to `/login?redirect=...`.
- `<AdminGuard>`: Restricts access to authenticated users with `role === 'ADMIN'`. Non-admin users are shown a clean 403 Forbidden message with a return button.
- Guarding in frontend is **UX protection only**; backend Spring Security filters remain authoritative.

### 6.4 Auth Scope Boundaries (YAGNI)
- **Supported:** Login, Register, Session context, Logout, Protected routes, Admin UX guard, 401 token clearing.
- **NOT Supported:** Refresh tokens, OAuth 2.0 / Social login, MFA / OTP, Password reset flow, Token blacklist, Remember-me tokens.

---

## 7. Design System Implementation

The frontend strictly implements the **Obsidian & Paper** industrial design aesthetic specified in `FE-DESIGN-SYSTEM.md`:

| Semantic Token | Color Value | Usage |
| :--- | :--- | :--- |
| **Canvas** | `#FAFAF9` (Stone-50) | Main background for pages |
| **Surface** | `#FFFFFF` (White) | Cards, panels, modals, dropdowns |
| **Raised** | `#F5F5F4` (Stone-100) | Secondary backgrounds, table headers, hover states |
| **Primary Text** | `#1C1917` (Stone-900) | High-contrast body text and headers |
| **Muted Text** | `#57534E` / `#78716C` (Stone-600/500) | Subtitles, helper text, timestamps |
| **Border** | `#E7E5E4` (Stone-200) | 1px clean separation lines |
| **Accent / Signal**| `#D97706` (Amber-600) | Primary CTAs, active indicators, highlights |
| **Destructive** | `#DC2626` (Red-600) | Cancel, Delete, Error alerts |

### Typography & Radii
- **Headings & Body:** Plus Jakarta Sans (`font-sans`), tight letter tracking (`tracking-tight`).
- **Data & Codes:** JetBrains Mono (`font-mono`), tabular numbers (`tabular-nums`) for currency and IDs.
- **Corner Radii:** Strict `rounded-[4px]` for controls and buttons; `rounded-[6px]` for cards and dialogs.
- **Shadows:** Restrained minimal borders with subtle shadows (`shadow-sm` maximum). No loud floating glow effects.

---

## 8. Shared Components Architecture

Components are created only when reused across multiple screens:
- `components/ui/Button.tsx`: Machine-cut button supporting `primary`, `secondary`, `outline`, `accent`, `destructive` variants, and loading states.
- `components/ui/Input.tsx`: Form inputs with consistent 4px borders, error messages, and label states.
- `components/ui/StatusBadge.tsx`: Badges for order statuses (`PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`), payment statuses (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`), and stock statuses.
- `components/ui/FeedbackStates.tsx`: Standardized `LoadingSkeleton`, `EmptyState`, and `ErrorState`.
- `components/layout/CustomerShell.tsx`: Standard wrapper binding `Header` and `Footer`.
- `components/layout/AdminHeader.tsx` & `AdminFooter.tsx`: Industrial header with `TECHGEAR — ADMIN` branding and section navigation.

---

## 9. Strict YAGNI (You Aren't Gonna Need It) Constraints

The following features and abstractions are explicitly prohibited from being added:
- No CMS or markdown engines.
- No analytics, telemetry, or user tracking scripts.
- No push notification systems.
- No recommendation engines.
- No wishlist functionality.
- No product reviews or ratings system.
- No coupon/discount code engines.
- No third-party shipping integrations.
- No inventory forecasting or analytics.
- No third-party payment gateway iframes or redirect SDKs (mock payment processing is backend-handled).
- No WebSockets or SSE subscriptions.
- No GraphQL layers.
- No microfrontend orchestration.
- No feature flag engines.
- No internationalization (i18n) frameworks.
- No theme engines or user dark-mode toggles.
- No plugin or dynamic extension architectures.

---

## 10. Sequential Implementation Plan

Following foundation completion, pages must be implemented in the exact sequential order below:

1. `Home` (`/`)
2. `Shop` (`/shop`)
3. `Product Detail` (`/products/[id]`)
4. `Login` (`/login`)
5. `Register` (`/register`)
6. `Profile` (`/profile`)
7. `Cart` (`/cart`)
8. `Checkout` (`/checkout`)
9. `Payment Result` (`/payment-result`)
10. `My Orders` (`/orders`)
11. `Order Detail` (`/orders/[orderId]`)
12. `Search` (`/search`)
13. `Category` (`/category/[categoryId]`)
14. `Admin Dashboard` (`/admin`)
15. `Admin Products` (`/admin/products`)
16. `Admin Categories` (`/admin/categories`)
17. `Admin Users` (`/admin/users`)
18. `Admin Orders` (`/admin/orders`)
19. `Admin Payments` (`/admin/payments`)

Each stage must build cleanly, typecheck without errors, and run on the standard development stack.
