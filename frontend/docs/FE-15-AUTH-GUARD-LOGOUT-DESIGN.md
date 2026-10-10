# TECHGEAR — FE-15 Auth Guard & Logout Design Specification

> **Document ID:** FE-15-AUTH-GUARD-LOGOUT-DESIGN  
> **Topic:** Authentication Guard, Session Lifecycle, Token Propagation & Client-Side Logout  
> **Target Scope:** Whole-Frontend Navigation & Security Boundary Specification  
> **Source Documents:** [FE-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-01-CATALOG-SITEMAP-FLOW.md), [FE-12](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-12-LOGIN-DESIGN.md), [FE-13](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-13-REGISTER-DESIGN.md), [FE-14](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-14-PROFILE-DESIGN.md)  
> **Backend Authority:** Spring Boot 3.2 Services (`api-gateway`, `user-service`, `order-service`, `payment-service`, `product-service`)  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Auth Guard & Logout Specification** (`FE-15`) establishes the definitive client-side security architecture for TECHGEAR. It governs how route access is granted or denied, how JWT bearer credentials propagate across API requests, how sessions terminate upon logout or expiration, and how error states (specifically HTTP 401 vs. HTTP 403) are resolved without exposing technical infrastructure failures to customers.

### Core Architecture Foundations
1. **Stateless JWT Session:** The backend issues cryptographically signed HS256 JWT tokens containing verified identity claims (`sub`, `userId`, `role`). There are no server-side HTTP sessions, session cookies, or remember-me tokens.
2. **Client-Side Logout:** The backend exposes **no logout endpoint** and maintains no token revocation blacklist. Logout is executed strictly on the client by destroying the active token and authenticated user state, then navigating to `/login`.
3. **Pragmatic Auth Guard:** Unauthenticated access to protected routes immediately navigates to `/login`. Upon successful authentication, users navigate to `/shop` (or resume their target destination if supported by client routing state).
4. **Distinct 401 vs. 403 Semantics:** HTTP 401 represents invalid or missing authentication and clears the session; HTTP 403 represents access forbidden to an authenticated user (role or resource ownership restriction) and **must never clear the session or trigger auto-logout**.

---

## 2. Source of Truth: Backend Audit & Grounding

Every security policy in this specification has been audited directly against active Spring Boot 3.2 backend source code:

| Component | Backend Source Reference | Verified Behavior & Architectural Grounding |
| :--- | :--- | :--- |
| **Auth Controller** | `user-service` / `AuthController.java:20-29` | Exposes only two endpoints: `POST /api/v1/auth/register` (201 Created) and `POST /api/v1/auth/login` (200 OK). **No `/logout` endpoint exists.** |
| **JWT Generation** | `user-service` / `UserService.java:55, 70` & `JwtService.java` | Packs `sub` (email), `userId` (Long), `role` (`CUSTOMER` or `ADMIN`), and expiration timestamp (`expiresIn = 86400000 ms` = 24 hours). |
| **User Security Config** | `user-service` / `SecurityConfig.java:33-58` | Stateless session policy (`SessionCreationPolicy.STATELESS`). Public: `/api/v1/auth/**`, `/actuator/**`, Swagger docs. Admin: `GET /api/v1/users`, `DELETE /api/v1/users/**`. All others: `authenticated()`. |
| **Gateway Auth Filter** | `api-gateway` / `AuthenticationFilter.java:38-75` | Validates `Authorization: Bearer <token>`. Checks signature, expiration date (`exp.after(now)`), and non-blank `subject`. Validates positive `userId`. Strips client spoof headers (`X-User-*`) and forwards verified identity downstream. Returns HTTP 401 for missing/invalid tokens. |
| **Order Security Config** | `order-service` / `OrderSecurityConfig.java:33-54` | Disables CSRF, basic auth, form login, and logout (`logout.disable()`). Stateless JWT filter (`OrderJwtAuthenticationFilter`). Protected: `/api/v1/orders/checkout`, `/api/v1/cart/**`, `GET /api/v1/orders/**`, `POST /api/v1/orders/*/cancel`. Admin-only: `PATCH /api/v1/orders/*/status`. |
| **Cart Controller** | `order-service` / `CartController.java:22-55` | Every endpoint requires `@AuthenticationPrincipal OrderPrincipal principal` (`principal.userId()`). Unauthenticated requests are rejected by security filter with HTTP 401. |
| **Order Controller** | `order-service` / `OrderController.java:45-155` | Validates `OrderPrincipal`. Checks IDOR ownership on single order `GET /api/v1/orders/{id}` and user orders `GET /api/v1/orders/user/{userId}`: non-matching user IDs trigger HTTP 403 Forbidden. |
| **Payment Security Config** | `payment-service` / `PaymentSecurityConfig.java:33-54` | Disables logout (`logout.disable()`). Protected: `GET /api/v1/payments/**`. Admin-only: `POST /api/v1/payments/refund`. Public internal charge: `POST /api/v1/payments/charge`. |
| **Disabled Account Handling** | `user-service` / `GlobalExceptionHandler.java:38-42` | Catches `DisabledException` on login &rarr; returns HTTP 403 with `{"status": 403, "message": "Account is disabled"}`. |

