package com.smartshelf.service;

import com.smartshelf.model.InventoryItem;
import com.smartshelf.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmartShelfEngineTest {

    private StoreService storeService;
    private SmartShelfEngine engine;

    @BeforeEach
    void setUp() {
        storeService = new StoreService();
        // Set store closing time far in advance (e.g. 23:59) so store is open during tests
        storeService.setClosingTime(LocalTime.of(23, 59));
        storeService.setStoreClosed(false);
        storeService.setRainDiscountApproved(false);
        engine = new SmartShelfEngine(storeService);
    }

    private InventoryItem createTestItem(String name, double basePrice, int shelfLifeDays, int quantity) {
        Product product = new Product();
        product.setId(101L);
        product.setName(name);
        product.setCategory("Breads");
        product.setBasePrice(basePrice);
        product.setShelfLifeDays(shelfLifeDays);

        InventoryItem item = new InventoryItem();
        item.setId(201L);
        item.setProduct(product);
        item.setStoreId(1L);
        item.setCurrentQuantity(quantity);
        return item;
    }

    @Test
    @DisplayName("Low inventory with clear weather should maintain regular price (0% discount)")
    void testMaintainRegularPrice() {
        // Low quantity (1 item) with many hours left
        InventoryItem item = createTestItem("Artisanal Sourdough", 120.0, 3, 1);

        Map<String, Object> result = engine.evaluateItem(item, 1.0, "Clear", false, 1.0);

        assertNotNull(result);
        assertEquals(0, result.get("discountPercentage"));
        assertEquals(120.0, (Double) result.get("adjustedPrice"), 0.01);
        assertEquals("Maintain Regular Price", result.get("action"));
        assertTrue((Double) result.get("riskScore") <= 1.50);
    }

    @Test
    @DisplayName("Approved rain emergency discount should trigger flat 50% discount")
    void testApprovedRainDiscount() {
        InventoryItem item = createTestItem("French Baguette", 80.0, 1, 5);

        Map<String, Object> result = engine.evaluateItem(item, 1.5, "Rain", true, 1.0);

        assertEquals(50, result.get("discountPercentage"));
        assertEquals(40.0, (Double) result.get("adjustedPrice"), 0.01);
        assertEquals("Approved Rain Emergency Sale (50% Off)", result.get("action"));
        assertEquals("Rainy Day App Push & In-store Banner", result.get("channel"));
    }

    @Test
    @DisplayName("When store is closed, end-of-day zero waste clearance triggers 50% discount")
    void testStoreClosedClearance() {
        storeService.setStoreClosed(true);
        InventoryItem item = createTestItem("Chocolate Croissant", 90.0, 1, 8);

        Map<String, Object> result = engine.evaluateItem(item, 1.0, "Clear", false, 1.0);

        assertEquals(50, result.get("discountPercentage"));
        assertEquals(45.0, (Double) result.get("adjustedPrice"), 0.01);
        assertEquals("End-of-Day Zero Waste Clearance (50% Off)", result.get("action"));
    }

    @Test
    @DisplayName("1-day shelf life items have higher risk factor than longer shelf life items")
    void testShelfLifeWeighting() {
        // 1-day item
        InventoryItem perishableItem = createTestItem("Fresh Cream Donut", 60.0, 1, 10);
        // 5-day item
        InventoryItem durableItem = createTestItem("Oatmeal Cookie Box", 60.0, 5, 10);

        Map<String, Object> perishableResult = engine.evaluateItem(perishableItem, 1.0, "Clear", false, 1.0);
        Map<String, Object> durableResult = engine.evaluateItem(durableItem, 1.0, "Clear", false, 1.0);

        double perishableScore = (Double) perishableResult.get("riskScore");
        double durableScore = (Double) durableResult.get("riskScore");

        assertTrue(perishableScore > durableScore, 
                "1-day fresh item risk score (" + perishableScore + ") should be higher than durable item (" + durableScore + ")");
    }

    @Test
    @DisplayName("Division by zero guard handles past closing time safely")
    void testHoursRemainingSafetyGuard() {
        // Closing time already passed
        storeService.setClosingTime(LocalTime.of(0, 1));
        InventoryItem item = createTestItem("Butter Croissant", 50.0, 1, 20);

        assertDoesNotThrow(() -> {
            Map<String, Object> result = engine.evaluateItem(item, 1.0, "Clear", false, 1.0);
            assertNotNull(result);
            assertTrue((Double) result.get("hoursUntilClosing") > 0);
        });
    }
}
