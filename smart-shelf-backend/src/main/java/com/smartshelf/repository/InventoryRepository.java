package com.smartshelf.repository;

import com.smartshelf.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByStoreId(Long storeId);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.product")
    List<InventoryItem> findAllWithProducts();

    List<InventoryItem> findByProductId(Long productId);

    Optional<InventoryItem> findFirstByProductId(Long productId);

    @Query("SELECT i FROM InventoryItem i WHERE LOWER(i.product.name) = LOWER(:name)")
    Optional<InventoryItem> findFirstByProductNameIgnoreCase(String name);

    void deleteByProductId(Long productId);
}