---

## 3. Classification: Backend-Confirmed vs. Frontend Design Decisions

| Aspect | Classification | Grounding & Rationale |
| :--- | :---: | :--- |
| **Missing/Expired JWT yields 401** | `BACKEND-CONFIRMED` | Gateway `AuthenticationFilter` and microservices security entrypoints reject missing or invalid tokens with HTTP 401. |
| **No Server-Side Logout Endpoint** | `BACKEND-CONFIRMED` | Audited `user-service`, `api-gateway`, `order-service`, and `payment-service`: zero logout endpoints exist; `logout.disable()` is explicitly set. |
| **No Token Blacklist / Revocation** | `BACKEND-CONFIRMED` | No Redis blacklist, database token table, or cache revocation exists. Verification is stateless cryptographic signature check. |
| **Role Claim (`CUSTOMER` vs `ADMIN`)** | `BACKEND-CONFIRMED` | `UserRole` enum in `user-service` has exactly two values: `CUSTOMER` and `ADMIN`. Claim is emitted in JWT and returned in `AuthResponse.user.role`. |
| **Forbidden Status (HTTP 403)** | `BACKEND-CONFIRMED` | Thrown when an authenticated user attempts to access another user's order or call an admin endpoint without `ADMIN` role. |
| **Client-Side Logout Flow** | `FRONTEND DESIGN DECISION` | Because backend exposes no logout endpoint, logout is handled on client by clearing local tokens and state, then navigating to `/login`. |
| **Auth Guard Route Interception** | `FRONTEND DESIGN DECISION` | Client-side routing interceptor checks token existence before rendering protected views and redirects unauthenticated users to `/login`. |
| **401 Session Clearance & Navigation** | `FRONTEND DESIGN DECISION` | Client HTTP interceptor clears credentials upon receiving HTTP 401 from any API and navigates user to `/login`. (Client choice, not backend redirect). |
| **Post-Login Default Destination (`/shop`)** | `FRONTEND DESIGN DECISION` | After login, user is routed to `/shop` (or target destination if in-memory routing state exists). No complex `?redirect=` query param engine is invented. |
| **Header Auth Toggle (Login vs Profile)** | `FRONTEND DESIGN DECISION` | Header dynamically shows `Login / Register` when unauthenticated, and `Orders / Profile / Logout` when authenticated. |

---

## 4. Route Protection Matrix

The following table defines the accessibility, backend grounding, and auth guard behavior for all TECHGEAR application routes:

