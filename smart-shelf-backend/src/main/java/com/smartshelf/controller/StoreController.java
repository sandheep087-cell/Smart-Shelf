package com.smartshelf.controller;

import com.smartshelf.model.StoreSettings;
import com.smartshelf.repository.StoreSettingsRepository;
import com.smartshelf.service.InventoryEvaluationService;
import com.smartshelf.service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class StoreController {

    private final StoreService storeService;
    private final StoreSettingsRepository storeSettingsRepository;
    private final InventoryEvaluationService inventoryEvaluationService;

    public StoreController(StoreService storeService, StoreSettingsRepository storeSettingsRepository,
            InventoryEvaluationService inventoryEvaluationService) {
        this.storeService = storeService;
        this.storeSettingsRepository = storeSettingsRepository;
        this.inventoryEvaluationService = inventoryEvaluationService;
    }

    @GetMapping("/api/store/status")
    public ResponseEntity<StoreSettings> getStoreStatus() {
        StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(() -> {
            StoreSettings defaultSettings = new StoreSettings();
            defaultSettings.setId(1L);
            defaultSettings.setIsOpen(true);
            defaultSettings.setClosingTime(storeService.getClosingTime());
            return storeSettingsRepository.save(defaultSettings);
        });
        storeService.setStoreClosed(!settings.isIsOpen());
        if (settings.getClosingTime() != null) {
            storeService.setClosingTime(settings.getClosingTime());
        }
        storeService.setRainDiscountApproved(settings.isRainDiscountApproved());
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/api/store/status")
    public ResponseEntity<StoreSettings> updateStoreStatus(@RequestParam boolean isOpen) {
        StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(StoreSettings::new);
        settings.setId(1L);
        settings.setIsOpen(isOpen);
        storeService.setStoreClosed(!isOpen);
        StoreSettings savedSettings = storeSettingsRepository.save(settings);
        if (isOpen) {
            inventoryEvaluationService.evaluateInventory();
        }
        return ResponseEntity.ok(savedSettings);
    }

    @PostMapping("/api/store/closing-time")
    public ResponseEntity<Map<String, Object>> updateClosingTime(@RequestParam String time) {
        try {
            java.time.LocalTime newTime = java.time.LocalTime.parse(time);
            storeService.setClosingTime(newTime);
            StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(StoreSettings::new);
            settings.setId(1L);
            settings.setClosingTime(newTime);
            storeSettingsRepository.save(settings);
            inventoryEvaluationService.evaluateInventory();
            return ResponseEntity.ok(Map.of("status", "Success", "newClosingTime", newTime.toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "Failure", "message", e.getMessage()));
        }
    }

    @PostMapping("/api/store/rain-discount")
    public ResponseEntity<Map<String, Object>> updateRainDiscount(@RequestParam boolean approved) {
        storeService.setRainDiscountApproved(approved);
        StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(StoreSettings::new);
        settings.setId(1L);
        settings.setRainDiscountApproved(approved);
        storeSettingsRepository.save(settings);
        inventoryEvaluationService.evaluateInventory();
        return ResponseEntity.ok(Map.of("status", "Success", "rainDiscountApproved", approved));
    }

    @PostMapping("/store/extend-time")
    public ResponseEntity<Map<String, Object>> extendClosingTime(@RequestParam int minutesToAdd) {
        try {
            storeService.extendClosingTime(minutesToAdd);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("status", "Failure", "message", exception.getMessage()));
        }

        StoreSettings settings = storeSettingsRepository.findById(1L).orElseGet(() -> {
            StoreSettings defaultSettings = new StoreSettings();
            defaultSettings.setId(1L);
            defaultSettings.setIsOpen(true);
            return defaultSettings;
        });
        settings.setClosingTime(storeService.getClosingTime());
        storeSettingsRepository.save(settings);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Success");
        response.put("newClosingTime", storeService.getClosingTime().toString());
        response.put("isStoreClosed", storeService.isStoreClosed());
        return ResponseEntity.ok(response);
    }
}