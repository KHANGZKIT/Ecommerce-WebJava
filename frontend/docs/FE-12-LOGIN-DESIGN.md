# TECHGEAR — FE-12 Customer Login Design Specification

> **Document ID:** FE-12-LOGIN-DESIGN  
> **Route:** `/login`  
> **Target Audience:** Unauthenticated Customers / Returning Users  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Primary Visual Inspiration:** Pinterest Reference #2 (Split-Screen Hardware Editorial Composition)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `0d23a371ef7849fe86723655b0eb3d9f`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Customer Login** screen (`/login`) serves as the authentication gateway for TECHGEAR customers. It enables returning users to authenticate using their verified email and password credentials, receive a secure JWT bearer token, and seamlessly transition into authenticated commerce workflows (such as `/checkout`, `/orders`, or `/profile`).

### Strict YAGNI Boundaries
* **Strict Backend Alignment:** The screen renders **only** the inputs required by `LoginRequest.java`: `email` and `password`.
* **Zero Speculative Auth Controls:**
  * **No Remember Me:** Backend does not support persistent session cookies or remember-me tokens.
  * **No Forgot / Reset Password:** Backend does not provide password reset or recovery endpoints.
  * **No Social Login:** No Google, Facebook, Apple, or OAuth SSO buttons.
  * **No Refresh Token Flow:** `AuthResponse` does not provide a refresh token; session is governed strictly by `accessToken` (`expiresIn`).
  * **No 2FA / MFA:** No multi-factor or biometric authentication mechanisms exist.
* **No Marketing Slop / AI Tropes:** Zero fake security badges ("256-bit encrypted"), zero trust counters ("Trusted by 10,000+ engineers"), zero floating card drop-shadows, and zero purple/blue neon gradients.

---

## 2. Route & Navigation Architecture

* **Canonical Route:** `/login`
* **Post-Login Destination Architecture:**
  * Default destination upon successful login: `/shop`.
  * If an existing protected-route redirect mechanism is already established in the application flow, the login flow reuses that existing mechanism.
  * Does not introduce or assume a new custom redirect system.
* **Top Navigation Anchors:**
  * **Brand Wordmark:** `TECHGEAR` links to homepage (`/`).
  * **Back to Shop:** Secondary navigation link `← BACK TO SHOP` links to `/shop`.
* **Secondary Registration Pathway:**
  * Under the submit action, a subtle link invites new users: `Don't have an account? Create account` linking to `/register`.
  * Verified backend support: `POST /api/v1/auth/register` is actively implemented in `user-service`.

---

## 3. Backend Verification & Integration Contract

### 3.1 Verified Backend Source Code References

| Component | Code Reference | Implementation Detail |
| :--- | :--- | :--- |
| **Auth Controller** | `AuthController.java:25-28` | `POST /api/v1/auth/login` &rarr; returns `ResponseEntity<AuthResponse>` via `userService.login(request)`. |
| **Registration Controller** | `AuthController.java:20-23` | `POST /api/v1/auth/register` &rarr; returns `ResponseEntity<AuthResponse>` (`HttpStatus.CREATED`). |
| **User Service** | `UserService.java:61-75` | Normalizes email (`toLowerCase().trim()`), authenticates via `authenticationManager.authenticate()`, generates JWT token with `userId` and `role` claims, and constructs `AuthResponse`. |
| **Exception Handler** | `GlobalExceptionHandler.java:31-36` | Catches `BadCredentialsException` &rarr; returns HTTP 401 with `{"status": 401, "message": "Invalid email or password"}`. |
| **Disabled Account** | `GlobalExceptionHandler.java:38-42` | Catches `DisabledException` &rarr; returns HTTP 403 with `{"status": 403, "message": "Account is disabled"}`. |
| **Validation Handler** | `GlobalExceptionHandler.java:44-58` | Catches `MethodArgumentNotValidException` &rarr; returns HTTP 400 with `errors` map. |
| **Gateway Routing** | `application.yml:30-36` | Route `user-service-auth` forwards `/api/v1/auth/**` publicly without rate limiters or bearer filters. |

### 3.2 Request Data Contract (`LoginRequest`)

```json
{
  "email": "architect@techgear.io",
  "password": "Password123!"
}
```

* **Backend Validation Constraints (`LoginRequest.java`):**
  * `email`: `@NotBlank(message = "Email is required")`, `@Email(message = "Must be a valid email address")`, `@Size(max = 255)`.
  * `password`: `@NotBlank(message = "Password is required")`, `@Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")`.

