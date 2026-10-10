# TECHGEAR — ADMIN-04 Admin Categories Design Specification

> **Document ID:** ADMIN-04-CATEGORIES-DESIGN  
> **Route:** `/admin/categories`  
> **Target Audience:** Authenticated Administrators (`ADMIN` role)  
> **Visual Direction:** Swiss Minimalism & Industrial Precision (Obsidian & Paper)  
> **Design System:** Obsidian & Paper (`assets/592f25a1d5524b0091e02d25cfc6dd89`)  
> **Stitch Screen ID:** `74a3b91aad1f442a88de9c46e58db0fe`  
> **Source Documents:** [ADMIN-01](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-01-ADMIN-SITEMAP-FLOW.md), [ADMIN-02](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-02-DASHBOARD-DESIGN.md), [ADMIN-03](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/ADMIN-03-PRODUCTS-DESIGN.md), [FE-11](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-11-CATEGORY-DESIGN.md), [FE-DESIGN-SYSTEM](file:///d:/CODE/WebJava/observable-microservices-ecommerce/frontend/docs/FE-DESIGN-SYSTEM.md)  
> **Backend Authority:** Spring Boot 3.2 `product-service` & `api-gateway`  
> **Status:** APPROVED & SPECIFIED — Strict YAGNI — Backend-Grounded

---

## 1. Executive Summary & Core Purpose

The **Admin Categories** screen (`/admin/categories`) provides the complete category taxonomy management surface for TECHGEAR administrators. It enables operators to view all active categories, create new categories, edit existing category metadata, and soft-delete categories that have no active product references.

### Strict YAGNI & Scope Boundaries
* **Core Capabilities Supported (`BACKEND-CONFIRMED`):**
  1. **LIST:** Unpaginated list of active categories (`GET /api/v1/categories`).
  2. **CREATE:** Create category form (`POST /api/v1/categories`).
  3. **EDIT:** Edit category metadata form (`PUT /api/v1/categories/{id}`).
  4. **SOFT DELETE:** Deactivate category with confirmation modal (`DELETE /api/v1/categories/{id}`).
* **Key Backend Behavioral Facts (`BACKEND-CONFIRMED`):**
  * `GET /api/v1/categories` returns **active-only** categories via `categoryRepository.findAllByActiveTrue()`.
  * `DELETE /api/v1/categories/{id}` is a **soft-delete** (`category.setActive(false)`), NOT a hard delete.
  * Delete is **blocked with HTTP 409 Conflict** if `productRepository.existsByCategoryIdAndActiveTrue(id)` returns `true`.
  * Once soft-deleted, a category **disappears from the list** — no restore endpoint exists.
  * `PUT /api/v1/categories/{id}` reuses `CreateCategoryRequest` — there is no separate `UpdateCategoryRequest` DTO.
  * The category list is **unpaginated** (returns `List<CategoryDto>`, not `Page<CategoryDto>`).
* **Explicit Enterprise Scope Exclusions (`SCOPE LIMITATION`):**
  * **No Nested Categories:** No parent category, category tree, or hierarchy.
  * **No Slug Manager:** Backend has no `slug` field on `Category` entity.
  * **No SEO Manager:** No SEO title, SEO description, or meta tag fields.
  * **No Banner / Image Manager:** `imageUrl` is a plain text field, not an asset uploader.
  * **No Category Ordering:** No `sortOrder` or `position` field exists.
  * **No Product Count System:** No product count API exists; no fetching products to compute count.
  * **No Restore Workflow:** No endpoint to reactivate a soft-deleted category.
  * **No Category Analytics:** No revenue, performance, ranking, or trending data.
  * **No Bulk Operations:** No CSV import/export, multi-select, bulk edit, or bulk delete.
  * **No Audit Log:** No audit trail API.

---

## 2. Route & Access Control

* **Canonical Route:** `/admin/categories`
* **Route Type:** Protected Administrative Route.
* **Actor & Permissions Matrix:**

| Actor | Action / Method | Behavior & UX Response | Enforcement Layer |
| :--- | :--- | :--- | :--- |
| **Guest (Unauthenticated)** | Any | Intercepted by auth guard; navigates to `/login` | `FE-15 Auth Guard` |
| **Customer (`CUSTOMER` role)**| Any | Renders inline **Access Denied** vitrine; does NOT log out | Client Route Guard |
| **Admin (`ADMIN` role)** | `GET /categories` (List) | HTTP 200 OK — loads active category list | `CategoryController.java:21-24` |
| **Admin (`ADMIN` role)** | `GET /categories/{id}` | HTTP 200 OK — returns single category | `CategoryController.java:26-29` |
| **Admin (`ADMIN` role)** | `POST /categories` (Create) | HTTP 201 Created — returns created `CategoryDto` | `CategoryController.java:31-34` |
| **Admin (`ADMIN` role)** | `PUT /categories/{id}` (Edit) | HTTP 200 OK — returns updated `CategoryDto` | `CategoryController.java:36-41` |
| **Admin (`ADMIN` role)** | `DELETE /categories/{id}` | HTTP 204 No Content — soft-deletes category | `CategoryController.java:43-47` |

---

## 3. Backend Verification & Integration Contracts

### 3.1 Verified Source Code References

| Capability | Backend File & Line | Method & Signature | Response Type | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **List Active Categories** | `CategoryController.java:21-24` | `getAllCategories()` | `List<CategoryDto>` | Calls `categoryService.getAllActiveCategories()` → `findAllByActiveTrue()`. Unpaginated. |
| **Get Category by ID** | `CategoryController.java:26-29` | `getCategoryById(@PathVariable Long id)` | `CategoryDto` | Uses `findById(id)` — can find inactive categories by ID, but they won't appear in list. |
| **Create Category** | `CategoryController.java:31-34` | `createCategory(@Valid @RequestBody CreateCategoryRequest request)` | `CategoryDto` (201) | Checks name uniqueness; sets `active = true`. |
| **Update Category** | `CategoryController.java:36-41` | `updateCategory(@PathVariable Long id, @Valid @RequestBody CreateCategoryRequest request)` | `CategoryDto` (200) | Reuses `CreateCategoryRequest`. Updates `name`, `description`; conditionally updates `imageUrl` (only if not null). |
| **Delete Category** | `CategoryController.java:43-47` | `deleteCategory(@PathVariable Long id)` | `Void` (204) | Checks `existsByCategoryIdAndActiveTrue(id)`. If active products exist → 409 Conflict. Otherwise sets `active = false`. |

---

### 3.2 Request & Response Data Contracts

#### A. List Categories Contract (`GET /api/v1/categories`)
* **Query Parameters:** None (`BACKEND-CONFIRMED`).
* **Pagination:** None — returns full `List<CategoryDto>` of all active categories.
* **Response Structure (`List<CategoryDto>`):**
  ```json
  [
    {
      "id": 1,
      "name": "Electronics",
      "description": "Electronic devices, computers, and accessories.",
      "imageUrl": "https://example.com/images/electronics-cat.png",
      "active": true,
      "createdAt": "2026-01-15T10:00:00"
    },
    {
      "id": 2,
      "name": "Keyboards & Mice",
      "description": "Precision mechanical keyboards and mice",
      "imageUrl": "https://example.com/images/cat-kbd.jpg",
      "active": true,
      "createdAt": "2026-01-15T12:00:00"
    }
  ]
  ```

#### B. Create Category Contract (`POST /api/v1/categories`)
* **Headers:** `Content-Type: application/json`, `Authorization: Bearer <accessToken>`
* **Payload Structure (`CreateCategoryRequest.java:9-22`):**
  ```json
  {
    "name": "Networking",
    "description": "Routers, switches and networking gear.",
    "imageUrl": "https://example.com/images/networking.jpg"
  }
  ```
* **Validation Rules (`BACKEND-CONFIRMED`):**
  * `name`: `@NotBlank(message = "Category name is required")`, `@Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")`. **Required.**
  * `description`: `@Size(max = 500, message = "Description cannot exceed 500 characters")`. **Optional.**
  * `imageUrl`: No validation annotation. **Optional.** Plain String.
* **Uniqueness Check (`CategoryService.java:40-42`):** `categoryRepository.existsByName(request.getName())` — if duplicate name exists, throws `CategoryAlreadyExistsException` → **HTTP 409 Conflict**.

#### C. Update Category Contract (`PUT /api/v1/categories/{id}`)
* **Headers:** `Content-Type: application/json`, `Authorization: Bearer <accessToken>`
* **Payload Structure:** Reuses `CreateCategoryRequest` — identical payload structure as Create.
* **Update Semantics (`CategoryService.java:54-59`):**
  * `name`: Always overwritten with request value.
  * `description`: Always overwritten with request value.
  * `imageUrl`: Conditionally updated — **only if `request.getImageUrl() != null`**. If null in request, existing `imageUrl` is preserved.
* **Validation Rules:** Same as Create (name required, description max 500).

#### D. Soft Delete Contract (`DELETE /api/v1/categories/{id}`)
* **Headers:** `Authorization: Bearer <accessToken>`
* **Response:** HTTP 204 No Content.
* **Delete Guard (`CategoryService.java:63-72`):**
  1. `findOrThrow(id)` — if category not found → **HTTP 404 Not Found** (`CategoryNotFoundException`).
  2. `productRepository.existsByCategoryIdAndActiveTrue(id)` — if active products reference this category → **HTTP 409 Conflict** (`CategoryInUseException`) with message: `"Cannot delete category '{name}' (id: {id}) because active products reference it"`.
  3. If no active products → sets `category.active = false` and saves. **Soft-delete.**
* **Verified by Test (`CategoryDeletionContractTest.java`):**
  * `categoryWithActiveProductsCannotBeDeleted()` — confirms 409 Conflict with exact error message.
  * `categoryWithoutActiveProductsCanBeSoftDeleted()` — confirms 204 and `active = false`.

---

### 3.3 CategoryDto Response Fields (`BACKEND-CONFIRMED`)

| Field | Type | Source | Notes |
| :--- | :--- | :--- | :--- |
| `id` | `Long` | `Category.id` | Auto-generated. |
| `name` | `String` | `Category.name` | Unique, max 100. |
| `description` | `String` | `Category.description` | Max 500. Nullable. |
| `imageUrl` | `String` | `Category.imageUrl` | Max 200. Nullable. |
| `active` | `boolean` | `Category.active` | Always `true` in list response (filtered by `findAllByActiveTrue`). |
| `createdAt` | `LocalDateTime` | `Category.createdAt` | JPA auditing. |

**Fields NOT in CategoryDto:** `updatedAt` (exists on entity but not exposed in DTO), `slug`, `parentId`, `sortOrder`, `productCount`.

---

### 3.4 Active/Inactive Behavior Summary

| Behavior | Verified Detail |
| :--- | :--- |
| **List returns** | **Active-only** (`findAllByActiveTrue`) |
| **Delete semantics** | **Soft-delete** (`active = false`) |
| **Delete blocked when** | Active products reference category (`existsByCategoryIdAndActiveTrue`) |
| **Restore endpoint** | **Does not exist** (`SCOPE LIMITATION`) |
| **Admin can toggle active?** | **No** — no endpoint or mechanism to set `active = true` on a deleted category |
| **Deleted category visible?** | **No** — disappears from `GET /categories` list after soft-delete |

---

## 4. UI Architecture & Screen Anatomy

### 4.1 Canonical Stitch Screen Identification
* **Screen ID:** `74a3b91aad1f442a88de9c46e58db0fe`
* **Title:** `TECHGEAR — Admin Categories`
* **Screenshot Artifact:** `projects/113306237264437644/files/a49a5020054b431f87550d103f91331c`
* **HTML Source Artifact:** `projects/113306237264437644/files/2eaac188360d4f859cdd631a211b435e`
* **Target Viewport:** Desktop `1440px` (max canvas `max-w-7xl`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ TECHGEAR // ADMIN                    DASHBOARD  PRODUCTS  [CATEGORIES]  USERS  ORDERS  PAYMENTS │ admin@techgear.io [ADMIN]  LOG OUT │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PORTAL // TAXONOMY MANAGEMENT                                                                    │
│ CATEGORIES                                                                   [ + NEW CATEGORY ]  │
│ Manage product category taxonomy. Create, edit, and deactivate categories.                      │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                          6 ACTIVE CATEGORIES     │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ CATEGORY NAME          DESCRIPTION                                    CREATED     ACTIONS     │ │
│ ├──────────────────────────────────────────────────────────────────────────────────────────────┤ │
│ │ Electronics            Electronic devices, computers...                15 JAN 26   [E] [D]   │ │
│ │ Keyboards & Mice       Precision mechanical keyboards...               15 JAN 26   [E] [D]   │ │
│ │ Audio                  Headphones, speakers, audio...                  20 JAN 26   [E] [D]   │ │
│ │ Displays               Monitors, screens and display...                01 FEB 26   [E] [D]   │ │
│ │ Accessories            Cables, hubs, docks, peripherals                10 FEB 26   [E] [D]   │ │
│ │ Networking             Routers, switches, networking...                01 MAR 26   [E] [D]   │ │
│ └──────────────────────────────────────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│ TECHGEAR OPERATIONAL ADMINISTRATION // V1.0                  ENDPOINT SCOPE: SPRING BOOT 3.2     │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

> *Wireframe Data Note:* "6 ACTIVE CATEGORIES" and all category rows are **DESIGN MOCK DATA**. At runtime, this binds dynamically to `List<CategoryDto>.size()`. Do not hard-code.

---

## 5. Detailed Component Specifications

### 5.1 Top Navigation & Header Station
* **Top Admin Header:** 64px sticky bar (`#FFFFFF` with `1px solid #E7E5E4`).
  * Wordmark: `TECHGEAR // ADMIN` (Plus Jakarta Sans `15px` bold).
  * Active Navigation Tab: `CATEGORIES` is active with high-contrast text `#1C1917` and a 2px hairline bottom indicator.
  * Right Rail: Administrator identity `admin@techgear.io [ADMIN]` and `LOG OUT` action.
* **Header Station:**
  * Breadcrumb: `PORTAL // TAXONOMY MANAGEMENT` in JetBrains Mono `11px` text `#78716C` uppercase tracking-wider.
  * Title: `CATEGORIES` in Plus Jakarta Sans `28px` font-bold text `#1C1917` uppercase tracking-tight.
  * Subtitle: `Manage product category taxonomy. Create, edit, and deactivate categories.` in Plus Jakarta Sans `14px` text `#57534E`.
  * Primary Action: `[ + NEW CATEGORY ]` button (bg `#1C1917`, text `#FAFAF9`, 4px radius, Plus Jakarta Sans `12px` font-semibold px-4 py-2.5 hover:bg-[#292524]).

---

### 5.2 Category Count Strip
* **Live Entity Count Vitrine:**
  * **DESIGN MOCK DATA:**
    `6 ACTIVE CATEGORIES`
  * **Runtime:**
    `List<CategoryDto>.size()`
  * **Rule:** Không hard-code 6. Bound dynamically to `{categories.length} ACTIVE CATEGORIES`.
  * Styling: JetBrains Mono `11px` font-semibold text `#1C1917` tracking-wider.

---

### 5.3 Categories Data Table
* **Container:** bg `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`, overflow-hidden, `shadow-none`.
* **No Pagination Required (`BACKEND-CONFIRMED`):** Category list is unpaginated (`List<CategoryDto>`). All active categories render in a single table without pagination controls.
* **Table Header Row (`h-10 bg-[#FAFAF9] border-b border-[#E7E5E4] px-4`):**
  * `CATEGORY NAME`: Left-aligned, Plus Jakarta Sans `11px` font-semibold text `#78716C` uppercase tracking-wider.
  * `DESCRIPTION`: Left-aligned, Plus Jakarta Sans `11px` text `#78716C` uppercase.
  * `CREATED`: Right-aligned, JetBrains Mono `11px` text `#78716C` uppercase.
  * `ACTIONS`: Right-aligned, JetBrains Mono `11px` text `#78716C` uppercase.
* **No STATUS Column (`UI DESIGN DECISION`):** `GET /api/v1/categories` returns **active-only** categories. Every row in the table is guaranteed `active = true`. A redundant `STATUS = ACTIVE` badge provides zero information. No active/inactive toggle. No restore action.

* **Table Rows (`h-12 border-b border-[#E7E5E4] px-4 hover:bg-[#FAFAF9] transition-colors`):**
  * **Category Name:** Plus Jakarta Sans `13px` font-medium text `#1C1917`.
  * **Description:** Plus Jakarta Sans `12px` text `#57534E`, truncated (`line-clamp-1` / `max-w-xs`).
  * **Created Date:** JetBrains Mono `12px` text `#78716C`. Formatted as `DD MMM YY` (e.g., `15 JAN 26`).
  * **Actions Rail (Right-Aligned):**
    * `[ EDIT ]`: Outline button (bg-white border `1px solid #E7E5E4` text `#1C1917` text-xs font-mono px-2.5 py-1 rounded `[4px]` hover:bg-[#F5F5F4]). Triggers Edit Category Modal.
    * `[ DELETE ]`: Outline button (bg-white border `1px solid #E7E5E4` text-[#DC2626] text-xs font-mono px-2.5 py-1 rounded `[4px]` hover:bg-[#FEF2F2]). Triggers Soft-Delete Confirmation Modal.

---

## 6. Modal & Action Workflows

### 6.1 Create Category Modal Workflow
* **Trigger:** Click `[ + NEW CATEGORY ]`.
* **Modal Architecture:**
  * Width: `max-w-md`, surface `#FFFFFF`, border `1px solid #E7E5E4`, rounded `[6px]`.
  * Title: `CREATE CATEGORY` (Plus Jakarta Sans `18px` font-bold text `#1C1917`).
* **Form Fields (`CreateCategoryRequest`):**
  1. **Category Name (`name`):** Text input, required. Placeholder e.g. `Electronics`. Min 2, max 100 characters.
  2. **Description (`description`):** Textarea, optional, max 500 chars. Placeholder e.g. `Electronic devices, computers, and accessories.`
  3. **Image URL (`imageUrl`):** Plain text URL input, optional. Placeholder `https://...`
* **Image URL Field Scope (`SCOPE LIMITATION`):**
  * `imageUrl` is a **plain URL text input** — admin types or pastes a URL string.
  * **No file upload.** No drag-and-drop. No image picker.
  * **No image manager.** No asset library or gallery.
  * **No image storage service.** Backend stores the URL string as-is; it does not host or proxy images.
  * Backend annotation: No validation annotation on `imageUrl` field (plain `String`, max 200 chars per entity column constraint).
* **Validation:**
  * Client-side: `name` required, min 2 chars, max 100 chars. `description` max 500 chars.
  * Server-side errors displayed inline per field.
* **Actions:**
  * `[ CANCEL ]`: Closes modal without mutation.
  * `[ CREATE CATEGORY ]`: Executes `POST /api/v1/categories`.
* **On Success (HTTP 201):** Closes modal, shows success notification, refreshes category list (`GET /api/v1/categories`).
* **On HTTP 409 Conflict:** Highlights `name` field with inline error: *"Category '{name}' already exists."*

---

### 6.2 Edit Category Metadata Modal Workflow
* **Trigger:** Click `[ EDIT ]` on a category table row.
* **Modal Architecture:**
  * Title: `EDIT CATEGORY` (Plus Jakarta Sans `18px` font-bold text `#1C1917`).
  * Subtitle: Category ID displayed as read-only reference.
* **Form Fields (`CreateCategoryRequest` — reused by backend):**
  1. **Category Name (`name`):** Text input, pre-populated with current name. Required.
  2. **Description (`description`):** Textarea, pre-populated with current description. Optional, max 500 chars.
  3. **Image URL (`imageUrl`):** Plain text URL input, pre-populated with current URL. Optional.
* **Image URL Field Scope (`SCOPE LIMITATION`):** Same constraints as Create — plain URL text input, no file upload, no image manager, no image storage service.
* **Update Semantics (`BACKEND-CONFIRMED`):**
  * `name` and `description` are always sent in the request payload.
  * `imageUrl` conditional update (`CategoryService.java:58`): `if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl())`.
    * Sending a **new URL string** → overwrites existing value.
    * Sending `null` (field omitted or explicitly null) → **backend preserves existing value**.
    * **No "Clear Image" UI action:** The exact backend behavior for clearing an existing `imageUrl` back to `null` is not verified (sending empty string `""` would set it to `""`, not `null`). Therefore, no "Clear Image" button or action is created. Admin can only replace the URL with a new one.
  * **No `active` toggle:** There is no UI control to change the `active` status. Deactivation is handled exclusively via the Delete action.
* **Actions:**
  * `[ CANCEL ]`: Closes modal.
  * `[ SAVE CHANGES ]`: Executes `PUT /api/v1/categories/{id}`.
* **On Success (HTTP 200):** Closes modal, refreshes category list.

---

### 6.3 Soft-Delete Confirmation Modal Workflow
* **Trigger:** Click `[ DELETE ]` on a category table row.
* **Modal Architecture:**
  * Width: `max-w-md`.
  * Title: `DEACTIVATE CATEGORY?`
* **Confirmation Body:**
  * *"Are you sure you want to deactivate **{category.name}**? The category will be soft-deleted and removed from the active catalog."*
  * Operational Note: *"This action cannot be undone. There is no restore capability."*
* **Wording Accuracy (`UI DESIGN DECISION`):**
  * Uses "deactivate" and "soft-deleted" — NOT "permanently delete" because backend sets `active = false`.
  * Does NOT promise cascading product deletion because products are NOT affected.
  * Does NOT promise product migration to another category.
* **Actions:**
  * `[ CANCEL ]`: Closes modal.
  * `[ DEACTIVATE ]`: Primary destructive button (bg `#DC2626`, text `#FFFFFF`, 4px radius). Executes `DELETE /api/v1/categories/{id}`.
* **On Success (HTTP 204):** Closes modal, category is removed from the list view, success alert displayed.
* **On HTTP 409 Conflict (`CategoryInUseException`):**
  * Modal displays error banner:
    *"Cannot deactivate this category because active products still reference it. Please reassign or deactivate those products first."*
  * The `[ DEACTIVATE ]` button is NOT auto-retried. Admin must close modal and resolve the product references via `/admin/products`.

---

## 7. Product Relationship Handling

* **No Product Count Column (`SCOPE LIMITATION`):** The category table does NOT display a "product count" column. Backend `CategoryDto` does not include product count. No product count API exists.
* **No Product Fetching for Count:** The UI does NOT call `GET /api/v1/products/category/{categoryId}` to compute a count for display purposes.
* **Delete Rejection is the Signal:** If an admin attempts to delete a category that has active products, the backend's HTTP 409 response with the `CategoryInUseException` message is the definitive signal. The UI surfaces this error clearly.
* **Cross-Navigation:** If admin needs to view or manage products in a category, they navigate to `/admin/products` and search by keyword or visually inspect the category column. No embedded product management inside `/admin/categories`.

---

## 8. Client-Side State, Validation & Error Handling

### 8.1 Loading States
* **Initial Category Fetch:** Renders 4 skeleton table rows with `animate-pulse` placeholders (`h-12 bg-[#F5F5F4]`) while `GET /api/v1/categories` is pending.
* **Action Pending:** Buttons display loading state (`Creating...`, `Saving...`, `Deactivating...`) and prevent double-submission.

### 8.2 Empty State
* If `GET /api/v1/categories` returns an empty list, display:
  * Icon or illustration area (minimal).
  * Text: *"No active categories found."*
  * Primary action: `[ + NEW CATEGORY ]` button.

### 8.3 Error Handling & HTTP Status Mapping

| HTTP Status | Backend Cause | Client UX Handling |
| :--- | :--- | :--- |
| **HTTP 400 Bad Request** | Validation failed (`MethodArgumentNotValidException`) | Highlights failing form fields with inline red helper text extracted from the `errors` map. |
| **HTTP 404 Not Found** | Category ID not found (`CategoryNotFoundException`) | Displays notification toast: *"Category not found."* Refreshes list. |
| **HTTP 409 Conflict (Create)** | Name already exists (`CategoryAlreadyExistsException`) | Highlights `name` input field with: *"Category with this name already exists."* |
| **HTTP 409 Conflict (Delete)** | Active products reference category (`CategoryInUseException`) | Modal displays error banner: *"Cannot deactivate — active products reference this category."* |
| **HTTP 401 Unauthorized** | Token expired or invalid | Clears auth state and navigates to `/login` (FE-15). |
| **HTTP 403 Forbidden** | User lacks `ADMIN` role | Displays inline Access Denied banner; does NOT log user out. |
| **Network Error / 500** | Server failure | Inline error card above table with `[ RETRY ]` button. |

---

## 9. Responsive Specifications

* **Desktop (≥ 1280px):** Full 4-column data table with complete category names, descriptions, creation dates, and inline action button group.
* **Tablet (768px – 1279px):** Truncate `DESCRIPTION` more aggressively. Maintain horizontal table alignment without scroll.
* **Mobile (< 768px):**
  * Table transforms into compact stacked category cards.
  * Each card displays category name, description excerpt, and a full-width action bar with `[ EDIT ]` and `[ DELETE ]` touch targets (≥ 44px).
  * `[ + NEW CATEGORY ]` button spans full width below header.

---

## 10. Design System Token Compliance

| Token Dimension | Value | Application Context |
| :--- | :--- | :--- |
| **Canvas Background** | `#FAFAF9` | Primary page underlay (Warm Alabaster). |
| **Surface Background** | `#FFFFFF` | Table surface, modal dialogues, input fields. |
| **Milled Neutral Pad**| `#F5F5F4` | Table header row background, skeleton loaders. |
| **Hairline Dividers** | `#E7E5E4` | 1px border on table rows, modal perimeters, inputs. |
| **Primary Solids** | `#1C1917` | Headers, category names, primary action buttons. |
| **Secondary Typography**| `#57534E` | Descriptions, subtitles. |
| **Metadata Typography** | `#78716C` | JetBrains Mono table headers, breadcrumbs, dates. |
| **Destructive Action** | `#DC2626` | Delete action button, deactivation modal confirm. |
| **Corner Radii** | `4px` / `6px` | `4px` on buttons, inputs, badges; `6px` on container panels. |
| **Elevation** | `shadow-none` | Absolutely flat surfaces; zero floating drop shadows. |

---

## 11. Final Audit & Verification Checklist

- [x] **All Category Endpoints Verified:** `GET /categories`, `GET /categories/{id}`, `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}`.
- [x] **DTOs Verified:** `CreateCategoryRequest` (3 fields: `name`, `description`, `imageUrl`) and `CategoryDto` (6 fields: `id`, `name`, `description`, `imageUrl`, `active`, `createdAt`) audited from source code.
- [x] **Validation Verified:** `@NotBlank` + `@Size(min=2, max=100)` on `name`, `@Size(max=500)` on `description`, no annotation on `imageUrl`.
- [x] **Active/Inactive Behavior Verified:** `GET /categories` returns active-only via `findAllByActiveTrue()`. Soft-deleted categories disappear from list.
- [x] **Delete Semantics Verified:** Soft-delete sets `active = false`. NOT a hard delete. Confirmed by `CategoryDeletionContractTest.java`.
- [x] **409 Conflict Verified:** `CategoryInUseException` thrown when active products reference the category. `CategoryAlreadyExistsException` thrown on duplicate name. Both return HTTP 409.
- [x] **Product Relationship Verified:** Delete is blocked by `existsByCategoryIdAndActiveTrue()`. No product count displayed. No product fetching for count. No embedded product management.
- [x] **ADMIN Permission Verified:** Gateway routes `/api/v1/categories/**` through `AuthenticationFilter`. Mutations require authenticated ADMIN role.
- [x] **No Fake Fields:** No `slug`, `parentId`, `sortOrder`, `productCount`, `seoTitle`, `seoDescription`, or `revenue` fields invented.
- [x] **No Product Count System:** No product count API created or consumed.
- [x] **No Restore Capability Invented:** No restore/reactivate UI or endpoint. Once deleted, category is gone from list.
- [x] **No CMS/SEO Scope Creep:** No SEO fields, no banner manager, no image uploader, no slug manager.
- [x] **No Redundant STATUS Column:** `GET /categories` returns active-only; STATUS badge removed as zero-information redundancy. No active/inactive toggle. No restore action.
- [x] **Image URL Plain Text Only:** `imageUrl` is a plain URL text input. No file upload, no image manager, no image storage service. No "Clear Image" action (backend clear semantics unverified).
- [x] **Exactly 1 Canonical Stitch Screen Created:** Screen ID `74a3b91aad1f442a88de9c46e58db0fe` generated in project `113306237264437644`.
- [x] **Documentation Created:** `frontend/docs/ADMIN-04-CATEGORIES-DESIGN.md`.
- [x] **Existing Docs Untouched:** `ADMIN-01`, `ADMIN-02`, `ADMIN-03`, and `FE-01` through `FE-15` preserved completely.
- [x] **Code & Infrastructure Integrity:** Zero frontend code written, zero packages added, zero backend modifications, zero Git commands executed.

---

**ADMIN-04 STATUS: READY FOR REVIEW**
