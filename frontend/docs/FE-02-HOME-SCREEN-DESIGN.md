# TECHGEAR — Customer Home Screen Design Specification

> **Document ID:** FE-02-HOME-SCREEN-DESIGN  
> **Version:** 1.0.0  
> **Screen:** Customer Homepage (`/`)  
> **Visual Direction:** Premium Editorial Tech Commerce (Swiss Minimalism & Industrial Precision)  
> **Taste Dials:** `DESIGN_VARIANCE = 7` | `MOTION_INTENSITY = 4` | `VISUAL_DENSITY = 4`  
> **Status:** Final Design Blueprint — Ready for Implementation

---

## 0. Design Read

> **Design Read:** Curated hardware storefront for design-conscious engineers, audio enthusiasts, and power users, with a restrained Swiss minimalist & editorial language, leaning toward tactile industrial precision, razor-sharp typography, and honest technical hierarchy.

---

## 1. Screen Architecture & Global Layout Matrix

The homepage uses a desktop-first layout container constrained to **1280px (`max-w-7xl`)**, centered with `mx-auto` and padded with responsive horizontal insets (`px-6` on mobile, `px-8` on tablet, `px-12` on desktop).

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. HEADER (Fixed / Sticky 64px, Hairline Border #E7E5E4, Blur 8px)      │
│ [ TECHGEAR ]     Shop   Categories(06)        [Search] [Cart(0)] [Acc] │
├────────────────────────────────────────────────────────────────────────┤
│ 2. EDITORIAL HERO (Asymmetric 58/42 Grid, Initial Viewport Lock)       │
│  COL 1: Headline, 18-word Subtext, Primary + Secondary CTAs            │
│  COL 2: Studio Hardware Visual (Keychron + Sony + MX Master on stone)   │
├────────────────────────────────────────────────────────────────────────┤
│ 3. FEATURED CATEGORIES (Swiss Matrix 6-Cell Grid, Index 01-06)         │
│  [01 Audio]  [02 Keyboards & Mouse]  [03 Gaming]                       │
│  [04 Accessories]  [05 Monitors]  [06 Laptop Accessories]              │
├────────────────────────────────────────────────────────────────────────┤
│ 4. FEATURED PRODUCTS (Curated 4-Column Grid from 24-Product Catalog)   │
│  [Sony XM5]      [MX Master 3S]     [Dell U2724D]     [Anker 737]      │
├────────────────────────────────────────────────────────────────────────┤
│ 5. EDITORIAL HARDWARE SPOTLIGHT (Centerpiece Feature: Keychron Q1 Pro) │
│  LEFT: Full Anodized 6063 Aluminum Showcase Visual                     │
│  RIGHT: Technical Monograph, 3 Precision Specs, Direct Product CTA     │
├────────────────────────────────────────────────────────────────────────┤
│ 6. FOOTER (Clean 4-Column Architectural Grid, Pure Utility)            │
│  [Brand]         [Catalog]          [Account & Orders] [Studio Info]   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Section-by-Section Visual Design

### 2.1 Section 1: Navigation Header

*   **Height:** Exactly `64px` (avoids bloated agency navbars).
*   **Position:** `sticky top-0 z-50`.
*   **Surface:** `#FAFAF9` with `backdrop-filter: blur(8px)` and background opacity `0.92`.
*   **Border:** `border-b border-[#E7E5E4]`.
*   **Layout Composition:**
    *   **Left (Brand):**
        *   Wordmark: `TECHGEAR`
        *   Typography: Plus Jakarta Sans, `17px`, Weight 700 (Bold), Letter-spacing `-0.04em`.
        *   Color: `#1C1917`.
    *   **Center (Primary Navigation):**
        *   Links: `Shop` (`/products`), `Categories` (`#categories`), `Featured` (`#featured`).
        *   Typography: Plus Jakarta Sans, `14px`, Weight 500 (Medium), Letter-spacing `0`.
        *   Color: `#57534E`, Hover: `#1C1917` (transition: `150ms`).
        *   Spacing: `gap-8` between items. Single line at desktop.
    *   **Right (Utility & Commerce):**
        *   `Search` Trigger: Minimal button with search glyph (Phosphor 16px, stroke 1.5px) + text "Search" + keyboard shortcut badge `⌘K` (JetBrains Mono 10px, `#78716C`, bg `#F5F5F4`, border `#E7E5E4`, radius 3px).
        *   `Account` Trigger: Text link "Account" or user icon (links to `/profile` if logged in, `/login` if guest).
        *   `Cart` Button: Primary commercial anchor. Bordered container (`h-9 px-3 border border-[#E7E5E4] rounded-[4px] bg-[#FFFFFF] hover:border-[#A8A29E]`). Contains Cart icon + label "Cart" + numerical count badge in JetBrains Mono (`[0]` or `[2]`, bold tabular numbers).

---

### 2.2 Section 2: Editorial Hero

*   **Viewport Intent:** Fits cleanly into the initial desktop viewport without forcing immediate scroll. Max top padding `pt-16 pb-16 md:pt-20 md:pb-24`.
*   **Grid Structure:** Asymmetric 2-column layout (55% narrative left, 45% hardware stage right).
*   **Left Column (Typography & Action):**
    *   *Eyebrow:* `CURATED HARDWARE ARCHITECTURE` (JetBrains Mono, `11px`, Weight 500, All-caps, Letter-spacing `+0.08em`, Color `#78716C`).
    *   *Headline:* 
        ```
        Hardware
        worth keeping.
        ```
        *Typography:* Plus Jakarta Sans, `52px` desktop (`36px` mobile), Weight 700 (Bold), Line-height `1.08`, Letter-spacing `-0.035em`, Color `#1C1917`. Strictly 2 lines on desktop.
    *   *Subtext (Exact Copy):*  
        "Curated technology gear for work, play, and acoustic precision. Machined materials, zero filler, built for lifetime utility."  
        *(Total word count: 18 words — strictly passes the 20-word anti-AI-slop cap).*  
        *Typography:* Plus Jakarta Sans, `16px`, Line-height `1.6`, Color `#57534E`, `max-w-[44ch]`.
    *   *CTA Group:*
        *   **Primary CTA:** `Explore Shop` (Solid obsidian `#1C1917`, text `#FFFFFF`, `h-11 px-6 rounded-[4px] font-medium text-14px hover:bg-[#292524] active:scale-[0.98] focus:ring-2 focus:ring-[#D97706]`).
        *   **Secondary CTA:** `View Categories` (Ghost button, text `#1C1917`, `h-11 px-5 rounded-[4px] font-medium text-14px hover:bg-[#F5F5F4] inline-flex items-center gap-2`).
*   **Right Column (Hardware Composition Stage):**
    *   *Frame:* Aspect ratio 4:3 or 1:1, background `#F5F5F4`, border `1px solid #E7E5E4`, radius `6px`.
    *   *Visual Content:* Crisp architectural studio arrangement featuring three physical anchors: Keychron Q1 Pro anodized keyboard, Sony WH-1000XM5 headphones, and Logitech MX Master 3S on a dark slate acoustic desk mat.
    *   *Tactile Detail Badge (Bottom Left of Image):* Clean inset technical plate:  
        `COLLECTION 2026 / 24 OBJECTS VERIFIED` (JetBrains Mono, `10px`, Color `#78716C`).

---

### 2.3 Section 3: Featured Categories (Swiss Matrix)

Rather than 6 identical floating cards with clipart icons, categories are presented as a structured **Swiss architectural matrix**:

*   **Section Header:**
    *   Headline: `Core Collections` (Plus Jakarta Sans, `24px`, Weight 600, `#1C1917`).
    *   Count indicator: `(06)` in JetBrains Mono `14px` (`#78716C`).
*   **Grid:** 3 columns × 2 rows on desktop (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-px bg-[#E7E5E4] border border-[#E7E5E4] rounded-[6px] overflow-hidden`).  
    *(The 1px grid gap over `#E7E5E4` creates crisp, authentic Swiss dividers without double-border clashing).*
*   **Individual Category Cells (All 6 Real Categories):**

| Cell # | Category Title | Technical Code | Curated Scope | Target Route |
| :---: | :--- | :--- | :--- | :--- |
| **01** | **Audio** | `CAT-01 // ACOUSTIC` | Studio monitors, ANC headphones, podcast mics | `/categories/1` |
| **02** | **Keyboards & Mouse** | `CAT-02 // INPUT` | CNC aluminum custom boards, ergonomic mice | `/categories/2` |
| **03** | **Gaming** | `CAT-03 // ESPORTS` | Optical switches, rapid trigger, controllers | `/categories/3` |
| **04** | **Accessories** | `CAT-04 // POWER` | GaN chargers, monitor lightbars, TB4 docks | `/categories/4` |
| **05** | **Monitors** | `CAT-05 // DISPLAY` | IPS Black 2K/4K, calibrated color, high refresh | `/categories/5` |
| **06** | **Laptop Accessories**| `CAT-06 // WORKSTATION` | Portable stands, multi-port hubs, sleeves | `/categories/6` |

*   **Cell Anatomy:**
    *   Background: `#FFFFFF`, Hover: `#FAFAF9` (smooth transition: `150ms`).
    *   Padding: `p-6 md:p-8`.
    *   Top line: Technical Index `01` / `02` (JetBrains Mono `11px`, `#A8A29E`).
    *   Middle line: Category Title (Plus Jakarta Sans `18px`, Weight 600, `#1C1917`).
    *   Bottom line: Brief scope descriptor (`13px`, `#78716C`) + subtle arrow glyph `→` that translates `+3px` on hover.

---

### 2.4 Section 4: Featured Products Grid

Curated selection of **4 key products** directly from the 24-product catalog in `FE-01-CATALOG-SITEMAP-FLOW.md`:

*   **Section Header:**
    *   Headline: `Selected Equipment` (Plus Jakarta Sans, `24px`, Weight 600, `#1C1917`).
    *   Action Link: `View All Equipment (24) →` (JetBrains Mono `12px`, `#1C1917`, hover underline).
*   **Grid:** 4 columns on desktop, 2 on tablet, 1 on mobile (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6`).
*   **The 4 Chosen Catalog Items:**

```
┌───────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐
│ [TG-AUD-001]              │ │ [TG-KBM-002]              │ │ [TG-MON-001]              │ │ [TG-ACC-001]              │
│ Sony WH-1000XM5           │ │ Logitech MX Master 3S     │ │ Dell UltraSharp U2724D    │ │ Anker 737 GaNPrime 140W   │
│ Audio                     │ │ Keyboards & Mouse         │ │ Monitors                  │ │ Accessories               │
│ 7.990.000 ₫               │ │ 2.190.000 ₫               │ │ 11.490.000 ₫              │ │ 2.850.000 ₫               │
│ ● In Stock (35)           │ │ ● In Stock (65)           │ │ ● In Stock (20)           │ │ ● In Stock (45)           │
└───────────────────────────┘ └───────────────────────────┘ └───────────────────────────┘ └───────────────────────────┘
```

*   **Product Card Structure (Compliant with `FE-DESIGN-SYSTEM.md`):**
    1.  **Image Stage:** Aspect ratio `1:1`, background `#F5F5F4`, border `1px solid #E7E5E4`, radius `4px`, padding `16px`. Hardware image centered.
    2.  **Category Tag:** `AUDIO` / `KEYBOARDS & MOUSE` (JetBrains Mono `10px`, Weight 500, All-caps, `#78716C`, margin-top `12px`).
    3.  **Product Title:** Plus Jakarta Sans `15px`, Weight 600, `#1C1917`, line-clamp `2`, margin-top `4px`.
    4.  **Price & Availability Bar (Margin-top 12px):**
        *   Price: JetBrains Mono `16px`, Weight 600, `#1C1917`, `tabular-nums` (`7.990.000 ₫`).
        *   Stock: JetBrains Mono `11px`, subtle green dot `●` + `35 units` (`#15803D`).
    5.  **Interaction:** Entire card links directly to `/products/{id}`. Hover subtly shifts the border from `#E7E5E4` to `#A8A29E` with a microscopic 1% image scale.

---

### 2.5 Section 5: Editorial Hardware Spotlight (Centerpiece)

A dedicated single-hardware deep dive to break visual monotony and reinforce artisanal hardware credibility.

*   **Featured Hardware:** **Keychron Q1 Pro Custom Mechanical Keyboard** (`TG-KBM-001` / ID: 5).
*   **Layout:** 50/50 horizontal split container on `#FFFFFF` with `border border-[#E7E5E4] rounded-[6px] overflow-hidden`.
*   **Left Half (Industrial Photography Stage):**
    *   Large high-resolution side-profile view showcasing the machined 6063 aluminum chassis, double-gasket dampening layer, and custom PBT keycaps.
    *   Background: `#F5F5F4`.
    *   Technical Annotation: `CHASSIS: CNC MACHINED 6063 ALUMINUM // WEIGHT: 1.73 KG` (JetBrains Mono `10px`, `#78716C`).
*   **Right Half (Technical Monograph & Acquisition Panel):**
    *   *Eyebrow:* `HARDWARE SPOTLIGHT // INPUT ARCHITECTURE` (JetBrains Mono `11px`, Color `#D97706`).
    *   *Title:* `Keychron Q1 Pro` (Plus Jakarta Sans `32px`, Weight 700, `#1C1917`).
    *   *Editorial Narrative:*  
        "An uncompromising 75% mechanical platform carved from solid aerospace aluminum. Double-gasket acoustic isolation meets fully programmable QMK/VIA firmware for a typing response that feels weighted, acoustic, and immutable."
    *   *3 Precision Technical Specs (Clean Key-Value Matrix):*
        *   **Switch Type:** K Pro Red Mechanical (Linear, 45g actuation)
        *   **Connectivity:** Bluetooth 5.1 & Type-C (1000Hz wired polling)
        *   **Acoustic Mount:** Double-Gasket with high-density EVA sound dampener
    *   *Price & Action:*
        *   Price: `4.490.000 ₫` in JetBrains Mono `22px` (Bold).
        *   Primary Button: `Inspect Specifications →` (Links to `/products/5`, solid obsidian button, `h-11 px-6 rounded-[4px] font-medium text-14px`).

---

### 2.6 Section 6: Structured Footer

*   **Surface:** `#FAFAF9` with `border-t border-[#E7E5E4]`.
*   **Padding:** `py-16 md:py-20`.
*   **Grid Structure:** 4 clean structural columns (`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-10`):
    *   **Column 1 (Brand & Philosophy):**
        *   `TECHGEAR` wordmark in `16px` bold.
        *   Text: "Independent technology equipment store. Curated hardware, certified mechanical integrity, and studio acoustics."
        *   Currency Declaration: `CURRENCY: VND (VIETNAM DONG)` (JetBrains Mono `11px`, `#78716C`).
    *   **Column 2 (Catalog Index):**
        *   Header: `COLLECTIONS` (JetBrains Mono `11px`, `#1C1917`).
        *   Links: Audio, Keyboards & Mouse, Gaming, Accessories, Monitors, Laptop Accessories, Full Catalog.
    *   **Column 3 (Orders & Account):**
        *   Header: `CUSTOMER PORTAL` (JetBrains Mono `11px`, `#1C1917`).
        *   Links: Order History (`/orders`), Account Profile (`/profile`), Order Status Lookup (`/orders/:id`).
    *   **Column 4 (Store & Service Information):**
        *   Header: `DISPATCH & SERVICE` (JetBrains Mono `11px`, `#1C1917`).
        *   Information: Direct warehouse dispatch, 5 payment methods accepted (`CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `NET_BANKING`, `WALLET`).
*   **Bottom Utility Bar:**
    *   Border: `border-t border-[#E7E5E4] pt-8 mt-12 flex flex-col md:flex-row justify-between items-center gap-4`.
    *   Left: `© 2026 TECHGEAR Store. All rights reserved.` (`12px`, `#78716C`).
    *   Right: `System Architecture: Observable Microservices (Spring Boot 3.2)` in JetBrains Mono `11px` (`#78716C`).

---

## 3. Responsive Adaptations (Mobile & Tablet)

1.  **Header:**
    *   On screens `< 768px`, navigation links collapse into a clean top-level modal sheet.
    *   Wordmark, Search icon, and Cart icon with counter remain permanently accessible.
2.  **Hero:**
    *   Stacks vertically: Headline and CTAs on top, hardware visual directly underneath.
    *   Headline font size scales from `52px` down to `36px` to maintain strict 2-line rendering.
3.  **Categories:**
    *   Collapses from 3 columns into 2 columns on tablet (`640px-1024px`) and 1 column on mobile (`< 640px`), retaining all 6 categories with index numbers intact.
4.  **Products:**
    *   Collapses to 2 columns on tablet and 1 column on mobile with full-width touch targets.
5.  **Spotlight:**
    *   Stacks image on top, technical monograph underneath.

---

## 4. Anti-AI-Slop Compliance Audit

| Rule / Constraint | Compliance in this Design |
| :--- | :--- |
| **No Purple/Blue Gradients** | Palette is strictly `#1C1917` (Obsidian), `#FAFAF9` (Alabaster), `#E7E5E4` (Stone), and `#D97706` (Signal Amber). Zero gradients. |
| **No Floating-Card Syndrome** | Structure is bounded by Swiss matrix hairline dividers (`1px solid #E7E5E4`) rather than floating bubbly cards. |
| **No Emojis as Icons** | 100% vector SVG icons (Phosphor / Lucide 1.5px stroke). |
| **Hero Viewport Lock** | Headline is strictly 2 lines; subtext is 18 words; top padding is capped at `pt-20`. Fits initial desktop viewport. |
| **Eyebrow Restraint** | Only 2 eyebrows used across the entire page (Hero and Section 5 Spotlight), well below the max 1 per 3 sections limit. |
| **No Split-Header Ban Violation**| Headers stack vertically; no lazy "left title + right filler paragraph" split headers. |
| **Honest Data Only** | Stock numbers and prices are 100% matched to the real 24-product catalog in `FE-01-CATALOG-SITEMAP-FLOW.md`. Zero fake reviews or fake countdown timers. |
| **Strict YAGNI Scope Lock** | Zero wishlist, zero ratings, zero coupons, zero newsletter forms, zero chatbots. |