### 3.3 Response Data Contract (`AuthResponse`)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
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

### 3.4 Verified Capabilities & Explicit Exclusions

| Feature | Backend Status | Specification Treatment |
| :--- | :---: | :--- |
| **Email + Password Login** | **SUPPORTED** | Primary form controls. |
| **Registration Pathway** | **SUPPORTED** | Secondary link to `/register`. |
| **Refresh Token** | **NOT SUPPORTED** | Session relies on `accessToken`. No refresh token logic or UI. |
| **Remember Me** | **NOT SUPPORTED** | No checkbox. Client session state handled per browser session. |
| **Forgot / Reset Password** | **NOT SUPPORTED** | No "Forgot Password?" link rendered. |
| **Social Login (OAuth)** | **NOT SUPPORTED** | No third-party OAuth buttons. |
| **Multi-Factor Auth (MFA)** | **NOT SUPPORTED** | No OTP/2FA verification screens. |

---

## 4. Architectural Verification: Grounded vs. Design Decisions vs. Mock Data

| Aspect | Classification | Grounding & Rationale |
| :--- | :--- | :--- |
| **Auth Endpoint & Method** | `BACKEND-CONFIRMED` | `POST /api/v1/auth/login` defined in `AuthController.java:25`. |
| **Form Fields (`email`, `password`)** | `BACKEND-CONFIRMED` | Exactly matching `LoginRequest.java`. No username or extra fields. |
| **Error Status Codes & Messages** | `BACKEND-CONFIRMED` | HTTP 401 (`"Invalid email or password"`), HTTP 403 (`"Account is disabled"`), HTTP 400 (`"Validation failed"`). |
| **Payload Contracts** | `BACKEND-CONFIRMED` | Returns `accessToken`, `tokenType`, `expiresIn`, and `UserProfileDto`. |
| **Absence of Auxiliary Features** | `BACKEND-CONFIRMED` | No refresh token, remember-me, or password reset endpoints exist. |
| **Split-Screen Composition** | `UI DESIGN DECISION` | Inspired by Pinterest Reference #2. 50/50 balance between hardware visual and authentication vitrine. |
| **Heading: "WELCOME BACK"** | `UI DESIGN DECISION` | Confident, understated editorial greeting without marketing hyperbole. |
| **Rectangular Form Styling** | `UI DESIGN DECISION` | Obsidian & Paper standard: 4px corner radius, hairline 1px border, signal amber focus ring. |
| **Hardware Photography Specimen** | `DESIGN MOCK DATA` | CNC machined 6063 aluminum keyboard photo serves as visual design asset in Stitch screen `0d23a371ef7849fe86723655b0eb3d9f`. |

---

## 5. Visual Direction: Reinterpretation of Pinterest Reference #2

The visual direction draws directly from the composition, proportion, and tonal balance of **Pinterest Reference #2**, translated through TECHGEAR's **Obsidian & Paper** design system:

* **Split-Screen Balance (50/50 Split):**
  * **Left Panel (Hardware Editorial Vitrine):** High-density tactile presence. Features large studio product photography of precision hardware (CNC machined aluminum custom mechanical keyboard) isolated with ample negative space on `#F5F5F4` milled neutral foundation.
  * **Right Panel (Authentication Vitrine):** Generous breathing room, precise Swiss typographic hierarchy, and a hyper-focused, minimal form on `#FAFAF9` warm alabaster canvas.
* **No Literal Copying:** Replaces third-party branding, generic fonts, and decorative elements with TECHGEAR's Plus Jakarta Sans, JetBrains Mono, obsidian solids, and hairline borders.

---

## 6. Screen Layout & Component Anatomy

