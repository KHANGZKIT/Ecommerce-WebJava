package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.model.ProductOperationMarker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductOperationMarkerRepository extends JpaRepository<ProductOperationMarker, Long> {

    Optional<ProductOperationMarker> findByCheckoutOperationKey(String checkoutOperationKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM ProductOperationMarker m WHERE m.checkoutOperationKey = :key")
    Optional<ProductOperationMarker> findByCheckoutOperationKeyForUpdate(@Param("key") String key);

    boolean existsByCheckoutOperationKey(String checkoutOperationKey);
}
