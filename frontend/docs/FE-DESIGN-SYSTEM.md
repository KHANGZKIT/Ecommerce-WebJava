# TECHGEAR — Frontend Design System

> **Document ID:** FE-DESIGN-SYSTEM  
> **Version:** 1.0.0  
> **Target Platform:** Web (Desktop & Mobile Responsive)  
> **Visual Direction:** Premium Editorial Tech Commerce  
> **Design Skill Intelligence:** `ui-ux-pro-max` (Swiss Minimalism & Industrial Precision)  
> **Backend Status:** FROZEN — Strict YAGNI — Zero Speculative Scope

---

## 1. Brand / Visual Identity

**TECHGEAR** is an editorial tech boutique dedicated to high-performance peripherals, acoustic gear, and professional desk architecture. It serves discerning engineers, audio enthusiasts, designers, and power users who value tactile build quality and industrial precision over flashy consumer hype.

*   **Tone of Voice:** Confident, restrained, architectural, authoritative.
*   **Aesthetic Heritage:** Dieter Rams' functionalism, Swiss typographic grid systems, high-end audio hardware (machined aluminum, knurled knobs, matte PBT, acoustic fabric).
*   **Distinctiveness:** Where mass-market tech stores scream with flashing promotional banners and artificial urgency, TECHGEAR presents hardware with gallery-level calm, razor-sharp typography, and honest technical specifications.

---

## 2. Design Philosophy

1.  **Product-First Physicality:** Hardware is the hero. Product images are showcased on neutral, tactile surfaces with authentic studio lighting. No floating mockups, no cartoon 3D blobs.
2.  **Typographic Hierarchy over Visual Clutter:** Hierarchy is established through size contrast, weight contrast, and strict baseline alignment—never through arbitrary colors or noisy background cards.
3.  **Architectural Restraint:** Every border, line, and divider has structural purpose. Elevation is communicated through subtle hairline borders and tonal shifts rather than heavy drop shadows.
4.  **Information Density with Breathing Room:** Dense, legible tabular specs where technical precision matters (SKUs, stock counts, dimensions), framed by generous editorial whitespace.
5.  **Strict Honesty (Anti-Hype):** Zero fake countdown timers, zero simulated scarcity, zero pseudo-social-proof badges. What is displayed reflects real backend state.

---

## 3. Color System

The palette is rooted in monochromatic stone and obsidian, paired with a single calibrated industrial accent (**Signal Amber**) for interactive focus and key conversion points.

### 3.1 Core Semantic Tokens

| Token Name | Hex Code | Role & Usage |
| :--- | :--- | :--- |
| `--color-bg-canvas` | `#FAFAF9` | Primary page background (warm alabaster / unbleached paper tone; avoids harsh clinical `#FFFFFF` glare). |
| `--color-surface-base` | `#FFFFFF` | Primary surface for product cards, modular sections, tables, and dropdowns. |
| `--color-surface-raised`| `#F5F5F4` | Secondary surface for image containers, table headers, and subtle inset zones. |
| `--color-surface-sunken`| `#E7E5E4` | Deep surface for technical code blocks, skeleton loaders, and inactive toggle tracks. |
| `--color-text-primary` | `#1C1917` | High-contrast primary copy, headings, and key technical readouts (Contrast > 14:1 against canvas). |
| `--color-text-secondary`| `#57534E` | Supporting metadata, field hints, breadcrumbs, descriptions (Contrast > 5.5:1). |
| `--color-text-tertiary` | `#78716C` | Captions, timestamps, disabled labels (Contrast > 4.5:1 against canvas). |
| `--color-border-subtle` | `#E7E5E4` | Hairline dividers, card outlines, subtle structural grid rules. |
| `--color-border-strong` | `#A8A29E` | Interactive element borders, active input outlines, table header borders. |
| `--color-brand-primary` | `#1C1917` | Primary brand actions (high-authority buttons, dark badges, nav active states). |
| `--color-brand-accent`  | `#D97706` | **Signal Amber** — primary interactive accent, checkout highlight, focus ring indicator. |
| `--color-brand-accent-hover` | `#B45309` | Hover state for accent buttons and active text links. |

