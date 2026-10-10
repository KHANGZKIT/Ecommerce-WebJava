# TECHGEAR — ADMIN-05 Admin Users Design Specification

> **Document ID:** ADMIN-05-USERS-DESIGN  
> **Route:** `/admin/users`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `9a07c1fcf0a74a54b9bc9905218ba508`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [ADMIN-02](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-02-DASHBOARD-DESIGN.md), [FE-14](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-14-PROFILE-DESIGN.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md)  
> **Backend Authority:** Spring Boot 3.2 `user-service` & `api-gateway`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Core Purpose

The **Admin Users** screen (`/admin/users`) provides the central directory and identity inspection surface for TECHGEAR administrators. It enables administrative operators to view the complete register of authenticated user accounts, inspect individual user profile metadata, and execute permanent account deletions.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CORE CAPABILITIES                               │
│                                                                        │
│   1. LIST USERS         GET /api/v1/users       (Unpaginated List)     │
│   2. VIEW USER DETAIL   GET /api/v1/users/{id}  (Profile Inspection)   │
│   3. DELETE USER        DELETE /api/v1/users/{id} (Hard Deletion)      │
└────────────────────────────────────────────────────────────────────────┘
```

### Strict YAGNI & Anti-Enterprise Boundaries
* **Core Capabilities Supported (`BACKEND-CONFIRMED`):**
  1. **LIST:** Unpaginated list of registered users (`GET /api/v1/users`).
  2. **VIEW DETAIL:** Read-only inspection modal showing complete verified user metadata (`GET /api/v1/users/{id}`).
  3. **DELETE:** Permanent account removal with high-friction confirmation dialog (`DELETE /api/v1/users/{id}`).
* **Key Backend Behavioral Facts (`BACKEND-CONFIRMED`):**
  * `GET /api/v1/users` returns `List<UserProfileDto>`, **not a paginated `Page<UserProfileDto>`**.
  * Endpoint accepts **no pagination query parameters** (`page`, `size`) and **no sorting parameters** (`sort`).
  * Endpoint accepts **no search or filter parameters** (`keyword`, `role`, `status`).
  * `DELETE /api/v1/users/{id}` is a **HARD DELETE** (`userRepository.deleteById(id)`). It physically erases the record from the `users` table. There is no soft delete (`active` or `deleted` flag) for users.
  * **Self-Deletion Behavior:** The backend has **zero self-deletion prevention** (`UserController.java:54-59` and `UserService.java:113-120` do not check the caller's ID against the path variable). If an admin sends `DELETE /api/v1/users/{currentAdminId}`, the backend physically deletes the admin account and returns HTTP 204.
  * Role values are strictly `CUSTOMER` and `ADMIN` (`UserRole.java:3-6`).
  * Account status is a read-only boolean flag (`enabled`).
* **Explicit Enterprise Scope Exclusions (`SCOPE LIMITATION`):**
  * **No Edit User / Update Account:** Backend provides no admin-level user modification endpoint (only customer self-update via `PATCH /users/me`).
  * **No Password Reset / Change Password:** Backend has no credential reset or temporary password generation API.
  * **No Impersonation ("Login as User"):** No identity switching or token spoofing capability.
  * **No Role Management / Role Editor:** No API exists to promote customers or modify roles.
  * **No Status Toggle / Ban / Suspend:** No API exists to change `enabled` state. No toggle switch is displayed.
  * **No CRM / Marketing / Customer Segments:** No customer tags, lifetime value, VIP tiers, or internal operator notes.
  * **No Synthetic Metrics:** No spending total, order tally, device telemetry, or last-login timestamp in the user table.
  * **No Export / Bulk Operations:** No CSV/Excel export, multi-select checkboxes, or bulk deletion tools.
  * **No Audit Trail / Login History:** No session logs or authentication history endpoints.

---

## 2. Route & Access Control

* **Canonical Route:** `/admin/users`
* **Route Type:** Protected Administrative Route.
* **Actor & Permissions Matrix:**

| Actor | Action / Method | Behavior & UX Response | Enforcement Layer | Classification |
| :--- | :--- | :--- | :--- | :--- |
| **Guest (Unauthenticated)** | Any | Intercepted by auth guard; navigates to `/login` | `FE-15 Auth Guard` | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Any | Renders inline **Access Denied** vitrine; does **NOT** log out | Client Route Guard | `UI DESIGN DECISION` |
| **Customer (`CUSTOMER` role)**| Direct API request | HTTP 403 Forbidden | `UserController.java:41` (`hasRole('ADMIN')`) | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /api/v1/users` | HTTP 200 OK — loads unpaginated user list | `UserController.java:40-44` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `GET /api/v1/users/{id}` | HTTP 200 OK — returns single `UserProfileDto` | `UserController.java:46-51` | `BACKEND-CONFIRMED` |
| **Admin (`ADMIN` role)** | `DELETE /api/v1/users/{id}` | HTTP 204 No Content — permanently deletes record | `UserController.java:53-59` | `BACKEND-CONFIRMED` |