| Route Path | Screen Name | Access Level | Backend API Contract | Unauthenticated Guard Action |
| :--- | :--- | :---: | :--- | :--- |
| `/` | Home (FE-02) | **PUBLIC** | `GET /api/v1/products` (newest), `GET /api/v1/categories` | Allow access. |
| `/shop` / `/products` | Shop Listing (FE-03) | **PUBLIC** | `GET /api/v1/products` (pageable, sortable) | Allow access. |
| `/products/:id` | Product Detail (FE-04) | **PUBLIC** | `GET /api/v1/products/{id}` | Allow access. |
| `/category/:categoryId` | Category Listing (FE-11) | **PUBLIC** | `GET /api/v1/products/category/{categoryId}` | Allow access. |
| `/search` | Search Results (FE-10) | **PUBLIC** | `GET /api/v1/products/search?keyword=` | Allow access. |
| `/login` | Customer Login (FE-12) | **PUBLIC (GUEST)** | `POST /api/v1/auth/login` | Allow access. (If already logged in, navigate to `/shop` or `/profile`). |
| `/register` | Customer Register (FE-13) | **PUBLIC (GUEST)** | `POST /api/v1/auth/register` | Allow access. (If already logged in, navigate to `/shop` or `/profile`). |
| `/cart` | Shopping Cart (FE-05) | **PROTECTED** | `GET /api/v1/cart`, `POST /api/v1/cart/items`, etc. (Requires `userId`) | Intercept &rarr; navigate `/login`. |
| `/checkout` | Checkout (FE-06) | **PROTECTED** | `POST /api/v1/orders/checkout` (Requires Bearer JWT) | Intercept &rarr; navigate `/login`. |
| `/payment-result` | Payment Result (FE-07) | **PROTECTED** | Post-checkout context in router memory. | State-driven page after checkout. If direct visit without checkout result state, handle per FE-07 flow (navigate `/orders` or `/shop`). |
| `/orders` | My Orders (FE-08) | **PROTECTED** | `GET /api/v1/orders/user/{userId}` (Requires Bearer JWT) | Intercept &rarr; navigate `/login`. |
| `/orders/:orderId` | Order Detail (FE-09) | **PROTECTED** | `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel` | Intercept &rarr; navigate `/login`. |
| `/profile` | Customer Profile (FE-14) | **PROTECTED** | `GET /api/v1/users/me`, `PATCH /api/v1/users/me` | Intercept &rarr; navigate `/login`. |
| `/admin/*` | Admin Portal (FE-01 Sitemap) | **ROLE-GUARDED (ADMIN)** | Admin endpoints (`/users`, `/orders/*/status`, `/payments/refund`) | If not ADMIN &rarr; render Access Denied (do not show admin UI). |

> [!NOTE]
> **Cart Authentication Requirement:**
> While some e-commerce systems allow anonymous carts, the TECHGEAR backend architecture (`order-service`/`CartController.java`) binds all cart operations directly to `principal.userId()`, and API Gateway enforces `requireUserId: true`. Therefore, `/cart` is strictly a **PROTECTED** route requiring authentication.

---

## 5. JWT Token Propagation & Lifecycle

### 5.1 Token Format & Verification
* **Algorithm:** HMAC-SHA256 (`HS256`).
* **Secret Key:** Shared 256-bit secret key across microservices (`JWT_SECRET`).
* **Payload Claims:**
  * `sub`: Customer email (e.g. `"jane.smith@example.com"`).
  * `userId`: Database customer ID (`Long`, e.g. `41`).
  * `role`: User authority (`"CUSTOMER"` or `"ADMIN"`).
  * `iat`: Issued-at timestamp.
  * `exp`: Expiration timestamp (Current time + 86,400,000 ms = 24 hours).