### 3.2 Status & Feedback Tokens

| Token Name | Hex Code | Role & Usage |
| :--- | :--- | :--- |
| `--color-success-fg` | `#15803D` | Text and icons for `CONFIRMED`, `DELIVERED`, and in-stock badges. |
| `--color-success-bg` | `#F0FDF4` | Subtle container background for positive statuses. |
| `--color-success-border` | `#BBF7D0` | Hairline border for positive status tags. |
| `--color-warning-fg` | `#B45309` | Text and icons for `PENDING`, low stock warning (< 20 units). |
| `--color-warning-bg` | `#FFFBEB` | Subtle container background for attention/pending states. |
| `--color-warning-border` | `#FDE68A` | Hairline border for warning tags. |
| `--color-error-fg`   | `#DC2626` | Text and icons for `PAYMENT_FAILED`, `CANCELLED`, and validation errors. |
| `--color-error-bg`   | `#FEF2F2` | Subtle container background for critical errors. |
| `--color-error-border` | `#FECACA` | Hairline border for error banners. |
| `--color-info-fg`    | `#0369A1` | Informational status `PROCESSING`, `SHIPPED`. |
| `--color-info-bg`    | `#F0F9FF` | Container background for transit/processing states. |
| `--color-info-border`| `#BAE6FD` | Hairline border for transit tags. |

---

## 4. Typography

Typography is the foundational pillar of the TECHGEAR identity. We pair a high-readability contemporary neo-grotesque with an industrial monospaced typeface for all quantitative/technical data.

*   **Primary Display & UI Font:** **Plus Jakarta Sans** (Google Fonts)  
    *Fallback:* `system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`
*   **Technical & Numeric Font:** **JetBrains Mono** (Google Fonts)  
    *Fallback:* `"SF Mono", Menlo, Consolas, Monaco, monospace`

```css
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap');
```

### 4.1 Typographic Scale & Hierarchy

| Hierarchy Role | Font Family | Size | Weight | Line Height | Tracking | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Display Hero** | Plus Jakarta Sans | 48px (`3rem`) | 700 (Bold) | 1.1 | `-0.03em` | Primary Home Hero headline (max 1 per page). |
| **Heading 1** | Plus Jakarta Sans | 32px (`2rem`) | 700 (Bold) | 1.2 | `-0.02em` | Page titles (Shop, Product Detail, Cart, Dashboard). |
| **Heading 2** | Plus Jakarta Sans | 24px (`1.5rem`) | 600 (SemiBold) | 1.25 | `-0.015em` | Section headers, category titles, checkout steps. |
| **Heading 3** | Plus Jakarta Sans | 18px (`1.125rem`)| 600 (SemiBold) | 1.3 | `-0.01em` | Product card titles, modal titles, order card headers. |
| **Body Large** | Plus Jakarta Sans | 16px (`1rem`) | 400 (Regular) | 1.5 | `0` | Product short descriptions, lead paragraphs. |
| **Body Default** | Plus Jakarta Sans | 14px (`0.875rem`)| 400 / 500 | 1.45 | `0` | Standard UI copy, form input values, table data. |
| **Body Small** | Plus Jakarta Sans | 12px (`0.75rem`) | 400 / 500 | 1.4 | `+0.01em` | Hints, form error messages, footer notes. |
| **Technical Label**| JetBrains Mono | 11px (`0.6875rem`)| 500 (Medium) | 1.2 | `+0.05em` | All-caps labels (CATEGORY, SKU, ID, STOCK: XX). |
| **Price Hero** | JetBrains Mono | 24px (`1.5rem`) | 600 (SemiBold) | 1.2 | `-0.02em` | Product Detail prominent price readout. |
| **Price Card** | JetBrains Mono | 16px (`1rem`) | 600 (SemiBold) | 1.2 | `-0.01em` | Product card price, Cart item price, subtotal. |

