package com.smartshelf.service;

import com.smartshelf.model.InventoryItem;
import com.smartshelf.repository.InventoryRepository;
import com.smartshelf.repository.StoreSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryEvaluationService.class);

    private final InventoryRepository inventoryRepository;
    private final SmartShelfEngine shelfEngine;
    private final WeatherService weatherService;
    private final StoreSettingsRepository storeSettingsRepository;
    private final StoreService storeService;
    private final WeatherPricingService weatherPricingService;

    public InventoryEvaluationService(InventoryRepository inventoryRepository,
            SmartShelfEngine shelfEngine, WeatherService weatherService,
            StoreSettingsRepository storeSettingsRepository, StoreService storeService,
            WeatherPricingService weatherPricingService) {
        this.inventoryRepository = inventoryRepository;
        this.shelfEngine = shelfEngine;
        this.weatherService = weatherService;
        this.storeSettingsRepository = storeSettingsRepository;
        this.storeService = storeService;
        this.weatherPricingService = weatherPricingService;
    }

    public void evaluateInventory() {
        storeSettingsRepository.findById(1L)
                .map(settings -> settings.getClosingTime())
                .ifPresent(storeService::setClosingTime);

        String weatherCondition = weatherService.fetchWeatherCondition();
        weatherPricingService.evaluateWeatherCondition(weatherCondition);
        double weatherPenalty = weatherService.getWeatherPenalty(weatherCondition);

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;

        for (InventoryItem item : inventoryRepository.findAllWithProducts()) {
            if (item.getExpiresAt() != null && now.isAfter(item.getExpiresAt())) {
                if (item.getCurrentQuantity() > 0 || !"EXPIRED".equals(item.getStatus())) {
                    item.setCurrentQuantity(0);
                    item.setStatus("EXPIRED");
                    inventoryRepository.save(item);
                    expiredCount++;
                }
            }
            shelfEngine.evaluateItem(item, weatherPenalty, weatherCondition, storeService.isRainDiscountApproved(), 1.0);
        }

        if (expiredCount > 0) {
            log.info("Continuous evaluation completed: Marked {} items as EXPIRED.", expiredCount);
        }
    }
}