# TECHGEAR — FE-14 Customer Profile Design Specification

> **Document ID:** FE-14-PROFILE-DESIGN  
> **Route:** `/profile`  
> **Target Audience:** Authenticated Customers  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `7037d9ffc5b24cc0bba7fc694ffad21c`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Customer Profile** screen (`/profile`) provides the account management surface for authenticated TECHGEAR customers. It allows customers to view their verified account identity metadata, read-only system credentials, and update their personal profile attributes (`firstName`, `lastName`, and contact `phone`).

### Strict YAGNI Boundaries
* **Strict Backend Alignment:** Only fields and capabilities provided by `user-service` are rendered:
  * Fetch profile via `GET /api/v1/users/me`.
  * Update profile via `PATCH /api/v1/users/me` with `UpdateProfileRequest`.
* **Zero Speculative Account Controls:**
  * **No Avatar Upload:** No image upload widgets, crop tools, or CDN avatar URLs.
  * **No Password Change / Reset:** No password update forms or security reset links.
  * **No Email Modification:** Email address is permanently bound to the authentication credential and cannot be modified via profile endpoints.
  * **No Verification Gates:** No OTP, SMS verification, or email re-verification mechanisms.
  * **No Address Book:** Shipping addresses are managed in the checkout context (`/checkout`), not in profile.
  * **No Payment Methods / Wallets:** Stored credit cards and payment gateways are omitted.
  * **No Order Duplication:** Order history lives exclusively on `/orders` (FE-08).
  * **No Auxiliary Consumer Features:** No wishlists, notification toggles, marketing preferences, loyalty points/tiers, social logins, or account deletion buttons.
* **No AI-Slop & Fake Badges:** Zero pseudo-telemetry server metrics, zero fake encryption ribbons, and zero diffuse floating drop shadows.

---

## 2. Route & Navigation Architecture

* **Canonical Route:** `/profile`
* **Top Navigation Bar (Sticky 64px Shared Commerce Nav):**
  * **Brand Wordmark:** `TECHGEAR` links to homepage (`/`).
  * **Commerce Rail:**
    * `SHOP` &rarr; `/shop`
    * `ORDERS` &rarr; `/orders`
    * `PROFILE` &rarr; `/profile` (Active state: bold text with `#1C1917` active border indicator)
  * **Utility Rail:** Cart indicator `[0]` &rarr; `/cart`.
* **Breadcrumb Hierarchy:**
  * `ACCOUNT / PROFILE` (JetBrains Mono `11px`, `#78716C`, uppercase tracking-wider).
* **Authentication Requirement & 401 Classification:**
  * Protected route requiring valid JWT `accessToken` in the `Authorization` header.
  * **BACKEND-CONFIRMED:**
    * Unauthenticated / invalid authentication → HTTP 401.
    * Backend and API Gateway reject missing, invalid, or expired credentials strictly with HTTP 401 Unauthorized. The backend does NOT issue an HTTP redirect to `/login`.
  * **UI DESIGN DECISION:**
    * Frontend handles 401 by navigating to `/login`.
    * Client-side auth interceptor clears local token storage upon encountering 401 and navigates to the login screen. Redirection to `/login` is purely a frontend design decision, never a backend behavior.

---

## 3. Backend Verification & Integration Contract

### 3.1 Verified Backend Source Code References

| Component | Code Reference | Implementation Detail |
| :--- | :--- | :--- |
| **User Controller (Get Profile)** | `UserController.java:23-28` | `GET /api/v1/users/me`<br>Extracts `@AuthenticationPrincipal UserDetails userDetails`.<br>Calls `userService.getUserByEmail(userDetails.getUsername())`.<br>Returns HTTP 200 with `UserProfileDto`. |
| **User Controller (Update Profile)** | `UserController.java:30-37` | `PATCH /api/v1/users/me`<br>Accepts `@Valid @RequestBody UpdateProfileRequest request`.<br>Extracts authenticated user email, retrieves entity ID, calls `userService.updateProfile(current.getId(), request)`.<br>Returns HTTP 200 with updated `UserProfileDto`. |
| **User Service (Update Logic)** | `UserService.java:99-110` | Finds user by ID.<br>Updates `firstName`, `lastName`, and `phone` if present.<br>Saves updated user and emits audit log.<br>Returns `toProfileDto(updated)`. |
| **Entity Constraints** | `User.java:12-15, 40` | `uk_users_email` (unique email), `uk_users_phone` (unique phone), `role` (`CUSTOMER`), `enabled` (`true`), `createdAt` (audited), `updatedAt` (audited). |
| **Security Configuration** | `SecurityConfig.java:46` | `.anyRequest().authenticated()`. Stateless session, JWT filter before auth filter. |
| **Gateway Routing** | `application.yml:40-51` | Route `user-service-users` protects `/api/v1/users/**` via `AuthenticationFilter` and user-level rate limiting (`RequestRateLimiter`). |
| **Exception Handling** | `GlobalExceptionHandler.java:44-58` | Catches `MethodArgumentNotValidException` &rarr; HTTP 400 with `errors` map. |