### Security Guard Architecture (FE-15 Alignment)

```
                     ┌───────────────────────────────────┐
                     │ Access Route: /admin/users        │
                     └─────────────────┬─────────────────┘
                                       │
                        Is user authenticated (JWT)?
                                       │
                        ┌──────────────┴──────────────┐
                       YES                            NO
                        │                              │
             Is role === 'ADMIN'?               Navigate to /login
                        │
             ┌──────────┴──────────┐
            YES                    NO
             │                      │
       Render User Table      Render Inline
       (/admin/users View)    "ACCESS DENIED"
                              (Do NOT log out)
```

* **403 Forbidden Policy:** When a non-admin authenticated user attempts access, the UI presents a restrained, non-dismissible Access Denied notification. The user's active session is **never destroyed or invalidated** upon a 403 response.

---

## 3. Backend Verification & Integration Contracts

### 3.1 Verified Source Code References

| Capability | Backend File & Line | Method & Signature | Response Type | Verified Implementation Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **List Users** | `UserController.java:40-44` | `getAllUsers()` | `List<UserProfileDto>` | Calls `userService.getAllUsers()` &rarr; `userRepository.findAll()`. Unpaginated, unsorted. |
| **Get User Detail** | `UserController.java:46-51` | `getUserById(@PathVariable Long id)` | `UserProfileDto` | Calls `userService.getUserById(id)` &rarr; `findById(id)`. Throws 404 if absent. |
| **Delete User** | `UserController.java:53-59` | `deleteUser(@PathVariable Long id)` | `Void` (HTTP 204) | Calls `userService.deleteUser(id)` &rarr; `userRepository.deleteById(id)`. Permanent hard delete. |
| **Security Config**| `SecurityConfig.java:43-44` | `securityFilterChain()` | N/A | Enforces `.requestMatchers(GET, "/api/v1/users").hasRole("ADMIN")` and `(DELETE, "/api/v1/users/**").hasRole("ADMIN")`. |
| **Error Handling** | `GlobalExceptionHandler.java:24-29` | `handleApiException()` | `ErrorResponse` | Formats `UserNotFoundException` as HTTP 404 with timestamp and message. |

---

### 3.2 Request & Response Data Contracts

#### A. List Users Contract (`GET /api/v1/users`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/users`
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Query Parameters:** None (`BACKEND-CONFIRMED`).
* **Pagination:** None — returns raw JSON array of all registered users.
* **Sorting:** None — returns natural database insertion order (`id` ascending).
* **Response Status:** `200 OK`
* **Response Payload Schema (`List<UserProfileDto>`):**
  ```json
  [
    {
      "id": 1,
      "firstName": "Marcus",
      "lastName": "Vance",
      "email": "marcus.vance@techgear.io",
      "phone": "+1 555-0192",
      "role": "ADMIN",
      "enabled": true,
      "createdAt": "2026-01-10T08:30:15",
      "updatedAt": "2026-01-10T08:30:15"
    },
    {
      "id": 2,
      "firstName": "Elena",
      "lastName": "Rostova",
      "email": "elena.r@designstudio.co",
      "phone": "+1 555-0144",
      "role": "CUSTOMER",
      "enabled": true,
      "createdAt": "2026-01-12T11:15:24",
      "updatedAt": "2026-01-16T09:40:12"
    },
    {
      "id": 7,
      "firstName": "Thomas",
      "lastName": "Wright",
      "email": "twright@archflow.io",
      "phone": "+1 555-0129",
      "role": "CUSTOMER",
      "enabled": false,
      "createdAt": "2026-01-22T13:05:00",
      "updatedAt": "2026-01-22T13:05:00"
    }
  ]
  ```