### 4.2 Formatting Rules
*   **Tabular Numbers:** All currency displays, counts, and SKUs must declare `font-variant-numeric: tabular-nums` to eliminate layout shift during increments.
*   **Currency Representation:** Vietnamese Dong formatted cleanly with period thousand separators followed by symbol: `7.990.000 ₫`.

---

## 5. Spacing System

Strict 8pt rhythmic scale. Layouts and components compose spacing exclusively from these tokens:

| Token Name | Value | Intent & Context |
| :--- | :--- | :--- |
| `--space-1` | 4px (`0.25rem`) | Micro gaps: badge padding, icon-to-text inline gap. |
| `--space-2` | 8px (`0.5rem`) | Compact padding: input inner vertical padding, tight item lists. |
| `--space-3` | 12px (`0.75rem`)| Standard internal padding: small button vertical, chip gaps. |
| `--space-4` | 16px (`1rem`) | Base component padding: card interiors, standard input horizontal padding. |
| `--space-6` | 24px (`1.5rem`) | Medium structural gap: between form fields, product card grid gap. |
| `--space-8` | 32px (`2rem`) | Section subsection gap: between card groups and headers. |
| `--space-12`| 48px (`3rem`) | Major module spacing: between distinct homepage blocks. |
| `--space-16`| 64px (`4rem`) | Page header to content spacing, major desktop section margin. |
| `--space-24`| 96px (`6rem`) | Maximum editorial breathing room for desktop hero / footer margins. |

---

## 6. Border Radius Strategy

TECHGEAR uses an **Architectural / Crisp Radius** strategy. Rounded bubbles, pill buttons, and heavy rounded card corners are strictly rejected.

| Token Name | Value | Target Components |
| :--- | :--- | :--- |
| `--radius-none` | `0px` | Structural grid lines, full-bleed media banners, table containers. |
| `--radius-sm` | `3px` | Badges, technical status tags, inline code chips. |
| `--radius-md` | `4px` | Form inputs, select menus, action buttons, alert boxes. |
| `--radius-lg` | `6px` | Product cards, modal dialogs, flyout cart drawer, dropdown menus. |
| `--radius-full` | `9999px`| **Forbidden for standard buttons & cards.** Permitted solely for circular quantity stepper buttons and avatar circles. |

---

## 7. Shadow Strategy

Shadows are restrained and architectural. They serve to indicate physical elevation in the z-axis (e.g. popovers, modals) rather than decorative styling for cards.

```css
/* Zero shadow for baseline cards — elevation achieved via hairline border */
--shadow-none: none;

/* Subtle lift on interactive hover (used sparingly) */
--shadow-subtle: 0 1px 3px 0 rgba(28, 25, 23, 0.04), 0 1px 2px -1px rgba(28, 25, 23, 0.04);

/* Dropdown menus, popovers, select options */
--shadow-dropdown: 0 4px 12px -2px rgba(28, 25, 23, 0.08), 0 2px 4px -2px rgba(28, 25, 23, 0.04);

/* Modal dialogs, Cart drawer */
--shadow-modal: 0 20px 25px -5px rgba(28, 25, 23, 0.12), 0 8px 10px -6px rgba(28, 25, 23, 0.06);
```

---

## 8. Grid / Layout Principles

*   **Max Content Width:** `1280px` (`max-w-7xl`), centered with auto horizontal margins.
*   **Desktop Column Grid:** 12-column grid with `24px` gutter (`--space-6`).
*   **Tablet Column Grid:** 8-column grid with `16px` gutter (`--space-4`).
*   **Mobile Column Grid:** 4-column grid with `16px` outer margin (`px-4`).
*   **Grid Line Aesthetic:** Where appropriate in the catalog and shop sections, structural grid lines (`1px solid var(--color-border-subtle)`) create a crisp Swiss matrix layout between items, eliminating redundant card boxes.

