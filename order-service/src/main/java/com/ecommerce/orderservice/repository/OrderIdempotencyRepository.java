package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.model.OrderIdempotency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderIdempotencyRepository extends JpaRepository<OrderIdempotency, Long> {

    Optional<OrderIdempotency> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OrderIdempotency r WHERE r.id = :id")
    Optional<OrderIdempotency> findByIdForUpdate(@Param("id") Long id);
}