#### B. Get User by ID Contract (`GET /api/v1/users/{id}`)
* **HTTP Method:** `GET`
* **Path:** `/api/v1/users/{id}`
* **Path Parameter:** `id: Long` (e.g., `2`)
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Response Status:** `200 OK`
* **Response Payload (`UserProfileDto`):**
  ```json
  {
    "id": 2,
    "firstName": "Elena",
    "lastName": "Rostova",
    "email": "elena.r@designstudio.co",
    "phone": "+1 555-0144",
    "role": "CUSTOMER",
    "enabled": true,
    "createdAt": "2026-01-12T11:15:24",
    "updatedAt": "2026-01-16T09:40:12"
  }
  ```

#### C. Delete User Contract (`DELETE /api/v1/users/{id}`)
* **HTTP Method:** `DELETE`
* **Path:** `/api/v1/users/{id}`
* **Path Parameter:** `id: Long`
* **Headers:** `Authorization: Bearer <admin_jwt_token>`
* **Response Status:** `204 No Content` (Empty body)
* **Error Response (User Not Found - 404):**
  ```json
  {
    "status": 404,
    "message": "User not found with id: 99",
    "timestamp": "2026-10-08T19:00:00.000"
  }
  ```

---

### 3.3 Field-by-Field Backend Verification Matrix

Every single field rendered in the UI is audited directly from `UserProfileDto.java` and `User.java`:

| DTO Field | Java Type | Nullable? | Exact Backend Meaning | UI Presentation & Treatment |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `Long` | No | Primary key ID in `users` table | `#01`, `#02` (JetBrains Mono `13px`, text `#78716C`) |
| `firstName` | `String` | No | First name (max 100 chars) | Combined with `lastName` into full name |
| `lastName` | `String` | No | Last name (max 100 chars) | Bold `#1C1917` (Plus Jakarta Sans `14px`) |
| `email` | `String` | No | Unique login email credential | JetBrains Mono / Sans `13px` `#57534E` |
| `phone` | `String` | Yes | Optional contact phone (unique if present) | Monospace formatted or em-dash `—` if null |
| `role` | `UserRole` | No | System role enum: `CUSTOMER` or `ADMIN` | Pill badge: `ADMIN` (Obsidian) / `CUSTOMER` (Raised) |
| `enabled` | `boolean` | No | Account active flag (default `true`) | Status badge: `ENABLED` (Green) / `DISABLED` (Red) |
| `createdAt` | `LocalDateTime` | No | Entity creation timestamp | Formatted date `YYYY-MM-DD HH:mm` (JetBrains Mono) |
| `updatedAt` | `LocalDateTime` | No | Last update timestamp | Formatted in inspection modal |

#### Strictly Forbidden Fields (`SCOPE LIMITATION`)
The following fields **DO NOT EXIST** in `UserProfileDto` or `User` and must **NEVER** be displayed or simulated:
* ❌ `spending` / `totalSpent` / `revenue`
* ❌ `orderCount` / `orders` / `cartItems`
* ❌ `customerValue` / `lifetimeValue`
* ❌ `lastLogin` / `lastLoginAt` / `lastActive`
* ❌ `device` / `ipAddress` / `userAgent`
* ❌ `address` / `shippingAddress` / `city`
* ❌ `verificationBadge` / `emailVerified`
* ❌ `loyaltyTier` / `rewardPoints`
* ❌ `avatarUrl` / `profileImage`

---

## 4. Operational Boundaries: Pagination, Sorting & Filtering

### 4.1 Pagination Grounding
* **Backend Status (`BACKEND-CONFIRMED`):**
  * `GET /api/v1/users` returns `List<UserProfileDto>`, not `Page<UserProfileDto>`.
  * The backend provides **zero pagination parameters** (`page`, `size` are ignored).
* **UI Design Rule (`UI DESIGN DECISION`):**
  * **No Fake Pagination:** The UI renders the full list of users directly in a clean, scrollable tabular frame.
  * No `[ Prev ] [ 1 ] [ 2 ] [ Next ]` pagination widget.
  * Registry count badge in header displays the exact array length: `TOTAL: {users.length} USERS` (`DESIGN MOCK DATA: 8 USERS`).

### 4.2 Sorting Grounding
* **Backend Status (`BACKEND-CONFIRMED`):**
  * Endpoint accepts no `sort` query parameter.
  * Database rows are retrieved via `userRepository.findAll()`, returned in primary key order (`id` ASC).
* **UI Design Rule (`UI DESIGN DECISION`):**
  * The UI does not issue sorting query parameters to the backend.
  * Any client-side table sorting (e.g. clicking column headers) operates strictly in memory over the loaded array.