---

## 9. Product Card Design Principles

The product card is the workhorse of the e-commerce experience. It must be clean, balanced, and instantly informative.

1.  **Image Container:** Fixed 1:1 or 4:3 aspect ratio. Background is clean neutral `#F5F5F4`. Image has padding (`16px`) so hardware doesn't bleed into the edges.
2.  **Metadata Placement:**
    *   Top line: Category in uppercase technical mono font (`11px`, `--color-text-secondary`).
    *   Middle line: Product Title in Plus Jakarta Sans (`15px`, weight 600, max 2 lines with graceful truncation).
    *   Bottom line: Price (`16px`, JetBrains Mono, weight 600, `--color-text-primary`) alongside stock indicator.
3.  **Stock Indicator:** 
    *   In stock: Small `6px` solid emerald dot + "In Stock" in 11px mono.
    *   Low stock (< 20): Small amber dot + "X left".
    *   Out of stock: Muted gray dot + "Sold Out", image opacity reduced to `0.65`.
4.  **No Hover Clutter:** Hovering a card produces a subtle border shift (`#A8A29E`) and a microscopic 1.5% scale on the hardware image. No floating "Quick View" modals or jarring overlays that hide product info.

---

## 10. Product Detail Visual Hierarchy

A disciplined 2-column layout (55% visual showcase, 45% sticky purchase panel on desktop):

1.  **Left Column (Visual Showcase):**
    *   Generous high-resolution product showcase container on `#F5F5F4`.
    *   No noisy carousel indicators. Crisp border outline.
2.  **Right Column (Sticky Information & Buy Box):**
    *   *Breadcrumbs:* Home > Category Name > SKU.
    *   *Product Title:* Heading 1 (`32px`, Bold).
    *   *SKU & Stock Badge:* Inline technical metadata block in JetBrains Mono.
    *   *Price Readout:* Prominent Price Hero (`28px`, JetBrains Mono 600).
    *   *Short Description:* High-readability copy (`15px`, line-height 1.6).
    *   *Quantity Selector + Primary CTA:* Clean numerical stepper (`-`, `number`, `+`) adjacent to high-contrast "Add to Cart" button.
    *   *Technical Specifications Grid:* Bordered 2-column key-value table (Category, SKU, Availability, Warranty/Origin notes).

---

## 11. Navigation & Header Principles

*   **Density:** Compact, high-utility header (`64px` height). Fixed / sticky with a subtle hairline bottom border (`#E7E5E4`) and translucent backdrop blur (`rgba(250, 250, 249, 0.85)` + `backdrop-filter: blur(8px)`).
*   **Hierarchy:**
    *   *Left:* TECHGEAR Wordmark (Bold, all-caps, tracking `-0.04em`, 18px).
    *   *Center:* 6 Category Links (Audio, Keyboards & Mouse, Gaming, Accessories, Monitors, Laptop Accessories) with 14px medium typography. Active link indicated by crisp 2px bottom bar, not rounded colored pills.
    *   *Right:* Search Trigger, Account / Profile icon button, Cart button with numerical counter badge.
*   **Search Interaction:** Clicking Search opens a focused, distraction-free search input with keyboard shortcut hint (`Esc` to close).

---

## 12. Cart & Checkout Visual Principles

### 12.1 Cart Page (`/cart`)
*   **Format:** Split layout (65% item table / list, 35% sticky order summary).
*   **Line Items:** Horizontal rows with crisp product thumbnail (64x64px), title, SKU, unit price, inline quantity stepper, and a subtle text "Remove" action.
*   **Total Bar:** Clear calculation of Subtotal, Shipping (Calculated at checkout), and Total Amount in bold JetBrains Mono.