### 3.2 Fetch Contract (`GET /api/v1/users/me`)

* **Headers:**
  ```http
  Authorization: Bearer <accessToken>
  ```
* **Response Payload (`UserProfileDto` — HTTP 200 OK):**
  ```json
  {
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
  ```

### 3.3 Update Contract (`PATCH /api/v1/users/me`)

* **Headers:**
  ```http
  Authorization: Bearer <accessToken>
  Content-Type: application/json
  ```
* **Request Payload (`UpdateProfileRequest`):**
  ```json
  {
    "firstName": "Jane",
    "lastName": "Smith",
    "phone": "+84901234567"
  }
  ```
* **Validation Constraints (`UpdateProfileRequest.java`):**
  * `firstName`:
    * Optional/nullable in request.
    * `@Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")`
    * `@Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s\\-']+$", message = "First name must contain at least one letter and can only include letters, spaces, hyphens, and apostrophes")`
  * `lastName`:
    * Optional/nullable in request.
    * `@Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")`
    * `@Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s\\-']+$", message = "Last name must contain at least one letter and can only include letters, spaces, hyphens, and apostrophes")`
  * `phone`:
    * Optional/nullable in request.
    * `@Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Must be a valid phone number")`
* **Response Payload:** Updated `UserProfileDto` (HTTP 200 OK).

### 3.4 Customer UI Display vs. Backend DTO Contract Matrix (Simplified Profile Specs)

> **Boundary Directive (Point 3 - Simplify Profile Account Specs):** Do NOT turn Profile into an account or system dashboard.
> The customer UI intentionally omits administrative attributes:
> - `ACCOUNT ROLE`
> - `ACCOUNT STATUS`
> - `ID`
> - `LAST MODIFIED`
>
> It preserves only essential customer information:
> - `Name` (`firstName` + `lastName`)
> - `Email` (`email` — read-only)
> - `Member Since` (`createdAt`)
>
> The backend DTO (`UserProfileDto`) remains completely unchanged. No new features, no new screens, and FE-01 → FE-13 remain untouched.

| Field | DTO Origin | Modifiability | Customer UI Status & Rationale |
| :--- | :--- | :---: | :--- |
| `firstName` | `UpdateProfileRequest` | **EDITABLE** | Retained. Standard rectangular input, 4px radius, validated 2–100 chars. |
| `lastName` | `UpdateProfileRequest` | **EDITABLE** | Retained. Standard rectangular input, 4px radius, validated 2–100 chars. |
| `phone` | `UpdateProfileRequest` | **EDITABLE** | Retained. Standard rectangular input, validated E.164 phone regex. |
| `email` | `UserProfileDto.email` | **READ-ONLY** | Retained (`Email`). Primary login identity. Immutable via `PATCH /api/v1/users/me`. Displayed with `IMMUTABLE` badge. |
| `createdAt` | `UserProfileDto.createdAt` | **READ-ONLY** | Retained (`Member Since`). Formatted as `DD MMM YYYY` (e.g. `01 MAR 2026`). |
| `id` | `UserProfileDto.id` | **OMITTED** | Omitted from customer UI (`ID: 0041`). Internal database identifier not required for customer self-service. Backend DTO unchanged. |
| `role` | `UserProfileDto.role` | **OMITTED** | Omitted from customer UI (`ACCOUNT ROLE: CUSTOMER`). System authorization role not required for customer self-service. Backend DTO unchanged. |
| `enabled` | `UserProfileDto.enabled` | **OMITTED** | Omitted from customer UI (`ACCOUNT STATUS: ACTIVE`). Session validity implies active status; avoids pseudo-admin status badge. Backend DTO unchanged. |
| `updatedAt` | `UserProfileDto.updatedAt` | **OMITTED** | Omitted from customer UI (`LAST MODIFIED: 15 MAR 2026, 12:00`). Internal audit timestamp not relevant to customer self-service. Backend DTO unchanged. |

