package com.smartshelf.service;

import com.smartshelf.model.InventoryItem;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SmartShelfEngine {

    private final StoreService storeService;

    public SmartShelfEngine(StoreService storeService) {
        this.storeService = storeService;
    }

    public Map<String, Object> evaluateItem(InventoryItem item, double weatherPenalty,
                String weatherCondition, boolean rainDiscountApproved, double dayWeight) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        
        // Target today's configured closing time
        LocalTime storeClosing = storeService.getClosingTime() != null ? storeService.getClosingTime() : LocalTime.of(21, 0);
        LocalDateTime closingDateTime = LocalDateTime.of(now.toLocalDate(), storeClosing);
        
        // If it's already past closing time or store closed, treat as end-of-day clearance
        double hoursRemaining;
        if (now.isAfter(closingDateTime) || storeService.isStoreClosed()) {
            hoursRemaining = 0.5; // Immediate clearance urgency
        } else {
            hoursRemaining = Duration.between(now, closingDateTime).toMinutes() / 60.0;
            if (hoursRemaining <= 0.1) {
                hoursRemaining = 0.1; // Safety guard against division by zero
            }
        }

        // Factor in shelf-life urgency
        double shelfLifeFactor = 1.0;
        int shelfLifeDays = item.getShelfLifeDays();
        if (shelfLifeDays <= 1) {
            shelfLifeFactor = 2.0; // 1-day fresh daily bakes must clear today!
        } else if (shelfLifeDays <= 2) {
            shelfLifeFactor = 1.4; // 2-day items (cakes, muffins)
        } else {
            shelfLifeFactor = 0.8; // Longer shelf life (cookies, brownies)
        }

        // Required sales rate: units per hour remaining to clear current inventory
        double requiredVelocity = ((double) item.getCurrentQuantity() / hoursRemaining);
        
        // Calibrated Risk Score Formula:
        // Divisor 2.5 ensures perishable items with surplus stock in the afternoon 
        // trigger realistic dynamic discounts (15%, 25%, 50%) to prevent waste
        double rawScore = requiredVelocity * weatherPenalty * dayWeight * shelfLifeFactor;
        double riskScore = Math.min(5.0, Math.round((rawScore / 2.5) * 100.0) / 100.0);

        // Decision Logic Matrix
        String action;
        String channel;
        int discountPercentage = 0;

        if (rainDiscountApproved) {
            discountPercentage = 50;
            action = "Approved Rain Emergency Sale (50% Off)";
            channel = "Rainy Day App Push & In-store Banner";
        } else if (storeService.isStoreClosed()) {
            discountPercentage = 50;
            action = "End-of-Day Zero Waste Clearance (50% Off)";
            channel = "Surplus Mystery Bag / Clearance";
        } else {
            if (riskScore <= 1.50) {
                action = "Maintain Regular Price";
                channel = "Digital Menu (Standard)";
                discountPercentage = 0;
            } else if (riskScore <= 2.80) {
                action = "Soft Bundle Deal (15% Off)";
                channel = "POS Prompt & In-store Screen";
                discountPercentage = 15;
            } else if (riskScore <= 4.00) {
                action = "Zero-Waste Flash Sale (25% Off)";
                channel = "Local SMS / Web Push";
                discountPercentage = 25;
            } else {
                action = "Surplus Mystery Clearance (50% Off)";
                channel = "Aggressive Social / App Alert";
                discountPercentage = 50;
            }
        }

        // Safe extraction of name from the Product catalog relation
        String itemName = (item.getProduct() != null) ? item.getProduct().getName() : "Unknown Item";

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", item.getId());
        result.put("itemName", itemName);
        double basePrice = (item.getProduct() != null) ? item.getProduct().getBasePrice() : 0.0;
        double adjustedPrice = Math.round((basePrice * (1.0 - (discountPercentage / 100.0))) * 100.0) / 100.0;
        result.put("riskScore", riskScore);
        result.put("currentQuantity", item.getCurrentQuantity());
        result.put("basePrice", basePrice);
        result.put("adjustedPrice", adjustedPrice);
        result.put("discountPercentage", discountPercentage);
        result.put("hoursUntilClosing", Math.round(hoursRemaining * 10.0) / 10.0);
        result.put("action", action);
        result.put("channel", channel);

        return result;
    }
}