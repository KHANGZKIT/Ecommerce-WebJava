package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Normal lookups only surface active products; soft-deleted rows are hidden. */
    Optional<Product> findByIdAndActiveTrue(Long id);

    Optional<Product> findBySkuAndActiveTrue(String sku);

    boolean existsBySku(String sku);

    /** True while at least one active product references the category — blocks category deletion. */
    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    Page<Product> findAllByActiveTrue(Pageable pageable);

    Page<Product> findAllByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    /**
     * Batch fetch — replaces the N+1 loop in OrderService.
     * Only returns active products so inactive items are rejected early.
     */
    List<Product> findAllByIdInAndActiveTrue(List<Long> ids);

    /**
     * Full-Text Search using PostgreSQL tsvectors + GIN index.
     *
     * Replaces the old LIKE '%keyword%' query which performed a sequential scan.
     * The native SQL leverages PostgreSQL's plainto_tsquery so multi-word
     * phrases work correctly, and the '||' weights both name and description.
     *
     * For best performance, add a GIN index in a migration:
     *   CREATE INDEX idx_product_fts ON products
     *     USING gin(to_tsvector('english', name || ' ' || description));
     */
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.active = true
              AND to_tsvector('english', coalesce(p.name,'') || ' ' || coalesce(p.description,''))
                  @@ plainto_tsquery('english', :keyword)
            """,
            countQuery = """
            SELECT count(*) FROM products p
            WHERE p.active = true
              AND to_tsvector('english', coalesce(p.name,'') || ' ' || coalesce(p.description,''))
                  @@ plainto_tsquery('english', :keyword)
            """,
            nativeQuery = true)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Atomically holds stock for a product if active and sufficient stock exists.
     * Returns 1 if stock was held, 0 if product was not found, inactive, or stock was insufficient.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.id = :productId AND p.active = true AND p.stockQuantity >= :quantity")
    int holdStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * Atomically releases stock for a product up to the maximum supported quantity.
     * Restores stock regardless of active status (inventory compensation principle).
     * Returns 1 if stock was restored, 0 if product was not found or quantity exceeds max.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity " +
           "WHERE p.id = :productId AND (p.stockQuantity + :quantity) <= :maxStock")
    int releaseStock(@Param("productId") Long productId, @Param("quantity") int quantity, @Param("maxStock") int maxStock);
}