---

## 4. Architectural Verification: Grounded vs. Design Decisions vs. Mock Data

| Aspect | Classification | Grounding & Rationale |
| :--- | :--- | :--- |
| **Profile Endpoints** | `BACKEND-CONFIRMED` | `GET /api/v1/users/me` and `PATCH /api/v1/users/me` defined in `UserController.java:24, 31`. |
| **Editable Field Scope** | `BACKEND-CONFIRMED` | Only `firstName`, `lastName`, and `phone` exist in `UpdateProfileRequest.java`. |
| **Immutability of Email** | `BACKEND-CONFIRMED` | `UpdateProfileRequest` has no email field; `UserService.updateProfile` does not alter email. |
| **Unauthenticated Status (HTTP 401)** | `BACKEND-CONFIRMED` | Unauthenticated / invalid authentication → HTTP 401. Backend & API Gateway strictly return 401; backend does NOT issue an HTTP redirect to `/login`. |
| **Validation Rules** | `BACKEND-CONFIRMED` | Exact Jakarta validation annotations on `UpdateProfileRequest`. |
| **401 Navigation Handling** | `UI DESIGN DECISION` | Frontend handles 401 by navigating to `/login`. Client interceptor catches 401, clears local auth state, and navigates to the login screen. (Client navigation decision, NOT backend redirect). |
| **Simplify Profile Account Specs** | `UI DESIGN DECISION` | Do not turn Profile into an account/system dashboard. Omit `ACCOUNT ROLE`, `ACCOUNT STATUS`, `ID`, and `LAST MODIFIED` from customer UI. Keep essential customer info (`Name`, `Email`, `Member Since`). Backend DTO unchanged. |
| **Two-Column Editorial Card Layout** | `UI DESIGN DECISION` | Compact, pragmatic account page separating customer identity (Left) from personal details form (Right). |
| **Monogram Identity Avatar (`JS`)** | `UI DESIGN DECISION` | Text-derived customer initials in 44px square neutral box; rejects external avatar uploads. |
| **Stitch Sample Mock Data** | `DESIGN MOCK DATA` | Visual values displayed in Stitch screen `7037d9ffc5b24cc0bba7fc694ffad21c`:<br>• `ID: 0041`<br>• `Jane Smith`<br>• `01 MAR 2026`<br>• `15 MAR 2026, 12:00`<br>are **strictly DESIGN MOCK DATA**. In documentation and runtime, the application MUST bind actual live data from `UserProfileDto`. Under no circumstances are these sample values hard-coded. |

---

## 5. Visual Direction & Design System

The Customer Profile screen adheres strictly to the **Obsidian & Paper** design system:

* **Canvas & Surface:**
  * Background: `#FAFAF9` (Warm Alabaster).
  * Card Containers: `#FFFFFF` (Pure Spec Surface) with hairline `1px solid #E7E5E4` borders and `6px` radius (`rounded-[6px]`).
  * Insets & Tags: `#F5F5F4` (Milled Neutral) for identity badge, status tag backing, and disabled inputs.
* **Typography:**
  * **Plus Jakarta Sans:** Governs page title, section headers, customer name, input labels, input values, and primary actions.
  * **JetBrains Mono:** Dedicated to breadcrumbs, technical metadata labels, timestamps, role tags, and the account identifier badge.
* **Colors:**
  * `#1C1917`: Primary typography, field labels, and `SAVE CHANGES` button.
  * `#57534E`: Subtitles and body guidance.
  * `#78716C`: Monospaced labels, inactive fields, and helper text.
  * `#E7E5E4`: Hairline framing dividers.
  * `#D97706`: Signal amber indicator for `ACTIVE` account status and focused input ring.

---

## 6. Screen Layout & Component Anatomy