```
┌──────────────────────────────────────┬──────────────────────────────────────┐
│ LEFT PANEL: HARDWARE EDITORIAL (50%) │ RIGHT PANEL: AUTHENTICATION (50%)    │
├──────────────────────────────────────┼──────────────────────────────────────┤
│ TECHGEAR CATALOG                     │ TECHGEAR              ← BACK TO SHOP │
│                                      │                                      │
│                                      │                                      │
│        ┌──────────────────┐          │             WELCOME BACK             │
│        │                  │          │     Enter your credentials to access │
│        │  [ STUDIO PHOTO  │          │         your TECHGEAR account.       │
│        │    CNC MACHINED  │          │                                      │
│        │     KEYBOARD ]   │          │     EMAIL ADDRESS                    │
│        │                  │          │     [ architect@techgear.io        ] │
│        └──────────────────┘          │                                      │
│                                      │     PASSWORD                         │
│                                      │     [ ••••••••                     ] │
│                                      │                                      │
│                                      │     [           SIGN IN            ] │
│                                      │                                      │
│                                      │     Don't have an account?           │
│                                      │     Create account                   │
│                                      │                                      │
│ PRECISION HARDWARE         EST. 2024 │ © 2026 TECHGEAR INC. ALL RIGHTS RES. │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

### 6.1 Left Panel: Hardware Editorial Frame
* **Dimensions:** 50% width on Desktop (`lg:grid-cols-2`), full viewport height (`min-h-screen`).
* **Background:** Milled neutral `#F5F5F4`, bounded by a hairline right border (`1px solid #E7E5E4`).
* **Header Rule:** `TECHGEAR CATALOG` (JetBrains Mono `11px`, `#78716C`, uppercase tracking `+0.08em`).
* **Visual Presentation:** Clean rectangular frame (`bg-white p-3 border border-[#E7E5E4] rounded-[4px]`) housing uncompressed hardware studio photography with clean matting and zero visual clutter.
* **Footer Rule:** `PRECISION HARDWARE` & `EST. 2024` (JetBrains Mono `11px`, `#78716C`).

### 6.2 Right Panel: Authentication Vitrine
* **Dimensions:** 50% width on Desktop, `min-h-screen`, background warm alabaster `#FAFAF9`.
* **Top Navigation Bar:**
  * Wordmark: `TECHGEAR` (Plus Jakarta Sans `20px`, bold, `#1C1917`, uppercase tracking `-0.02em`).
  * Return Link: `← BACK TO SHOP` (JetBrains Mono `11px`, `#78716C`, hover: `#1C1917`, transition 150ms).
* **Form Container:** Centered with vertical auto margins, constrained to `max-w-sm` (384px) with generous lateral padding.
* **Typographic Hierarchy:**
  * Headline: `WELCOME BACK` (Plus Jakarta Sans `32px`, bold Weight 700, `#1C1917`, tracking `-0.02em`).
  * Subtitle: `Enter your credentials to access your TECHGEAR account.` (Plus Jakarta Sans `14px`, `#57534E`, line-height `1.5`).
* **Input Specifications:**
  * Container height: `h-11` (44px) meeting touch-target standards.
  * Surface: `#FFFFFF`, border `1px solid #E7E5E4`, corner radius `4px`.
  * Padding: `px-3.5`.
  * Typography: Plus Jakarta Sans `14px`, text `#1C1917`, placeholder `#A8A29E`.
  * Focus State: Border shifts to `#1C1917` with subtle `1px` ring in signal amber `#D97706`.
* **Primary Action Button (`SIGN IN`):**
  * Dimensions: `h-11 w-full`.
  * Surface: `#1C1917`, hover `#292524`, active `#0C0A09`.
  * Typography: Plus Jakarta Sans `12px`, font-semibold, `#FAFAF9`, tracking `+0.06em` uppercase.
  * Border radius: `4px` (strict non-pill).
* **Registration Prompt:**
  * Text: Plus Jakarta Sans `14px`, `#57534E`.
  * Link: `Create account` (`font-semibold text-[#1C1917] underline underline-offset-4 decoration-stone-300 hover:decoration-[#1C1917]`).
* **Footer Utility:**
  * Copyright: `© 2026 TECHGEAR INC. ALL RIGHTS RESERVED.` (JetBrains Mono `10px`, `#A8A29E`).

---

## 7. Form Behavior, Validation & Error States

### 7.1 Client-Side Validation
* **Email Field:**
  * Evaluated before submission against standard RFC email regex.
  * Empty trigger: `"Email address is required."`
  * Format mismatch: `"Please enter a valid email address."`
* **Password Field:**
  * Empty trigger: `"Password is required."`
  * Length mismatch: `"Password must be at least 8 characters."`
* **Visual Presentation:** Subtle inline message in `#DC2626` (Plus Jakarta Sans `12px`) below the respective input with hairline red border highlight (`border-[#DC2626]`).

### 7.2 Server-Side Error Handling
1. **Invalid Credentials (HTTP 401):**
   * Returned by backend on wrong password or unregistered email.
   * Display: Form-level alert box placed above form fields:
     * Surface `#FFFFFF`, border `1px solid #FCA5A5`, background `#FEF2F2`, `p-3 rounded-[4px]`.
     * Message: `"Invalid email or password. Please verify your credentials."` (Plus Jakarta Sans `13px`, `#991B1B`).
