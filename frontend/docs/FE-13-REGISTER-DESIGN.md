# TECHGEAR — FE-13 Customer Registration Design Specification

> **Document ID:** FE-13-REGISTER-DESIGN  
> **Route:** `/register`  
> **Target Audience:** Unauthenticated New Customers / Prospective Users  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Primary Visual Inspiration:** Sibling to FE-12 Customer Login (50/50 Split-Screen Hardware Editorial Composition)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `8f428d07a8364a6e9262be41cf9888f9`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Purpose

The **Customer Registration** screen (`/register`) provides the account creation onboarding interface for TECHGEAR customers. It enables new users to register using verified customer profile attributes, receive an immediate JWT authentication bearer token directly from the backend registration endpoint, and transition automatically into an authenticated session without intermediate OTP friction or artificial redirection hurdles.

### Strict YAGNI Boundaries
* **Strict Backend Alignment:** The form collects and submits **only** the attributes modeled in `RegisterRequest.java`: `firstName`, `lastName`, `email`, `password`, and optional `phone`.
* **Zero Speculative Auth Controls:**
  * **No Social Login:** No Google, Apple, Facebook, or OAuth SSO providers.
  * **No OTP / SMS Verification:** No phone OTP or multi-step SMS verification screens.
  * **No Email Verification Gate:** Backend activates the account immediately upon creation (`enabled = true`); no email confirmation token gate is enforced.
  * **No Auxiliary Profile Customization:** No avatar upload, address entry, marketing newsletter subscription, referral code, or loyalty program opt-in.
  * **No Speculative Redirect Engines:** No custom `?redirect=` redirect engine created.
* **No AI-Slop & Fake Badges:** Zero pseudo-security labels ("256-bit TLS"), zero fake customer counters, zero floating cards, and zero purple/blue neon gradients.

---

## 2. Route & Navigation Architecture

* **Canonical Route:** `/register`
* **Top Navigation Anchors:**
  * **Brand Wordmark:** `TECHGEAR` links to homepage (`/`).
  * **Back to Shop:** Secondary utility link `← BACK TO SHOP` links to `/shop`.
* **Secondary Authentication Pathway:**
  * Beneath the primary action, a subtle link invites existing users: `Already have an account? Sign in` linking to `/login`.
* **Post-Registration Destination Architecture:**
  * Default destination upon successful registration: `/shop`.
  * If an existing protected-route return mechanism is already established in the application flow (e.g. from checkout initiation), the flow navigates to that destination.
  * Does not introduce a new or unverified redirection system.

---

## 3. Backend Verification & Integration Contract

### 3.1 Verified Backend Source Code References

| Component | Code Reference | Implementation Detail |
| :--- | :--- | :--- |
| **Registration Controller** | `AuthController.java:20-23` | `POST /api/v1/auth/register` &rarr; returns `ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request))`. HTTP 201 Created. |
| **Login Controller** | `AuthController.java:25-28` | `POST /api/v1/auth/login` &rarr; returns `ResponseEntity.ok(userService.login(request))`. HTTP 200 OK. |
| **Registration Logic** | `UserService.java:32-59` | Normalizes email (`toLowerCase().trim()`), verifies uniqueness against `existsByEmail()`, checks phone uniqueness if present, hashes password with BCrypt, sets `role = UserRole.CUSTOMER`, sets `enabled = true`, saves entity, generates JWT token with `userId` and `role` claims, and returns `AuthResponse`. |
| **Duplicate User Error** | `UserAlreadyExistsException.java:5-9` | Extends `ApiException` with `HttpStatus.CONFLICT` (HTTP 409). |
| **Exception Handler (Duplicate)** | `GlobalExceptionHandler.java:24-29` | Catches `ApiException` &rarr; returns HTTP 409 with `{"status": 409, "message": "An account with this email address already exists."}`. |
| **Exception Handler (Validation)** | `GlobalExceptionHandler.java:44-58` | Catches `MethodArgumentNotValidException` &rarr; returns HTTP 400 with `errors` map containing field-level messages. |
| **Gateway Routing** | `application.yml:30-36` | Public route forwards `/api/v1/auth/**` through API Gateway without bearer auth filter requirements. |