### 4.3 Search & Filtering Grounding
* **Backend Status (`BACKEND-CONFIRMED`):**
  * `UserController` has no `@RequestParam` for `keyword`, `search`, `role`, or `status`.
* **UI Design Rule (`UI DESIGN DECISION`):**
  * The Admin UI does **NOT** present an advanced server-side search input or multi-faceted filter drawer.
  * If a client-side search input is present, it is strictly an in-memory client text filter across loaded `name` and `email` properties. It does not invoke any network requests.

---

## 5. User Detail Inspection Specification

When an administrator clicks the `[ VIEW ]` action button on any user row, the interface opens the **User Profile Detail Modal**.

```
┌────────────────────────────────────────────────────────────────────────┐
│ USER PROFILE DETAIL                                         [#02]  [✕] │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  USER ID                   SYSTEM ROLE                                 │
│  2                         CUSTOMER                                    │
│  ────────────────────────  ──────────────────────────────────────────  │
│  FULL NAME                 ACCOUNT STATUS                              │
│  Elena Rostova             ENABLED                                     │
│  ────────────────────────  ──────────────────────────────────────────  │
│  EMAIL ADDRESS             PHONE NUMBER                                │
│  elena.r@designstudio.co   +1 555-0144                                 │
│  ────────────────────────  ──────────────────────────────────────────  │
│  REGISTERED DATE           LAST MODIFIED                               │
│  2026-01-12 11:15:24       2026-01-16 09:40:12                         │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│  [ VIEW USER ORDERS ] →                                                │
├────────────────────────────────────────────────────────────────────────┤
│  [ CLOSE ]                                              [ DELETE USER ]│
└────────────────────────────────────────────────────────────────────────┘
```

### 5.1 Modal Architecture & Rationale
* **Why an Inline Modal (`UI DESIGN DECISION`):**
  * `GET /api/v1/users/{id}` returns the exact same fields already present in `UserProfileDto`.
  * Opening a high-density modal maintains operator spatial context within `/admin/users` without unnecessary route navigation.
  * Avoids route bloat and preserves strict alignment with `ADMIN-01` sitemap architecture.
* **Metadata Key-Value Grid:**
  * **User ID:** Numeric ID formatted in monospaced font.
  * **System Role:** Read-only role badge (`CUSTOMER` or `ADMIN`).
  * **Full Name:** `firstName + " " + lastName`.
  * **Account Status:** Read-only status badge (`ENABLED` or `DISABLED`).
  * **Email Address:** Full email string.
  * **Phone Number:** Contact phone or `—` if unpopulated.
  * **Registered Date:** `createdAt` formatted to second precision (`YYYY-MM-DD HH:mm:ss`).
  * **Last Modified:** `updatedAt` formatted to second precision (`YYYY-MM-DD HH:mm:ss`).
* **Cross-Module Operational Link (`ADMIN-01 Alignment`):**
  * `[ VIEW USER ORDERS ] →` action button navigates directly to `/admin/orders?userId={id}`, utilizing the customer order lookup capability (`GET /api/v1/orders/user/{userId}`).
* **Footer Actions:**
  * `[ CLOSE ]`: Dismisses the modal dialog.
  * `[ DELETE USER ]`: Triggers the destructive deletion confirmation flow for this user.

---

## 6. User Deletion Semantics & Self-Deletion Behavior

### 6.1 Delete Semantics (`BACKEND-CONFIRMED`)
* **Physical Hard Delete:**
  * Endpoint: `DELETE /api/v1/users/{id}`
  * Implementation: `userRepository.deleteById(id)` physically issues `DELETE FROM users WHERE id = ?`.
  * **Unrecoverable:** Deletion is permanent and immediate. The record is permanently removed from the database.
  * **No Soft Delete:** There is no `active = false` state and no restore API.
  * **Cross-Service Constraints:** In this microservices architecture, `order-service` stores `userId` as an independent numeric column without foreign key constraints to the `users` table. Deleting a user does not fail on DB foreign key locks.
* **HTTP Status Code:** Returns `204 No Content` on success; `404 Not Found` if user ID does not exist.

---

### 6.2 Self-Deletion Audit & Verification (`CRITICAL`)