```
┌────────────────────────────────────────────────────────────────────────┐
│ HEADER: TECHGEAR            SHOP   ORDERS   PROFILE [Active]       [0] │
├────────────────────────────────────────────────────────────────────────┤
│ ACCOUNT / PROFILE                                                      │
│ PROFILE                                                                │
│ Manage your personal details and account contact information.          │
│ ────────────────────────────────────────────────────────────────────── │
│                                                                        │
│ ┌───────────────────────────┐ ┌──────────────────────────────────────┐ │
│ │ LEFT: CUSTOMER IDENTITY   │ │ RIGHT: PERSONAL INFORMATION & EDIT   │ │
│ │ (lg:col-span-4, bg #FFF)  │ │ (lg:col-span-8, bg #FFF, r-6px)      │ │
│ │                           │ │                                      │ │
│ │ [JS]                      │ │ PERSONAL INFORMATION                 │ │
│ │ Jane Smith                │ │ Update your name and phone number... │ │
│ │ jane.smith@example.com    │ │                                      │ │
│ │ ───────────────────────── │ │ FIRST NAME         LAST NAME         │ │
│ │ MEMBER SINCE   01 MAR 2026│ │ [ Jane         ]   [ Smith        ]  │ │
│ └───────────────────────────┘ │                                      │ │
│                               │ EMAIL ADDRESS (READ-ONLY) [IMMUTABLE]│ │
│                               │ [ jane.smith@example.com           ] │ │
│                               │ Email is tied to authentication...   │ │
│                               │                                      │ │
│                               │ PHONE NUMBER                         │ │
│                               │ [ +84 90 123 4567                  ] │ │
│                               │ ──────────────────────────────────── │ │
│                               │ [ SAVE CHANGES ]   [ CANCEL ]        │ │
│                               └──────────────────────────────────────┘ │
│                                                                        │
│ ────────────────────────────────────────────────────────────────────── │
│ © 2026 TECHGEAR INC. ALL RIGHTS RESERVED.             CUSTOMER PROFILE │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.1 Left Panel: Customer Identity Card (`lg:col-span-4`)
* **Container:** `bg-white border border-[#E7E5E4] rounded-[6px] p-6 shadow-none flex flex-col justify-between`.
* **Top Monogram:**
  * Monogram Box: `w-[44px] h-[44px] bg-[#F5F5F4] border border-[#E7E5E4] rounded-[4px] flex items-center justify-center font-bold text-[#1C1917] text-base`. Dynamically derived from customer initials (e.g. `JS`).
* **Identity Block:**
  * Full Name: Runtime bound from `firstName` + `lastName` (e.g. `Jane Smith` in Plus Jakarta Sans `18px`, bold, `#1C1917`).
  * Email: Runtime bound from `email` (e.g. `jane.smith@example.com` in Plus Jakarta Sans `13px`, `#57534E`, break-all).
* **Divider:** `border-t border-[#E7E5E4] my-5`.
* **Membership Information:**
  * `MEMBER SINCE`: Formatted date derived dynamically from `createdAt` (e.g. `01 MAR 2026` in JetBrains Mono `12px` `#1C1917`).
* *Cleanliness & Anti-Bloat Assurance:* Administrative and internal telemetry attributes (`ACCOUNT ROLE`, `ACCOUNT STATUS`, `ID`, and `LAST MODIFIED`) are intentionally omitted from customer-facing UI, preventing dashboard bloat and keeping the focus on customer identity.

### 6.2 Right Panel: Personal Information & Form (`lg:col-span-8`)
* **Container:** `bg-white border border-[#E7E5E4] rounded-[6px] p-6 sm:p-8 shadow-none`.
* **Card Header:**
  * Title: `PERSONAL INFORMATION` (Plus Jakarta Sans `16px`, bold uppercase tracking-wider `#1C1917`).
  * Subtitle: `Update your name and phone number. Account email is permanently bound.` (Plus Jakarta Sans `13px`, `#78716C`, mt-1 mb-6).
* **Form Inputs:**
  * **Row 1 — First Name & Last Name (`sm:grid-cols-2 gap-4`):**
    * `FIRST NAME`: Label uppercase `11px` JetBrains Mono `#1C1917` mb-1.5. Input `h-11 w-full bg-white border border-[#E7E5E4] rounded-[4px] px-3.5 text-sm text-[#1C1917] focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706]`.
    * `LAST NAME`: Label uppercase `11px` JetBrains Mono `#1C1917` mb-1.5. Input `h-11 w-full bg-white border border-[#E7E5E4] rounded-[4px] px-3.5 text-sm text-[#1C1917] focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706]`.
  * **Row 2 — Email Address (Read-Only):**
    * Label: `EMAIL ADDRESS (READ-ONLY)` paired with an `IMMUTABLE` mono badge (`text-[10px] bg-[#F5F5F4] border border-[#E7E5E4] px-1.5 py-0.5 rounded-[2px]`).
    * Input: `h-11 w-full bg-[#F5F5F4] border border-[#E7E5E4] rounded-[4px] px-3.5 text-sm text-[#78716C] cursor-not-allowed` with `disabled` attribute.
    * Helper Note: `"Email is tied to authentication and cannot be changed."` in JetBrains Mono `11px` `#A8A29E` mt-1.
  * **Row 3 — Phone Number:**
    * Label: `PHONE NUMBER` (JetBrains Mono `11px` uppercase `#1C1917` mb-1.5).
    * Input: `type="tel"`, `h-11 w-full bg-white border border-[#E7E5E4] rounded-[4px] px-3.5 text-sm text-[#1C1917] focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706]`.
* **Action Row (`pt-4 border-t border-[#E7E5E4] flex items-center gap-3`):**
  * Primary Button: `SAVE CHANGES` (`h-11 px-6 bg-[#1C1917] hover:bg-[#292524] text-[#FAFAF9] font-semibold text-xs tracking-wider uppercase rounded-[4px] transition-colors focus:ring-2 focus:ring-[#D97706]`).
  * Secondary Button: `CANCEL` (`h-11 px-6 bg-white border border-[#E7E5E4] hover:bg-[#F5F5F4] text-[#1C1917] font-semibold text-xs tracking-wider uppercase rounded-[4px] transition-colors`).
  * Feedback Indicator: Inline status message (`UPDATED` in JetBrains Mono `12px` `#D97706`).

---

## 7. Form Behavior, Validation & Error States

### 7.1 View Mode vs. Edit Mode Behavior
* **Default State (View / Form Ready):** Form displays currently fetched values from `UserProfileDto`. Fields are ready for modification.
* **Saving State:**
  * When `SAVE CHANGES` is clicked:
    * Button text switches to: `SAVING...`
    * Button disabled (`cursor-not-allowed`, opacity `85%`).
    * Double-submission strictly blocked.
* **Cancel Action:**
  * Clicking `CANCEL` immediately resets form inputs back to the active `UserProfileDto` state and clears any validation error highlights.

### 7.2 Validation Rules
* **Client-Side Checks:**
  * `firstName`: Optional/nullable, but if provided must be 2–100 characters and contain valid name characters.
  * `lastName`: Optional/nullable, but if provided must be 2–100 characters and contain valid name characters.
  * `phone`: Optional/nullable, but if provided must match E.164 phone format (`^\+?[0-9]{10,15}$`).
* **Server-Side Validation Mapping (HTTP 400 Bad Request):**
  * `GlobalExceptionHandler` returns `errors` map.
  * UI renders error messages directly below the corresponding input in `#DC2626` (Plus Jakarta Sans `12px`).
* **Duplicate Phone Conflict (HTTP 409 Conflict):**
  * If a phone number is already registered by another customer:
  * UI displays inline error on Phone field: `"An account with this phone number already exists."`.
* **Unauthorized Access (HTTP 401 Unauthorized):**
  * **BACKEND-CONFIRMED:** Unauthenticated / invalid authentication → HTTP 401. Backend & API Gateway strictly return HTTP 401 Unauthorized; they do NOT perform an HTTP redirect to `/login`.
  * **UI DESIGN DECISION:** Frontend handles 401 by navigating to `/login`. Client interceptor catches 401, clears invalid credentials from storage, and executes client navigation to `/login`. (Navigation to `/login` is purely a frontend design decision, never a backend behavior).
* **Generic API Error (HTTP 500 / Network Failure):**
  * UI displays compact alert banner: `"Unable to save profile changes. Please retry."` with `RETRY` button.

---

## 8. Responsive Layout & Mobile Behavior

* **Desktop (`>= 1024px`):**
  * 12-column grid within `max-w-4xl` centered canvas.
  * Left identity spec card spans 4 columns (`col-span-4`).
  * Right personal information form spans 8 columns (`col-span-8`).
* **Mobile (`< 1024px`):**
  * Collapses gracefully into a single vertical stack (`grid-cols-1`).
  * Left identity card sits cleanly above the personal information form.
  * All input fields maintain full width with `>= 44px` height (`h-11`).
  * Touch targets adhere to accessibility standards (`>= 44px`).
  * Zero horizontal overflow (`overflow-x-hidden`).

---

## 9. Stitch Canonical Artifact Specification

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `7037d9ffc5b24cc0bba7fc694ffad21c`
* **Screen Title:** `TECHGEAR — Customer Profile`
* **Design Token Reference:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
* **Screenshot URL:** [View Canonical Customer Profile Screen](https://lh3.googleusercontent.com/aida/AEtjO1UNwjmcPbipknw84vOGz2T8LxhB5uh5g46mISEVzioykqCbHr9kILpEoiJohWgQXxcIcZsrl7Dp9yQfYH1nk1FrDmL06wvbwqZFvUZHtOhXOt-Lc7r1A0intg6Gd2WkEHdXktMaF5lquXe1ijFd-XByPzqUUK4iWYwJTliqOmTA4xYZFoTHY6jin7GKyw2jqr5NItU_i6jAHnSxs7YVYrhyMHOBm78OCZRa0-9i-9PrLiSh7pgpA69HTQ)
* **Visual Audit Confirmation:**
  * Top Navigation: Sticky 64px header with `TECHGEAR` logo, `SHOP`, `ORDERS`, active `PROFILE`, and cart `[0]`.
  * Breadcrumb & Headline: `ACCOUNT / PROFILE` breadcrumb with bold `PROFILE` heading and divider.
  * Left Card: Clean customer identity card with monogram `JS`, customer name, email, and clean `MEMBER SINCE` row (`01 MAR 2026`). Completely free of administrative system badges (`ID: 0041`, `ACCOUNT ROLE`, `ACCOUNT STATUS`, `LAST MODIFIED`) or pseudo-server telemetry strips.
  * Right Form: Clean form container with `FIRST NAME`, `LAST NAME`, read-only `EMAIL ADDRESS` (`IMMUTABLE` badge), `PHONE NUMBER`, and rectangular action buttons (`SAVE CHANGES`, `CANCEL`) with crisp 4px corners.
  * Footer: Muted single copyright line `© 2026 TECHGEAR INC. ALL RIGHTS RESERVED.` with `CUSTOMER PROFILE`.
  * **Design Mock Data Declaration (Point 2 - Mark Stitch Sample Data Correctly):**
    Visual values displayed in the canonical Stitch screen `7037d9ffc5b24cc0bba7fc694ffad21c`:
    * `ID: 0041`
    * `Jane Smith`
    * `01 MAR 2026`
    * `15 MAR 2026, 12:00`
    are **strictly DESIGN MOCK DATA**.
    In documentation and implementation:
    * Runtime MUST bind actual customer data from `UserProfileDto`.
    * Never hard-code any of the sample values above.

---

## 10. Final Verification Checklist

- [x] `GET /api/v1/users/me` verified in `UserController.java:24`
- [x] `PATCH /api/v1/users/me` verified in `UserController.java:31`
- [x] Request fields verified (`UpdateProfileRequest`: `firstName`, `lastName`, `phone`)
- [x] Response fields verified (`UserProfileDto`: `id`, `firstName`, `lastName`, `email`, `phone`, `role`, `enabled`, `createdAt`, `updatedAt`)
- [x] Editable fields verified (Only `firstName`, `lastName`, `phone` are editable; `email` is strictly read-only)
- [x] Validation verified (`@Size`, `@Pattern` on names and phone)
- [x] Authentication verified (`SecurityConfig.anyRequest().authenticated()` + Gateway `AuthenticationFilter`)
- [x] 401 unauthorized handling verified (HTTP 401 is BACKEND-CONFIRMED; navigation to /login is UI DESIGN DECISION)
- [x] Update behavior verified (Calls `userService.updateProfile`)
- [x] Duplicate constraints verified (`uk_users_phone`)
- [x] No fake profile data (No loyalty tiers, no fake spending counters)
- [x] No account features outside backend (No avatar upload, no password change, no address book)
- [x] No dashboard behavior (Practical customer profile layout, not an admin dashboard)
- [x] Obsidian & Paper design tokens reused
- [x] Responsive layout specified (Desktop 2-column, Mobile single-column)
- [x] Exactly 1 canonical Stitch screen created (`7037d9ffc5b24cc0bba7fc694ffad21c`)
- [x] Documentation created (`frontend/docs/FE-14-PROFILE-DESIGN.md`)
- [x] FE-01 &rarr; FE-13 unchanged
- [x] No backend / package / Git changes

---

**FE-14 STATUS: READY FOR REVIEW**