### 3.2 Request Data Contract (`RegisterRequest`)

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "password": "Password123!",
  "phone": "+84901234567"
}
```

* **Field Specifications & Validation Annotations (`RegisterRequest.java`):**
  * `firstName`:
    * `@NotBlank(message = "First name is required and cannot be empty or blank")`
    * `@Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")`
    * `@Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s\\-']+$", message = "First name must contain at least one letter and can only include letters, spaces, hyphens, and apostrophes")`
    * *Status:* Required.
  * `lastName`:
    * `@NotBlank(message = "Last name is required and cannot be empty or blank")`
    * `@Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")`
    * `@Pattern(regexp = "^(?=.*\\p{L})[\\p{L}\\s\\-']+$", message = "Last name must contain at least one letter and can only include letters, spaces, hyphens, and apostrophes")`
    * *Status:* Required.
  * `email`:
    * `@NotBlank(message = "Email is required")`
    * `@Email(message = "Must be a valid email address")`
    * `@Size(max = 255, message = "Email must not exceed 255 characters")`
    * *Status:* Required.
  * `password`:
    * `@NotBlank(message = "Password is required")`
    * `@Size(min = 8, message = "Password must be at least 8 characters")`
    * `@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")`
    * *Status:* Required.
  * `phone`:
    * `@Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Must be a valid phone number format (+919876543210)")`
    * `@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)`
    * *Status:* Optional.

### 3.3 Response Data Contract (`AuthResponse` — HTTP 201 Created)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
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
    "createdAt": "2026-10-07T14:40:00",
    "updatedAt": "2026-10-07T14:40:00"
  }
}
```

* **Auto-Login Behavior Verified:**
  * `UserService.register()` creates the account, generates the JWT token immediately, and returns the full `AuthResponse`.
  * The customer is **automatically authenticated** upon successful registration. No secondary login call is needed.

### 3.4 Verified Capabilities & Explicit Exclusions

| Feature | Backend Status | Specification Treatment |
| :--- | :---: | :--- |
| **Customer Registration Endpoint** | **SUPPORTED** | `POST /api/v1/auth/register` returning HTTP 201 Created. |
| **Immediate JWT Session (Auto-Login)** | **SUPPORTED** | Returns `AuthResponse` containing `accessToken`. |
| **Immediate Account Activation** | **SUPPORTED** | `user.enabled = true` on persistence. |
| **Duplicate Email Detection** | **SUPPORTED** | Throws `UserAlreadyExistsException` &rarr; HTTP 409 Conflict. |
| **Duplicate Phone Detection** | **SUPPORTED** | Throws `UserAlreadyExistsException` &rarr; HTTP 409 Conflict. |
| **Email Verification / OTP** | **NOT SUPPORTED** | Not present in backend; no OTP screen. |
| **Social Login (OAuth)** | **NOT SUPPORTED** | Not present in backend; no third-party OAuth buttons. |
| **Password Reset Endpoint** | **NOT SUPPORTED** | Not present in backend; no forgot password link. |

---

## 4. Architectural Verification: Grounded vs. Design Decisions vs. Mock Data vs. UI-Only Validation

| Aspect | Classification | Grounding & Rationale |
| :--- | :--- | :--- |
| **Register Endpoint & Method** | `BACKEND-CONFIRMED` | `POST /api/v1/auth/register` defined in `AuthController.java:21`. Returns HTTP 201. |
| **Backend Payload Attributes** | `BACKEND-CONFIRMED` | Exactly matching `RegisterRequest.java`: `firstName`, `lastName`, `email`, `password`, `phone`. |
| **Auto-Authentication Response** | `BACKEND-CONFIRMED` | Returns `AuthResponse` with active JWT token (`accessToken`). |
| **Duplicate Account Status** | `BACKEND-CONFIRMED` | HTTP 409 Conflict with customer-safe message from `UserAlreadyExistsException`. |
| **Password Regex & Min Length** | `BACKEND-CONFIRMED` | 8+ characters, uppercase, lowercase, digit, special character (`@$!%*?&`). |
| **Split-Screen Layout & Tokens** | `UI DESIGN DECISION` | 50/50 split matching FE-12 (`0d23a371ef7849fe86723655b0eb3d9f`) under Obsidian & Paper. |
| **Two-Column Name Field Layout** | `UI DESIGN DECISION` | `FIRST NAME` and `LAST NAME` placed side-by-side in a 2-column grid to maintain vertical rhythm. |
| **"Confirm Password" Field** | `UI-ONLY VALIDATION` | Because the backend does not provide a password reset endpoint, a typing mistake during registration would permanently lock out the user. Client-side confirmation prevents this fatal UX lockout. The field is validated purely on the client and is **never** sent to the backend API. |
| **Hardware Photography Specimen** | `DESIGN MOCK DATA` | Keychron CNC machined mechanical keyboard photograph in Stitch screen `8f428d07a8364a6e9262be41cf9888f9`. |

---

## 5. Visual Direction: Sibling Parity with FE-12 Customer Login

FE-13 is a visual sibling to FE-12, adhering strictly to the **Obsidian & Paper** design system:

* **Split-Screen Balance (50/50 Desktop):**
  * **Left Panel (Hardware Editorial Vitrine):** Milled neutral `#F5F5F4` background bounded by a hairline right border (`1px solid #E7E5E4`). Holds a centered studio frame (`bg-white border border-[#E7E5E4] p-3 rounded-[4px]`) featuring precision hardware photography surrounded by generous negative space. Pure minimalist focus: decorative labels (`TECHGEAR CATALOG`, signal amber dot, and `PRECISION HARDWARE // EST. 2024`) have been eliminated to ensure serene editorial clarity and prevent unconfirmed brand claims.
  * **Right Panel (Registration Vitrine):** Warm alabaster `#FAFAF9` canvas, generous vertical breathing space, Swiss typographic hierarchy, and a form container constrained to `max-w-sm` (384px).