```
==========================================================================
CRITICAL AUDIT FINDING: SELF-DELETION BEHAVIOR
==========================================================================
Backend Source: UserController.java:53-59 & UserService.java:113-120

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
}

FINDING:
The backend does NOT verify if @PathVariable Long id matches the currently
authenticated administrator's ID. 

If an admin sends DELETE /api/v1/users/{currentAdminId}, the backend executes
deleteById(id) and permanently destroys the caller's account.

POLICY:
DO NOT INVENT a policy claiming "Admin cannot delete themselves".
Backend is the ultimate authority. UI documents this exact behavior.
==========================================================================
```

#### UI Handling for Self-Deletion (`UI DESIGN DECISION`)
To protect against accidental administrative lockout while remaining strictly honest to backend capabilities:
1. **Visual Identification:**
   * The UI inspects the active session (`userId` and `email` from JWT token).
   * The row matching the currently logged-in administrator displays a distinct `(You)` badge in the table and modal header.
2. **Standard Deletion Confirmation Modal:**
   * Used when deleting other accounts (`id !== currentAdminId`):
     ```
     ┌──────────────────────────────────────────────────────────────────┐
     │ DELETE USER ACCOUNT                                          [✕] │
     ├──────────────────────────────────────────────────────────────────┤
     │ Permanent Action Confirmation                                    │
     │                                                                  │
     │ Are you sure you want to delete the user account for:            │
     │                                                                  │
     │   Elena Rostova (elena.r@designstudio.co)                        │
     │   User ID: #02                                                   │
     │                                                                  │
     │ WARNING: This action performs an unrecoverable hard delete.      │
     │ All credentials and profile data will be permanently purged.     │
     ├──────────────────────────────────────────────────────────────────┤
     │ [ CANCEL ]                                       [ DELETE USER ] │
     └──────────────────────────────────────────────────────────────────┘
     ```
3. **High-Friction Self-Deletion Warning Modal:**
   * Triggered **only** when an administrator clicks delete on their **own** account (`id === currentAdminId`):
     ```
     ┌──────────────────────────────────────────────────────────────────┐
     │ ⚠️ CRITICAL: SELF-DELETION WARNING                            [✕] │
     ├──────────────────────────────────────────────────────────────────┤
     │ You are attempting to delete your OWN administrator account:     │
     │                                                                  │
     │   Marcus Vance (marcus.vance@techgear.io)                        │
     │   User ID: #01 [ACTIVE SESSION]                                  │
     │                                                                  │
     │ CONSEQUENCES:                                                    │
     │ 1. Your credentials will be permanently erased immediately.      │
     │ 2. Your current session will terminate upon completion.          │
     │ 3. You will be redirected to /login and cannot re-authenticate.  │
     │                                                                  │
     │ [ ] I understand this will immediately destroy my admin access   │
     ├──────────────────────────────────────────────────────────────────┤
     │ [ CANCEL ]                             [ CONFIRM SELF-DELETE ]   │
     └──────────────────────────────────────────────────────────────────┘
     ```
   * The destructive action button remains disabled until the confirmation checkbox is checked.
   * If confirmed, the API request `DELETE /api/v1/users/{id}` is executed. Upon receiving HTTP 204, the client purges local tokens and redirects to `/login`.

---

## 7. Role & Status Specifications

### 7.1 Role Specification (`BACKEND-CONFIRMED`)
* **Source:** `com.ecommerce.userservice.model.UserRole`:
  ```java
  public enum UserRole {
      CUSTOMER,
      ADMIN
  }
  ```
* **Strict Limitations:**
  * Only two roles exist: `CUSTOMER` and `ADMIN`.
  * **No intermediate roles:** Zero `SUPER_ADMIN`, `STAFF`, `MODERATOR`, or `MANAGER`.
  * **Read-Only Display:** Role is displayed as a non-interactive badge:
    * `ADMIN`: Obsidian badge (`bg: #1C1917`, `text: #FAFAF9`, `font: JetBrains Mono 10px`, `border: 1px solid #1C1917`).
    * `CUSTOMER`: Neutral badge (`bg: #F5F5F4`, `text: #57534E`, `font: JetBrains Mono 10px`, `border: 1px solid #E7E5E4`).
  * **No Role Modification:** No role dropdown, promote action, or role editor exists.

### 7.2 Status / Enabled Specification (`BACKEND-CONFIRMED`)
* **Source:** `User.java:49-50` (`private boolean enabled = true;`) and `UserProfileDto.java:22` (`private boolean enabled;`).
* **Strict Limitations:**
  * Account status is a boolean flag indicating whether the account is unlocked.
  * **Read-Only Status Display:**
    * `ENABLED`: Positive badge (`bg: #F0FDF4`, `border: 1px solid #BBF7D0`, `text: #15803D`).
    * `DISABLED`: Destructive badge (`bg: #FEF2F2`, `border: 1px solid #FECACA`, `text: #DC2626`).
  * **No Status Toggle:** The backend exposes **no enable, disable, ban, or suspend endpoint**.
  * The UI **never** renders a toggle switch or button to modify `enabled` status.