2. **Disabled Account (HTTP 403):**
   * Display: `"This account has been disabled."`
   * *Strict Boundary:* Does not invent or reference an unverified support system or contact channel.
3. **Network / Server Outage (HTTP 500 / Timeout):**
   * Display: `"Unable to connect to authentication service. Please check your connection and retry."`
   * *Anti-Leak Rule:* Never expose internal terms such as `"BadCredentialsException"`, `"JWT generation failure"`, or `"user-service:8081 unreachable"`.

### 7.3 Loading & Submission Protection
* When customer clicks `SIGN IN`, the button enters a loading state:
  * Text changes to: `SIGNING IN...`
  * Opacity reduces to `85%`, button disabled (`cursor-not-allowed`).
  * Double-submission is strictly prevented.

---

## 8. Success Flow & Session Management

1. **Token Persistence:**
   * Upon receiving `AuthResponse`, frontend saves `accessToken` to secure browser storage (HTTP-only cookie or memory session storage).
   * Populates client authentication state (`auth.user = response.user`).
2. **Post-Authentication Navigation:**
   * Successful login navigates to the existing application destination.
   * Default destination: `/shop`.
   * Reuses an existing protected-route redirect mechanism only if one already exists in the application flow. Does not create or assume a new redirect system.
3. **Cart Synchronization:**
   * The authenticated customer's cart session is retained and unified with active cart items.

---

## 9. Responsive Layout & Mobile Behavior

* **Desktop (`>= 1024px`):**
  * 50/50 full-height split screen layout (`min-h-screen grid grid-cols-2`).
  * Hardware visual vitrine displayed in full editorial format on the left.
* **Tablet (`768px – 1023px`):**
  * Split screen maintains 50/50 balance with proportional padding adjustments (`p-8`).
* **Mobile (`< 768px`):**
  * Transitions gracefully to a single vertical column (`grid-cols-1`).
  * Left hardware visual panel is collapsed or presented as a compact editorial masthead above the form to prioritize immediate input interaction.
  * Inputs and submit button maintain full width with `>= 44px` height.
  * Zero horizontal page overflow (`overflow-x-hidden`).

---

## 10. Stitch Canonical Artifact Specification

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `0d23a371ef7849fe86723655b0eb3d9f`
* **Screen Title:** `TECHGEAR — Login`
* **Design Token Reference:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
* **Visual Audit Confirmation:**
  * Layout: Full-height 50/50 split-screen composition.
  * Left Vitrine: Clean studio hardware photography (machined aluminum mechanical keyboard) with generous negative space, bordered by `TECHGEAR CATALOG` and `PRECISION HARDWARE`. Zero fake security or telemetry overlays.
  * Right Vitrine: Minimalist form featuring `WELCOME BACK` heading, email and password inputs, primary black `SIGN IN` button (`rounded-[4px]`), and secondary registration link.
  * Footer: Clean single copyright line `© 2026 TECHGEAR INC. ALL RIGHTS RESERVED.` with zero pseudo-encrypted telemetry claims.
  * Quality: Pure Swiss typography, zero gradients, zero shadows, zero AI slop.

---

## 11. Verification Checklist

- [x] Auth endpoint verified (`POST /api/v1/auth/login`)
- [x] Request fields verified (`email`, `password`)
- [x] Response verified (`AuthResponse`: `accessToken`, `tokenType`, `expiresIn`, `user`)
- [x] Validation verified (`@NotBlank`, `@Email`, `@Size`)
- [x] Error behavior verified (HTTP 401, 403, 400)
- [x] JWT behavior verified (Bearer token propagation)
- [x] Refresh token capability verified (Confirmed absent)
- [x] Password reset capability verified (Confirmed absent)
- [x] Social login capability verified (Confirmed absent)
- [x] Register capability verified (`POST /api/v1/auth/register`)
- [x] Pinterest reference #2 used as inspiration (50/50 split screen)
- [x] No literal copying
- [x] Premium hardware visual direction maintained
- [x] Obsidian & Paper reused
- [x] No AI-slop patterns (No gradients, no glassmorphism, no fake badges)
- [x] No fake security / telemetry claims
- [x] Responsive layout designed
- [x] Exactly 1 canonical Stitch screen created (`0d23a371ef7849fe86723655b0eb3d9f`)
- [x] Documentation created (`FE-12-LOGIN-DESIGN.md`)
- [x] FE-01 through FE-11 completely preserved without modification
- [x] No backend, package, or Git changes

---

**FE-12 STATUS: READY FOR REVIEW**