* **Color Palette:**
  * `#FAFAF9`: Canvas background.
  * `#FFFFFF`: Input surfaces and image frame backing.
  * `#F5F5F4`: Left panel milled surface.
  * `#1C1917`: Primary typography, field labels, and primary CTA button.
  * `#57534E`: Subtitles and body guidance.
  * `#78716C`: Monospaced rules, inactive labels, and password criteria note.
  * `#E7E5E4`: Hairline structural borders.
  * `#D97706`: Focused input border/ring accent (reserved for functional focus states).

---

## 6. Screen Layout & Component Anatomy

```
┌──────────────────────────────────────┬──────────────────────────────────────┐
│ LEFT PANEL: HARDWARE VITRINE (50%)   │ RIGHT PANEL: REGISTRATION (50%)      │
├──────────────────────────────────────┼──────────────────────────────────────┤
│                                      │ TECHGEAR              ← BACK TO SHOP │
│                                      │                                      │
│                                      │             CREATE ACCOUNT           │
│        ┌──────────────────┐          │     Register for a TECHGEAR account  │
│        │                  │          │     to access orders and checkout.   │
│        │  [ STUDIO PHOTO  │          │                                      │
│        │    CNC MACHINED  │          │     FIRST NAME         LAST NAME     │
│        │     KEYBOARD ]   │          │     [ Jane       ]     [ Smith      ]│
│        │                  │          │                                      │
│        └──────────────────┘          │     EMAIL ADDRESS                    │
│                                      │     [ jane.smith@example.com       ] │
│                                      │                                      │
│                                      │     PHONE NUMBER (OPTIONAL)          │
│                                      │     [ +84 90 123 4567              ] │
│                                      │                                      │
│                                      │     PASSWORD                         │
│                                      │     [ ••••••••                     ] │
│                                      │     Min. 8 chars with upper, ...     │
│                                      │                                      │
│                                      │     CONFIRM PASSWORD                 │
│                                      │     [ ••••••••                     ] │
│                                      │                                      │
│                                      │     [        CREATE ACCOUNT        ] │
│                                      │                                      │
│                                      │     Already have an account?         │
│                                      │     Sign in                          │
│                                      │                                      │
│                                      │ © 2026 TECHGEAR INC. ALL RIGHTS RES. │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

### 6.1 Left Panel: Hardware Editorial Frame
* **Dimensions:** 50% width on Desktop (`lg:grid-cols-2`), full viewport height (`min-h-screen`).
* **Background:** Milled neutral `#F5F5F4`, bounded by `1px solid #E7E5E4` right border.
* **Layout:** Centered alignment with generous negative space (`justify-center items-center`).
* **Product Vitrine:** Centered white matting container (`bg-white border border-[#E7E5E4] p-3 rounded-[4px]`) housing precision CNC machined hardware photography (`aspect-[16/10] grayscale contrast-105`).
* **Clean Surface:** Free of pseudo-technical decoration, fake status dots, or unverified claims.