---

## 8. Screen Layout & Wireframes

### 8.1 Desktop Canonical Wireframe (`/admin/users`)

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR // ADMIN                                                 admin@techgear.io [ADMIN]  [LOG OUT] │
├────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ DASHBOARD     PRODUCTS     CATEGORIES     [ USERS ]     ORDERS     PAYMENTS                            │
│                                           ═════════                                                    │
├────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                        │
│ ADMIN / USERS                                                                                          │
│ USER DIRECTORY                                                          [ TOTAL: 8 USERS ]             │
│ Complete register of system accounts, role privileges, and identity records.                          │
│                                                                                                        │
│ ┌────────────────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ ID   NAME            EMAIL                     PHONE         ROLE       STATUS    CREATED AT ACTIONS│ │
│ ├────────────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ #01  Marcus Vance    marcus.vance@techgear.io  +1 555-0192   ADMIN (You)ENABLED   2026-01-10 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #02  Elena Rostova   elena.r@designstudio.co   +1 555-0144   CUSTOMER   ENABLED   2026-01-12 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #03  David Kim       d.kim@soundlab.org        +1 555-0188   CUSTOMER   ENABLED   2026-01-14 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #04  Sarah Jenkins   sarah.j@techgear.io       +1 555-0172   ADMIN      ENABLED   2026-01-15 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #05  Michael Torres  mtorres@craftworks.dev    —             CUSTOMER   ENABLED   2026-01-18 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #06  Anya Petrova    anya@nordicstudio.se      +46 8 123 456 CUSTOMER   ENABLED   2026-01-20 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #07  Thomas Wright   twright@archflow.io       +1 555-0129   CUSTOMER   DISABLED  2026-01-22 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ │ #08  Jane Smith      jane.smith@modular.com    —             CUSTOMER   ENABLED   2026-01-25 [VIEW] │ │
│ │                                                                                             [DELETE]│ │
│ └────────────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                                        │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Visual System & Design Tokens

Following the **Obsidian & Paper** design system:

| Visual Element | Design Token / Specification | Applied Context |
| :--- | :--- | :--- |
| **Page Canvas** | `#FAFAF9` (`--color-bg-canvas`) | Background for the entire admin view |
| **Surface Base** | `#FFFFFF` (`--color-surface-base`) | Table container, modal cards, nav rail |
| **Surface Raised**| `#F5F5F4` (`--color-surface-raised`) | Table headers, inset telemetry zones, inactive chips |
| **Primary Text** | `#1C1917` (`--color-text-primary`) | Page title, user names, primary labels |
| **Secondary Text**| `#57534E` (`--color-text-secondary`) | Email addresses, descriptions, field hints |
| **Muted Metadata**| `#78716C` (`--color-text-tertiary`) | User IDs, timestamps, breadcrumbs, table headers |
| **Hairline Dividers**| `1px solid #E7E5E4` | Table borders, row rules, modal dividers |
| **Interactive Focus**| `#D97706` (Signal Amber) | Keyboard focus ring (2px offset) |
| **Destructive Action**| `#DC2626` / Hover `#B91C1C` | `[ DELETE ]` action buttons and danger modal |
| **Success Status** | `#15803D` text on `#F0FDF4` bg | `ENABLED` status badge |
| **Disabled Status**| `#DC2626` text on `#FEF2F2` bg | `DISABLED` status badge |
| **Typography (Headings)**| Plus Jakarta Sans (Bold / Semi-bold) | Page title (28px), Section titles (16px), Modal headers |
| **Typography (Data/Code)**| JetBrains Mono | User IDs (`#01`), Roles, Statuses, Timestamps, Metrics |
| **Border Radius** | `4px` (Controls), `6px` (Cards/Modals) | Machined industrial geometric corners |

---

## 10. Error Handling & Loading States

### 10.1 Loading State
* Rendered while `GET /api/v1/users` is resolving.
* Displays a high-density skeleton table containing 6 rows with shimmering `#E7E5E4` blocks for ID, name, email, phone, role, status, and actions.
* Prevents layout shifts.

