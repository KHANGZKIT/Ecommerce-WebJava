package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " CONCAT(o.id, '') LIKE %:search% OR " +
           " LOWER(o.shippingAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.paymentTransactionId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findAllWithFilter(@Param("status") OrderStatus status, @Param("search") String search, Pageable pageable);

    long countByStatus(OrderStatus status);
}