### 12.2 Checkout Page (`/checkout`)
*   **Step-Free Distraction-Free Form:** A single, clean structured flow:
    1.  *Contact & Shipping:* Shipping address textarea with strict character constraints.
    2.  *Payment Selection:* 5 distinct radio cards (`CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`, `WALLET`). Selected state is bordered in `#1C1917` with a clear radio dot.
    3.  *Delivery Notes:* Optional field.
    4.  *Idempotency Protection:* Primary CTA displays a transient spinner while disabled on click to prevent double-submit.

---

## 13. Order Status Visual Language

Strict 1-to-1 visual mapping for all 9 backend `OrderStatus` values defined in `order-service`:

| Backend `OrderStatus` | Visual Style | Border & Background | User Action Allowed |
| :--- | :--- | :--- | :--- |
| `PENDING` | Amber badge | `#FDE68A` border / `#FFFBEB` bg | Customer may **Cancel Order** |
| `CONFIRMED` | Emerald badge | `#BBF7D0` border / `#F0FDF4` bg | Customer may **Cancel Order** |
| `PROCESSING` | Sky Blue badge | `#BAE6FD` border / `#F0F9FF` bg | Read-only |
| `SHIPPED` | Indigo badge | `#C7D2FE` border / `#EEF2FF` bg | Read-only |
| `DELIVERED` | Deep Green badge | `#86EFAC` border / `#DCFCE7` bg | Read-only |
| `CANCELLED` | Muted Gray badge | `#E7E5E4` border / `#F5F5F4` bg | Read-only |
| `PAYMENT_FAILED` | Crimson badge | `#FECACA` border / `#FEF2F2` bg | Prompt to Retry / New Order |
| `PAYMENT_UNKNOWN` | Amber dashed | `#FDE68A` border / `#FFFBEB` bg | Contact Support / Verify |
| `REFUNDED` | Purple badge | `#DDD6FE` border / `#F5F3FF` bg | Read-only |

---

## 14. Button Hierarchy

Buttons express clear levels of authority and commitment:

| Hierarchy Level | Style & Construction | State Changes | Usage |
| :--- | :--- | :--- | :--- |
| **Primary** | Solid Obsidian (`#1C1917`), text `#FFFFFF`, radius `4px`, height `44px`. | Hover: `#292524`. Focus: 2px ring offset with `#D97706`. | "Add to Cart", "Place Order", "Sign In". Exactly 1 primary per view. |
| **Secondary** | White background, border `1px solid #A8A29E`, text `#1C1917`, radius `4px`. | Hover: background `#F5F5F4`. | "Continue Shopping", "Update Quantity", "Cancel". |
| **Tertiary / Ghost**| No background, no border, text `#57534E`, underline on hover. | Hover: text `#1C1917`. | "Remove Item", "Clear Filters", inline links. |
| **Destructive** | Solid Crimson (`#DC2626`) or Bordered Crimson with red text. | Hover: `#B91C1C`. | "Cancel Order" confirmation, "Delete User" (Admin). |

---

## 15. Form Design Principles

*   **Labels:** Always visible above inputs (`13px`, weight 500, `--color-text-primary`). No disappearing floating labels.
*   **Inputs:** `42px` height, radius `4px`, border `1px solid #D6D3D1`, background `#FFFFFF`.
*   **Focus State:** High-contrast `2px` focus ring in `--color-brand-accent` (`#D97706`) with `1px` offset.
*   **Validation Errors:**
    *   Input border shifts to `--color-error-fg` (`#DC2626`).
    *   Inline error message rendered immediately below input (`12px`, with an alert circle icon).
    *   Form submit failure displays a top-level focusable error summary linked to faulty fields (per WCAG & `ui-ux-pro-max` guidelines).
*   **Disabled Inputs:** Background `#F5F5F4`, border `#E7E5E4`, cursor `not-allowed`.

---

## 16. Responsive Principles

*   **Mobile (< 640px):**
    *   Header collapses navigation into a clean sliding side drawer.
    *   Product grids collapse to 1 or 2 columns with reduced image padding.
    *   Sticky Bottom Action Bar on Product Detail (`Add to Cart` + Price pinned at bottom of viewport for thumb reach).
    *   Minimum touch target size: `44 x 44px` on all interactive controls.