### 10.2 Empty State
* Rendered if `GET /api/v1/users` returns an empty array `[]`.
* Container: Inset `#FFFFFF` card with `1px solid #E7E5E4` border.
* Typography:
  * Headline: `NO USERS REGISTERED` (Plus Jakarta Sans `16px` bold `#1C1917`).
  * Body: `The user directory currently contains zero registered accounts.` (text `#57534E`).

### 10.3 API Error State (HTTP 500 / Network Error)
* Rendered if the list request fails.
* Message: `FAILED TO LOAD USER DIRECTORY. Please verify network connectivity or microservice availability.`
* Action: Compact rectangular `[ RETRY ]` button triggering re-fetch.
* **Security Hygiene:** The UI **never leaks** internal stack traces, SQL error statements, Spring class names, or JWT token details.

### 10.4 Delete Error Handling
* **404 Not Found:** User was already removed by another operator. UI displays toast: `"User not found or already deleted."` and refreshes user list.
* **403 Forbidden:** Administrative session expired. Redirects to `/login`.
* **500 Internal Error:** UI displays toast: `"An unexpected server error occurred while deleting user."`

---

## 11. Responsive Architecture

### 11.1 Breakpoint Strategy

```
┌─────────────────┬─────────────────┬──────────────────────────────────────────┐
│ Breakpoint      │ Width           │ Adaptation Strategy                      │
├─────────────────┼─────────────────┼──────────────────────────────────────────┤
│ Desktop         │ ≥ 1280px        │ Full 8-column data table.                │
│ Tablet          │ 768px – 1279px  │ Hide PHONE and CREATED AT columns.       │
│ Mobile          │ < 768px         │ Card stack layout; 44px touch targets.   │
└─────────────────┴─────────────────┴──────────────────────────────────────────┘
```

### 11.2 Tablet View (768px – 1279px)
* Columns displayed: `ID`, `NAME`, `EMAIL`, `ROLE`, `STATUS`, `ACTIONS`.
* `PHONE` and `CREATED AT` are omitted from the table to prevent horizontal overflow; both fields remain fully visible inside the `[ VIEW ]` detail modal.

### 11.3 Mobile Card Stack (< 768px)
* Table transforms into a vertical list of individual user cards:
  ```
  ┌────────────────────────────────────────────────────────┐
  │ #02 · ELENA ROSTOVA                   [ CUSTOMER ]     │
  │ elena.r@designstudio.co               [ ENABLED ]      │
  │ Phone: +1 555-0144                                     │
  │ Created: 2026-01-12                                    │
  ├────────────────────────────────────────────────────────┤
  │ [ VIEW DETAILS ]                       [ DELETE ]      │
  └────────────────────────────────────────────────────────┘
  ```
* All touch targets maintain minimum dimensions of `44px × 44px`.

---

## 12. Canonical Stitch Screen Reference

* **Stitch Project ID:** `113306237264437644`
* **Canonical Screen ID:** `9a07c1fcf0a74a54b9bc9905218ba508`
* **Title:** `TECHGEAR — Admin Users`
* **Device Type:** `DESKTOP`
* **Design System Asset:** `assets/592f25a1d5524b0091e02d25cfc6dd89` (Obsidian & Paper)
* **Screen Contents:**
  1. Top sticky admin header (`TECHGEAR // ADMIN` with navigation and active `USERS` indicator).
  2. Page header with breadcrumbs `ADMIN / USERS`, title `USER DIRECTORY`, and registry counter badge `TOTAL: 8 USERS`.
  3. High-density data table rendering 8 realistic user records with verified fields (`ID`, `NAME`, `EMAIL`, `PHONE`, `ROLE`, `STATUS`, `CREATED AT`, `ACTIONS`).
  4. Active **User Profile Detail Modal** overlay demonstrating the inspect state for user `#02 Elena Rostova` with metadata grid, direct orders link (`[ VIEW USER ORDERS ] →`), close button, and delete action.

---

## 13. Scope Classification Summary Table

