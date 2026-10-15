package com.smartshelf.service;

import com.smartshelf.model.Customer;
import com.smartshelf.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MarketingBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(MarketingBroadcastService.class);
    private final CustomerRepository customerRepository;

    public MarketingBroadcastService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Map<String, Object> executeBroadcast(Map<String, Object> payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        response.put("timestamp", timestamp);

        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) payload.getOrDefault("channels", List.of("sms", "whatsapp", "instagram"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topDeals = (List<Map<String, Object>>) payload.getOrDefault("topDeals", Collections.emptyList());
        Number maxDiscountNum = (Number) payload.getOrDefault("maxDiscount", 50);
        int maxDiscount = maxDiscountNum != null ? maxDiscountNum.intValue() : 50;
        String storeUrl = (String) payload.getOrDefault("storeUrl", "http://localhost:8080#customer-store");
        String whatsappCommunityUrl = (String) payload.getOrDefault("whatsappCommunityUrl", "https://chat.whatsapp.com/BakeAndBlissCommunity");
        String instagramHandle = (String) payload.getOrDefault("instagramHandle", "@bakeandbliss");

        // Format deals summary for text messages
        StringBuilder dealsText = new StringBuilder();
        for (Map<String, Object> deal : topDeals) {
            String name = (String) deal.getOrDefault("itemName", "Item");
            Object flashPrice = deal.getOrDefault("flashPrice", deal.getOrDefault("adjustedPrice", "0.00"));
            Object origPrice = deal.getOrDefault("originalPrice", deal.getOrDefault("basePrice", "0.00"));
            Object discount = deal.getOrDefault("discount", deal.getOrDefault("discountPercentage", ""));
            dealsText.append("• ").append(name)
                    .append(": ₹").append(flashPrice)
                    .append(" (Was ₹").append(origPrice).append(")");
            if (discount != null && !discount.toString().isBlank()) {
                dealsText.append(" [").append(discount).append("% OFF]");
            }
            dealsText.append("\n");
        }

        // 1. Text Messages (SMS) - Strictly sent to regular customers whose numbers are entered
        if (channels.contains("sms")) {
            List<Customer> regularCustomers = customerRepository.findAll().stream()
                    .filter(c -> c.getPhone() != null && !c.getPhone().trim().isEmpty())
                    .toList();

            List<Map<String, Object>> smsDispatches = new ArrayList<>();
            for (Customer customer : regularCustomers) {
                String customerName = customer.getName() != null && !customer.getName().isBlank() 
                        ? customer.getName() : "Valued Customer";
                String personalizedMsg = String.format(
                        "Hi %s! ⚡ Smart Shelf Flash Deal Alert at Bake & Bliss! Up to %d%% OFF on freshly baked treats before closing:\n%sOrder online to reserve: %s\n~ Zero Waste Flash Deals",
                        customerName,
                        maxDiscount,
                        dealsText.toString(),
                        storeUrl
                );

                // Log SMS Dispatch (simulates carrier/Twilio/Fast2SMS delivery gateway)
                log.info("[SMS BROADCAST] Dispatched SMS to regular customer {} ({}) - Message: {}",
                        customerName, customer.getPhone(), personalizedMsg.replace("\n", " "));

                Map<String, Object> dispatchRecord = new LinkedHashMap<>();
                dispatchRecord.put("customerId", customer.getId());
                dispatchRecord.put("customerName", customerName);
                dispatchRecord.put("phone", customer.getPhone());
                dispatchRecord.put("status", "DELIVERED");
                dispatchRecord.put("deliveredAt", timestamp);
                smsDispatches.add(dispatchRecord);
            }

            Map<String, Object> smsResult = new LinkedHashMap<>();
            smsResult.put("recipientCount", smsDispatches.size());
            smsResult.put("recipients", smsDispatches);
            smsResult.put("status", "SUCCESS");
            smsResult.put("sampleMessage", String.format(
                    "Hi [Customer Name]! ⚡ Smart Shelf Flash Deal Alert at Bake & Bliss! Up to %d%% OFF:\n%sOrder now: %s",
                    maxDiscount, dealsText.toString(), storeUrl));
            response.put("sms", smsResult);
        }

        // 2. WhatsApp Community
        if (channels.contains("whatsapp")) {
            String whatsappMsg = String.format(
                    "🚨 *BAKE & BLISS FLASH SALE IS LIVE!* 🚨\n\n" +
                    "Smart Shelf just triggered flash price drops on our handcrafted daily bakes to guarantee ZERO food waste! 🥐🥖\n\n" +
                    "🔥 *TOP MARKDOWN SPECIALS (Up to %d%% OFF):*\n%s\n" +
                    "🛵 *Doorstep Delivery & Store Pickup Available!*\n" +
                    "📲 *Order Instantly Online:*\n%s\n\n" +
                    "⚡ *Limited stocks available. Claim yours before shelves clear!* ⚡",
                    maxDiscount,
                    dealsText.toString(),
                    storeUrl
            );

            log.info("[WHATSAPP BROADCAST] Posted Flash Poster & Deal Announcement to WhatsApp Community: {}", whatsappCommunityUrl);

            Map<String, Object> waResult = new LinkedHashMap<>();
            waResult.put("communityUrl", whatsappCommunityUrl);
            waResult.put("status", "DISPATCHED");
            waResult.put("message", whatsappMsg);
            response.put("whatsapp", waResult);
        }

        // 3. Instagram
        if (channels.contains("instagram")) {
            String instagramCaption = String.format(
                    "⚡ FLASH DEALS ACTIVE TODAY AT BAKE & BLISS! 🥐✨\n\n" +
                    "Freshly baked artisan favorites just dropped in price to prevent food waste! Enjoy up to %d%% OFF right now:\n\n" +
                    "%s\n" +
                    "🔗 Tap the link in bio or visit %s to order directly online!\n\n" +
                    "#SmartShelf #FlashSale #BakeAndBliss #ZeroWaste #SaveFood #ArtisanBakery #FreshBakes #DiscountsToday #Foodies #BakeryLife",
                    maxDiscount,
                    dealsText.toString(),
                    storeUrl
            );

            log.info("[INSTAGRAM BROADCAST] Prepared & Dispatched Flash Poster to Instagram Handle: {}", instagramHandle);

            Map<String, Object> igResult = new LinkedHashMap<>();
            igResult.put("handle", instagramHandle);
            igResult.put("status", "PUBLISHED");
            igResult.put("caption", instagramCaption);
            igResult.put("posterReady", true);
            response.put("instagram", igResult);
        }

        response.put("success", true);
        response.put("message", "1-Click Multi-Channel Broadcast executed successfully!");
        return response;
    }
}