* **Response Wrapper (`AuthResponse`):**
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 41,
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@example.com",
      "phone": "+84901234567",
      "role": "CUSTOMER",
      "enabled": true,
      "createdAt": "2026-03-01T10:00:00",
      "updatedAt": "2026-03-15T12:00:00"
    }
  }
  ```

### 5.2 Outgoing Request Header Propagation
On all requests directed to protected endpoints (`/api/v1/cart/**`, `/api/v1/orders/**`, `/api/v1/users/**`, `/api/v1/payments/**`):
* Client must attach the exact HTTP header:
  ```http
  Authorization: Bearer <accessToken>
  ```
* **Header Hygiene Rules:**
  * Exactly one `Authorization` header must be sent (multiple headers return 401).
  * Prefix must be exactly `"Bearer "` (case-sensitive, with single space).
  * Client must **never** send `X-User-Id`, `X-User-Email`, `X-User-Role`, or `X-Auth-Token` headers. The API Gateway explicitly strips all client-supplied `X-User-*` headers and populates them downstream only after verifying the JWT.

---

## 6. Auth Guard Behavior

The client-side Auth Guard serves as an immediate UX gate before protected views are rendered.

```
                  ┌───────────────────────────────┐
                  │ Customer navigates to Route   │
                  └──────────────┬────────────────┘
                                 │
                     Is route in Protected List?
                                 │
                   ┌─────────────┴─────────────┐
                  YES                          NO
                   │                            │
         Has valid auth token?             Allow Route
                   │                       Render (Public)
         ┌─────────┴─────────┐
        YES                  NO
         │                    │
    Check Token Expiry    Navigate to /login
         │
    ┌────┴────┐
  VALID    EXPIRED
    │         │
  Allow    Clear Auth State
  Route       │
  Render   Navigate to /login
```

### 6.1 Route Guard Rules
1. **Authenticated Access:** If the user possesses a non-expired auth token, protected routes (`/cart`, `/checkout`, `/orders`, `/orders/:id`, `/profile`) render normally.
2. **Unauthenticated Access:** If no auth token is present in client state, navigation to any protected route is aborted and the user is redirected to `/login`.
3. **No Infrastructure Errors:** The user must never encounter a raw JSON error (`{"error":"Missing Authorization header","status":401}`) or browser network crash. The guard intercepts the request client-side before any network roundtrip occurs.

---

## 7. Logout Behavior (Client-Side)

### 7.1 Backend Fact Statement
> **"Logout is client-side because current backend does not expose a logout endpoint."**

* The backend services are strictly stateless.
* `OrderSecurityConfig.java:39` and `PaymentSecurityConfig.java:39` explicitly invoke `logout(logout -> logout.disable())`.
* There is no server-side token revocation table, Redis blocklist, or OAuth revocation endpoint.

### 7.2 Logout Execution Flow
When the customer clicks the `LOG OUT` action:

```
[ Customer Clicks "LOG OUT" ]
       │
       ▼
[ Step 1: Clear Client Token ]
  • Remove accessToken from client storage
       │
       ▼
[ Step 2: Clear Authenticated User State ]
  • Clear user profile object in memory
  • Reset Cart item count indicator to 0
       │
       ▼
[ Step 3: Client Navigation ]
  • Navigate customer directly to /login
```

* **No Synthetic Network Calls:** The client does **not** dispatch a fabricated `POST /logout` or `POST /api/v1/auth/logout`.
* **Immediate Transition:** State clearance and navigation happen synchronously.

---

## 8. Error & Edge Case Specifications

The frontend must handle edge cases deterministically according to this taxonomy:

### 8.1 Missing Token
* **Scenario:** Guest directly pastes URL to `/profile`, `/orders`, `/orders/105`, or `/checkout`.
* **Guard Action:** Intercepted prior to fetch; immediately navigate to `/login`.

### 8.2 Expired or Malformed Token in Storage
* **Scenario:** User revisits application after the 24-hour expiration window (`expiresIn` elapsed), or token payload is malformed.
* **Guard Action:**
  1. Detect expiration via timestamp check or failed initial fetch.
  2. Clear invalid token and user state.
  3. Navigate to `/login`.

### 8.3 HTTP 401 API Response (Unauthorized)
* **Scenario:** An API call dispatched to backend returns HTTP 401 Unauthorized (`"Invalid or expired JWT token"`, `"Missing Authorization header"`, or `"Token lacks userId"`).
* **Interceptor Action:**
  1. Recognize that server has rejected the session credentials.
  2. Clear local auth token and user state.
  3. Navigate customer to `/login`.
  4. Display inline contextual notice on the login screen if appropriate: `"Session expired. Please log in again."`.

### 8.4 HTTP 403 API Response (Forbidden)
* **CRITICAL RULE: DO NOT AUTO-LOGOUT ON 403.**
* **Scenario 1 — Resource Ownership Mismatch (IDOR Protection):**
  * Customer 41 attempts to view Order 99 belonging to Customer 102 (`GET /api/v1/orders/99`).
  * Backend returns HTTP 403: `{"status": 403, "message": "Access denied to order 99"}`.
  * **Handling:** Token is valid; user remains authenticated. Render an inline "Access Denied" state on `/orders/:id`:
    > *"Access Denied. You do not have permission to view this order."*  
    > Link: `← Back to My Orders` (`/orders`).
* **Scenario 2 — Insufficient Permissions (Customer attempting Admin endpoint):**
  * Authenticated customer attempts an admin action (e.g. updating order status or payment refund).
  * Backend returns HTTP 403: `{"status": 403, "message": "Access denied"}`.
  * **Handling:** User remains authenticated. Display permission error banner; **do not log the customer out**.
* **Scenario 3 — Disabled Account:**
  * Login attempt for a disabled account returns HTTP 403 (`"Account is disabled"`).
  * **Handling:** Render error directly on login form: `"This account has been disabled."` (FE-12).

### 8.5 HTTP 409 Conflict (Idempotency Key Mismatch)
* **Scenario:** Customer submits checkout with a reused idempotency key with conflicting payload.
* **Handling:** Retain auth state. Render inline form error informing user to refresh or retry checkout.

---

## 9. Redirect & Post-Authentication Navigation

To avoid fragile redirect engines and unnecessary complexity:

* **Unauthenticated Protected Access:**
  * Navigates directly to `/login`.
* **Post-Login Default Destination:**
  * Default destination upon successful login is `/shop`.
* **Preserving Target Destination:**
  * If the frontend application architecture supports an in-memory route navigation state, it may restore the intended destination (e.g. `/checkout` or `/profile`) after login.
  * The frontend **does not invent or rely on an unverified query string engine** (such as `?redirect=/orders`) unless explicitly implemented in the routing codebase.

---

## 10. Customer vs. Admin Role Boundaries

* **Role Authority:**
  * The backend is the sole security authority. Roles are defined by `UserRole`: `CUSTOMER` and `ADMIN`.
  * The client receives `user.role` from `AuthResponse`.
* **Customer Scope:**
  * Access to storefront, catalog, cart, checkout, my orders, order detail, and personal profile.
* **Admin Scope:**
  * Restricted admin capabilities (`GET /api/v1/users`, `DELETE /api/v1/users/{id}`, `PATCH /api/v1/orders/*/status`, `POST /api/v1/payments/refund`).
* **No Premature Admin UI:**
  * FE-15 does not create or specify admin screens.
  * Customers attempting to navigate to conceptual `/admin/*` routes must receive an Access Denied view, backed by Spring Security's `hasRole("ADMIN")` rejection.

---

## 11. Auth State Storage & Current Implementation Status

### 11.1 Frontend Codebase Audit Status
* **Audit Finding:** The `frontend/` directory currently contains **only design and specification documents** (`frontend/docs/FE-01` through `FE-14`).
* There is **no active React, Next.js, Vue, or JavaScript runtime code** in the repository.
* There is **no state management library** installed (no Redux, Zustand, React Context, or Pinia).

### 11.2 State Storage Requirements (Specification Only)
Because no frontend framework is yet initialized, this document defines **requirements**, not premature library selections:
1. **Token Persistence:** The authenticated state must persist across page refreshes during an active session (e.g., standard browser storage chosen during implementation).
2. **State Envelope:** The auth state must store:
   * `accessToken` (`String`)
   * `tokenType` (`"Bearer"`)
   * `expiresIn` (`Long`, 86400000)
   * `user` (`UserProfileDto`: `id`, `firstName`, `lastName`, `email`, `phone`, `role`, `enabled`)
3. **Atomic Purge:** The auth state must provide an atomic reset mechanism that clears both token and user profile simultaneously upon logout or 401 response.

---

## 12. UI Impact on Shared Navigation Header

The global navigation header reflects the customer's authentication status using the minimal, high-contrast Obsidian & Paper design language:

### 12.1 Unauthenticated Header State (Guest)
```
┌────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR               SHOP                  LOGIN   REGISTER      [0] │
└────────────────────────────────────────────────────────────────────────┘
```
* **Brand Wordmark:** `TECHGEAR` &rarr; links to `/`.
* **Commerce Rail:** `SHOP` &rarr; links to `/shop`.
* **Auth Links:**
  * `LOGIN` &rarr; links to `/login` (JetBrains Mono `12px`, uppercase, `#57534E`, hover `#1C1917`).
  * `REGISTER` &rarr; links to `/register` (JetBrains Mono `12px`, uppercase, `#1C1917`, font-semibold).
* **Cart Indicator:** `[0]` &rarr; links to `/cart` (prompts login upon navigation).

### 12.2 Authenticated Header State (Customer)
```
┌────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR        SHOP   ORDERS   PROFILE   LOG OUT                  [2] │
└────────────────────────────────────────────────────────────────────────┘
```
* **Brand Wordmark:** `TECHGEAR` &rarr; links to `/`.
* **Commerce Rail:**
  * `SHOP` &rarr; links to `/shop`.
  * `ORDERS` &rarr; links to `/orders`.
  * `PROFILE` &rarr; links to `/profile`.
  * `LOG OUT` &rarr; triggers client-side logout flow (JetBrains Mono `12px`, uppercase, `#78716C`, hover `#1C1917`).
* **Cart Indicator:** `[itemCount]` &rarr; live count of active cart items from `CartDto.itemCount`.

---

## 13. Strict YAGNI Exclusions

In strict compliance with project principles, the following features are **explicitly excluded**:

* **No Refresh Token Flow:** Session is governed strictly by the 24-hour `accessToken`.
* **No Server-Side Token Blacklist:** No Redis token blacklisting or revocation storage.
* **No OAuth / Social Logins:** No Google, Apple, or GitHub single sign-on buttons.
* **No Multi-Factor Authentication (MFA / 2FA):** No SMS OTP or authenticator apps.
* **No Password Reset / Forgot Password:** No backend endpoints exist for self-service resets.
* **No Device Management / Active Sessions:** No device fingerprinting or multi-device revocation.
* **No Remember Me:** No persistent cookies or refresh tokens.
* **No Auth Telemetry Dashboard:** No security log inspectors or fake audit telemetry.
* **No Backend Changes:** All behaviors leverage existing frozen microservices APIs.

---

## 14. Final Verification Checklist

- [x] Public routes verified (`/`, `/shop`, `/products/:id`, `/category/:id`, `/search`, `/login`, `/register`)
- [x] Protected routes verified (`/cart`, `/checkout`, `/payment-result`, `/orders`, `/orders/:id`, `/profile`)
- [x] JWT behavior verified (`HS256`, 24h expiration, `sub`, `userId`, `role` claims)
- [x] HTTP 401 verified (Missing/invalid token yields 401; frontend clears auth state &rarr; `/login`)
- [x] HTTP 403 verified (Does NOT auto-logout; renders inline Access Denied)
- [x] Token expiry verified (Client-side expiry check triggers auth purge &rarr; `/login`)
- [x] Logout endpoint absence verified (`user-service` has no logout API; Spring Security disables logout)
- [x] Logout behavior documented (Purely client-side token/state purge &rarr; `/login`)
- [x] Auth guard documented (Client navigation interceptor for protected routes)
- [x] No fake redirect engine invented (Default `/shop`, preserves existing in-memory state if available)
- [x] No invented auth storage architecture (Documented as requirements; no premature library choices)
- [x] Customer vs Admin distinction verified (`UserRole` enum; backend enforces role boundaries)
- [x] No scope creep (No MFA, refresh tokens, blacklists, or OAuth)
- [x] Documentation created (`frontend/docs/FE-15-AUTH-GUARD-LOGOUT-DESIGN.md`)
- [x] FE-01 &rarr; FE-14 unchanged
- [x] No code implementation, packages, backend, Docker, or Git commits

---

**FE-15 STATUS: READY FOR REVIEW**