### 6.2 Right Panel: Registration Container
* **Dimensions:** 50% width on Desktop, `min-h-screen`, background `#FAFAF9`.
* **Top Utility Bar:**
  * Wordmark: `TECHGEAR` (Plus Jakarta Sans `20px`, bold, `#1C1917`, uppercase tracking `-0.02em`).
  * Return Link: `← BACK TO SHOP` (JetBrains Mono `11px`, `#78716C`, hover: `#1C1917`).
* **Form Container:** Centered with vertical auto margins, width constrained to `max-w-sm` (384px).
* **Form Anatomy:**
  * **Header:**
    * Headline: `CREATE ACCOUNT` (Plus Jakarta Sans `32px`, bold Weight 700, `#1C1917`, tracking `-0.025em`).
    * Subtitle: `Register for a TECHGEAR account to access orders and checkout.` (Plus Jakarta Sans `14px`, `#57534E`, mt-2 mb-6).
  * **Row 1 — First Name & Last Name (2-Column Grid, Gap 12px):**
    * Labels: `FIRST NAME` / `LAST NAME` (JetBrains Mono `11px`, font-medium, `#1C1917`, uppercase mb-1.5).
    * Inputs: `h-11 w-full bg-white border border-[#E7E5E4] rounded-[4px] px-3.5 text-sm text-[#1C1917] placeholder-[#A8A29E] focus:border-[#1C1917] focus:ring-1 focus:ring-[#D97706]`.
  * **Row 2 — Email Address:**
    * Label: `EMAIL ADDRESS` (JetBrains Mono `11px`, `#1C1917`, uppercase mb-1.5).
    * Input: `type="email"`, `h-11`, placeholder `"jane.smith@example.com"`.
  * **Row 3 — Phone Number (Optional):**
    * Label: `PHONE NUMBER (OPTIONAL)` (JetBrains Mono `11px`, `#78716C`, uppercase mb-1.5).
    * Input: `type="tel"`, `h-11`, placeholder `"+84 90 123 4567"`.
  * **Row 4 — Password:**
    * Label: `PASSWORD` (JetBrains Mono `11px`, `#1C1917`, uppercase mb-1.5).
    * Input: `type="password"`, `h-11`, tracking-widest, placeholder `"••••••••"`.
    * Helper Note: `"Min. 8 characters with uppercase, lowercase, number and symbol"` in JetBrains Mono `11px` `#78716C` mt-1.5.
  * **Row 5 — Confirm Password (`UI-ONLY VALIDATION`):**
    * Label: `CONFIRM PASSWORD` (JetBrains Mono `11px`, `#1C1917`, uppercase mb-1.5).
    * Input: `type="password"`, `h-11`, tracking-widest, placeholder `"••••••••"`.
  * **Primary CTA (`CREATE ACCOUNT`):**
    * Dimensions: `h-11 w-full mt-2`.
    * Surface: `#1C1917`, hover `#292524`.
    * Typography: Plus Jakarta Sans `12px`, font-semibold, `#FAFAF9`, tracking `+0.06em` uppercase.
    * Radius: `rounded-[4px]` (strict non-pill).
  * **Secondary Link:**
    * Text: `Already have an account?` (Plus Jakarta Sans `14px`, `#57534E`).
    * Link: `Sign in` (font-medium `#1C1917` underline underline-offset-2 hover:text-[#000000]) linking to `/login`.
* **Bottom Footer:**
  * Copyright: `© 2026 TECHGEAR INC. ALL RIGHTS RESERVED.` in JetBrains Mono `10px` `#A8A29E`.
  * *Zero pseudo-telemetry tags* (confirmed: removed `'SECURE 256-BIT TLS'`).

---

## 7. Form Behavior, Validation & Error States

### 7.1 Client-Side Validation Rules
* **First Name / Last Name:**
  * Evaluated on blur and before submission.
  * Must be 2–100 characters and contain valid name characters.
  * Error message: `"First name must be between 2 and 100 characters."` / `"Last name must be between 2 and 100 characters."`
* **Email Address:**
  * RFC email format validation.
  * Error message: `"Please enter a valid email address."`
* **Phone Number (Optional):**
  * If non-empty, must match regex `^\+?[0-9]{10,15}$`.
  * Error message: `"Must be a valid phone number (+84901234567)."`
* **Password:**
  * Must be at least 8 characters and contain at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character (`@$!%*?&`).
  * Mismatch message: `"Password must contain uppercase, lowercase, number, and special character."`
* **Confirm Password (`UI-ONLY VALIDATION`):**
  * Checked client-side against the value of `password`.
  * Mismatch message: `"Passwords do not match."`
  * Stripped from the payload before dispatching `POST /api/v1/auth/register`.

### 7.2 Server-Side Error Handling & Mapping

1. **Duplicate Email Error (HTTP 409 Conflict):**
   * Backend returns: `{"status": 409, "message": "An account with this email address already exists."}`.
   * UI displays inline error on Email field or top-level notification alert:
     * Message: `"An account with this email address already exists."`
     * Styled: Surface `#FEF2F2`, border `1px solid #FCA5A5`, text `#991B1B`.
2. **Duplicate Phone Error (HTTP 409 Conflict):**
   * Backend returns: `{"status": 409, "message": "An account with this phone number already exists."}`.
   * UI displays inline error on Phone field.
3. **Field Validation Errors (HTTP 400 Bad Request):**
   * Backend returns `GlobalExceptionHandler` error response with `errors` map.
   * UI dynamically attaches the backend error message directly to the corresponding input field in `#DC2626`.
4. **Network Outage / Server Failure (HTTP 500 / Timeout):**
   * UI displays short, neutral notification:
     * Message: `"Unable to complete registration. Please check your connection and retry."`
     * Action: `RETRY`.
     * Anti-leak rule: Never exposes internal stack traces, DB constraints, or service names.

### 7.3 Submission State & Double-Click Protection
* Upon clicking `CREATE ACCOUNT`:
  * Button text changes to: `CREATING ACCOUNT...`
  * Button disabled (`cursor-not-allowed`, opacity `85%`).
  * Double-submission is strictly prevented.

---

## 8. Success Flow & Session Management

1. **Auto-Login Execution:**
   * Backend `POST /api/v1/auth/register` responds with HTTP 201 Created and an `AuthResponse` containing `accessToken`.
   * Frontend stores `accessToken` in secure client session storage.
   * Populates client user state (`auth.user = response.user`).
2. **Navigation Transition:**
   * Navigates directly to the default commerce destination: `/shop`.
   * If a protected-route return destination exists (e.g., customer initiated checkout as a guest), the flow routes to that existing destination.
   * No intermediate OTP, email verification gate, or custom redirect engine is required.

---

## 9. Responsive Layout & Mobile Behavior

* **Desktop (`>= 1024px`):**
  * Full 50/50 split-screen layout (`min-h-screen grid grid-cols-2`).
  * Left hardware vitrine displayed in full editorial format.
* **Tablet (`768px – 1023px`):**
  * Maintains 50/50 balance with proportional container padding (`p-8`).
* **Mobile (`< 768px`):**
  * Transitions gracefully to a single vertical column (`grid-cols-1`).
  * Left vitrine collapsed or simplified into a compact editorial masthead above the form to prioritize immediate input interaction.
  * Inputs and submit button maintain full width with `>= 44px` height.
  * Zero horizontal page overflow (`overflow-x-hidden`).

---

## 10. Stitch Canonical Artifact Specification

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `8f428d07a8364a6e9262be41cf9888f9`
* **Screen Title:** `TECHGEAR — Register`
* **Design Token Reference:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (*Obsidian & Paper*)
* **Screenshot URL:** [View Canonical Registration Screen](https://lh3.googleusercontent.com/aida/AEtjO1WJpjbjw2j6C7X3Ko1g2tAN6v7DN_QYb4mGvjaPah3ORgCdOJebqpTeOnUhAZbdR4hhnOLEiV4k59K4A8IOX0da2udAEdfCVrx_tC9sL3H1YKRuV9JWFxJB83dv-deY1ZYB7EIW8_hO4jkR-2dEBHOy7SLu7S1Kow80IvvOz11c0bS1Ua96mNj2pKgW1b2Oep1t6MJ_7nYGLK0UmGdnUZVN_YOGYZgbj1M_bX-2apLbqq3sZHMvVLnQ)
* **Visual Audit Confirmation:**
  * Layout: Full-height 50/50 split-screen composition matching FE-12.
  * Left Vitrine: Clean studio hardware photography (machined aluminum mechanical keyboard) centered within crisp white matting surrounded by generous negative space against `#F5F5F4` background. Completely free of decorative headers (`TECHGEAR CATALOG`), amber status dots, and unverified brand claims (`PRECISION HARDWARE // EST. 2024`).
  * Right Form: Clean form container featuring `CREATE ACCOUNT` headline, two-column First/Last Name, Email, Phone (optional), Password with criteria caption, Confirm Password, primary black `CREATE ACCOUNT` button (`rounded-[4px]`), and secondary `Sign in` link to `/login`.
  * Footer: Muted single copyright line `© 2026 TECHGEAR INC. ALL RIGHTS RESERVED.` with zero fake security claims.
  * Visual Quality: Pure Swiss typography, zero gradients, zero shadows, zero AI slop.

---

## 11. Final Verification Checklist

- [x] Register endpoint verified (`POST /api/v1/auth/register` returning HTTP 201 Created)
- [x] Request fields verified (`firstName`, `lastName`, `email`, `password`, `phone`)
- [x] Optional / required fields verified (`firstName`, `lastName`, `email`, `password` required; `phone` optional)
- [x] Validation verified (`@NotBlank`, `@Email`, `@Size`, `@Pattern`)
- [x] Password rules verified (8+ chars, uppercase, lowercase, digit, special character)
- [x] Duplicate email behavior verified (Throws `UserAlreadyExistsException` &rarr; HTTP 409 Conflict)
- [x] Response verified (`AuthResponse`: `accessToken`, `tokenType`, `expiresIn`, `user`)
- [x] Auto-login behavior verified (`UserService.register()` creates token and logs in automatically)
- [x] JWT behavior verified (Bearer token returned in `AuthResponse`)
- [x] OTP / email verification verified (Confirmed absent)
- [x] Login link verified (`/login`)
- [x] FE-12 visual language reused (Obsidian & Paper, identical 50/50 split layout, Swiss typography)
- [x] Split-screen composition retained
- [x] No fake features (No OAuth, no avatar upload, no address, no marketing checkboxes)
- [x] No fake security claims (Removed `'SECURE 256-BIT TLS'`)
- [x] No scope creep
- [x] Responsive behavior specified
- [x] Exactly 1 canonical Stitch screen created (`8f428d07a8364a6e9262be41cf9888f9`)
- [x] Documentation created (`frontend/docs/FE-13-REGISTER-DESIGN.md`)
- [x] FE-01 &rarr; FE-12 unchanged
- [x] No backend / package / Git changes

---

**FE-13 STATUS: READY FOR REVIEW**
