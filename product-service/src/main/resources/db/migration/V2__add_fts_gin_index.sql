-- ===========================================================================
-- Migration: Add GIN index for full-text search on products table
-- ===========================================================================
-- Run this once against product_db after the service has started
-- (Hibernate ddl-auto=update creates the table, then this adds the index).
--
-- The index dramatically speeds up the plainto_tsquery search in
-- ProductRepository.searchByKeyword() — O(log N) vs O(N) sequential scan.
-- ===========================================================================

-- GIN index on pre-computed tsvector (fastest possible FTS)
CREATE INDEX IF NOT EXISTS idx_product_fts
    ON products
    USING gin (
        to_tsvector('english',
            coalesce(name, '') || ' ' || coalesce(description, ''))
    );

-- B-tree index for active-flag filter (used in every query)
CREATE INDEX IF NOT EXISTS idx_product_active
    ON products (active)
    WHERE active = true;

-- Composite index for category + active lookups (getProductsByCategory)
CREATE INDEX IF NOT EXISTS idx_product_category_active
    ON products (category_id, active)
    WHERE active = true;