*   **Tablet (640px - 1024px):**
    *   Product grid renders 2 to 3 columns.
    *   Cart page collapses summary below items.
*   **Desktop (> 1024px):**
    *   Full 4-column product grid in Shop.
    *   Persistent 2-column layouts for Checkout and Product Detail.

---

## 17. Accessibility (A11y) Principles

1.  **Contrast Standards:** All text elements meet or exceed WCAG 2.1 AA requirements (minimum `4.5:1` for regular text, `3:1` for large headings and interactive borders).
2.  **Focus Visibility:** Unbroken, visible keyboard focus rings on every interactive component (`:focus-visible`).
3.  **Semantic Vector Icons:** All UI icons are SVGs (Phosphor or Lucide). Purely decorative icons carry `aria-hidden="true"`. Icon-only buttons (Cart, Menu, Search) possess explicit `aria-label` attributes.
4.  **No Color-Only State:** Status tags pair colors with clear textual labels and distinctive icons.
5.  **Reduced Motion:** Strictly honor `@media (prefers-reduced-motion: reduce)` by disabling transforms and easing transitions.

---

## 18. Interaction & Motion Principles

*   **Duration:** Fast and purposeful. Micro-interactions run between `150ms` and `220ms`.
*   **Easing:** Standard physical deceleration: `cubic-bezier(0.16, 1, 0.3, 1)`.
*   **Hover Rules:** Never use layout-shifting animations (e.g. growing borders that push neighbor elements).
*   **Page Transitions:** Instant or subtle opacity fade (`100ms`). Avoid slow slide carousels.

---

## 19. Empty / Loading / Error States

*   **Loading:** High-fidelity skeleton screens with subtle shimmer reflecting the exact shape of product cards, tables, and detail blocks. No generic full-page spinner.
*   **Empty State:** Clear, human copy explaining the situation with an immediate corrective action (e.g., "Your cart is empty" -> Button: "Explore Audio Gear").
*   **Error State:** Honest technical error display (e.g., "Payment authorization timed out. Your card was not charged.") with clear recovery guidance.

---

## 20. Explicit Anti-AI-Slop Rules

To ensure TECHGEAR does not look like a generic, prompt-generated template, the following patterns are **strictly prohibited**:

1.  **NO Purple/Blue Gradients:** No violet, indigo, or rainbow mesh gradient hero backgrounds.
2.  **NO Glassmorphism Everywhere:** No semi-transparent frosted cards stacked upon other blurred surfaces.
3.  **NO Floating Card Syndrome:** Pages must not consist of dozens of isolated pillowy cards floating in empty space. Use clean grid lines and section dividers.
4.  **NO Emojis as UI Icons:** Zero emojis (🎧, ⌨️, 🚀, 🔥) used as navigation icons, feature bullets, or status flags. Vector SVGs only.
5.  **NO Giant Generic "Hero + 3 Cards" Template:** The landing page opens directly with featured hardware, curated categories, and architectural typography.
6.  **NO Fake Marketing Urgency:** No "Only 2 left in stock! 14 people are viewing this!" popups. Stock numbers reflect actual database counts.
7.  **NO Invented Social Proof:** Zero fake 5-star rating stars, zero placeholder client testimonials, zero invented review quotes.
8.  **NO Abstract Blobs or Squiggles:** No decorative SVG blobs, confetti, or floating isometric illustrations that convey zero information.
9.  **NO Pill Buttons:** Buttons use functional 4px radius, not stadium/pill shapes.
10. **NO Unnecessary Animations:** No floating hover physics, no cursor-following glow dots, no parallax scroll that causes motion sickness.

---

> **Summary:** The TECHGEAR design system is deliberate, functional, and durable. It honors the hardware it sells by adhering to Swiss graphic design standards, honest technical presentation, and zero superfluous ornamentation.