| Category / Capability | Exact Backend / UI State | Classification Tag | Source Reference / Rationale |
| :--- | :--- | :--- | :--- |
| **List Users Endpoint** | `GET /api/v1/users` | `BACKEND-CONFIRMED` | `UserController.java:40-44` |
| **Get User Detail Endpoint**| `GET /api/v1/users/{id}` | `BACKEND-CONFIRMED` | `UserController.java:46-51` |
| **Delete User Endpoint**| `DELETE /api/v1/users/{id}` | `BACKEND-CONFIRMED` | `UserController.java:53-59` |
| **Delete Semantics** | Hard delete via `userRepository.deleteById(id)` | `BACKEND-CONFIRMED` | `UserService.java:113-119` |
| **Self-Deletion Behavior** | Backend does not block self-delete | `BACKEND-CONFIRMED` | `UserController.java:54` & `UserService.java:113` |
| **Self-Delete UI Guard** | `(You)` badge + high-friction warning modal | `UI DESIGN DECISION` | Prevents accidental session lockout |
| **User Roles** | Exactly `CUSTOMER` and `ADMIN` | `BACKEND-CONFIRMED` | `UserRole.java:3-6` |
| **Status Field** | Boolean `enabled` (read-only) | `BACKEND-CONFIRMED` | `UserProfileDto.java:22` |
| **Pagination** | None (unpaginated list) | `BACKEND-CONFIRMED` | Returns `List<UserProfileDto>` |
| **Sorting** | None (natural ID order) | `BACKEND-CONFIRMED` | No `sort` parameter in controller |
| **Search / Filter API** | None | `BACKEND-CONFIRMED` | No query params in controller |
| **Total Users Count** | Design mockup: `TOTAL: 8 USERS` | `DESIGN MOCK DATA` | Runtime uses `users.length` |
| **User Records in Wireframe**| Marcus Vance, Elena Rostova, etc. | `DESIGN MOCK DATA` | Representative seed data |
| **Inspect Detail Modal** | High-density dialog overlay | `UI DESIGN DECISION` | Simple, contained within `/admin/users` |
| **View User Orders Link** | Navigates to `/admin/orders?userId={id}` | `UI DESIGN DECISION` | Implements `ADMIN-01:254` cross-navigation |
| **Forbidden CRM / Edits** | No edit, no reset pass, no CRM, no ban | `SCOPE LIMITATION` | Strict YAGNI — zero backend support |

---

## 14. Verification Checklist & Audit Pre-Flight

- [x] **User endpoints verified:** `GET /api/v1/users`, `GET /api/v1/users/{id}`, `DELETE /api/v1/users/{id}` audited directly against backend source code.
- [x] **DTO fields verified:** All table and modal fields match `UserProfileDto` (`id`, `firstName`, `lastName`, `email`, `phone`, `role`, `enabled`, `createdAt`, `updatedAt`).
- [x] **ADMIN permission verified:** Secured via `@PreAuthorize("hasRole('ADMIN')")` in `UserController` and `SecurityConfig`.
- [x] **Pagination verified:** Verified unpaginated (`List<UserProfileDto>`); zero fake pagination controls created.
- [x] **Sorting verified:** Verified no sort endpoint params; zero fake sorting API calls created.
- [x] **Search/filter verified:** Verified no backend search/filter params; no fake server search created.
- [x] **Detail behavior verified:** High-density read-only inspection modal with cross-link to orders (`/admin/orders?userId={id}`).
- [x] **Delete semantics verified:** Hard physical delete via `userRepository.deleteById(id)`; documented as permanent and unrecoverable.
- [x] **Self-delete behavior verified:** Backend contains no self-deletion check; documented accurately; UI introduces high-friction warning modal without fabricating fake backend restrictions.
- [x] **Role behavior verified:** Strictly `CUSTOMER` and `ADMIN`; read-only display; no role editor.
- [x] **Enabled/status behavior verified:** Strictly read-only boolean badge (`ENABLED` / `DISABLED`); no toggle switch or ban button invented.
- [x] **No fake fields:** Spending, order count, customer value, last login, device, address, badges, loyalty points completely excluded.
- [x] **No edit capability invented:** No edit user modal or update endpoint simulated.
- [x] **No security features invented:** No password reset, impersonation, or 2FA controls simulated.
- [x] **No CRM/analytics scope:** Zero charts, customer tags, lifetime value, or notes.
- [x] **Exactly 1 canonical Stitch screen:** Created screen `9a07c1fcf0a74a54b9bc9905218ba508` in project `113306237264437644`.
- [x] **Documentation created:** Written to `frontend/docs/ADMIN-05-USERS-DESIGN.md`.
- [x] **Existing screens/docs unchanged:** `ADMIN-01` through `ADMIN-04` and `FE-01` through `FE-15` untouched.
- [x] **No code/package/backend/Git changes:** Zero implementation code or configuration modifications made.

---

**ADMIN-05 STATUS: READY FOR REVIEW**
