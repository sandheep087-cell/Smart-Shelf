package com.smartshelf.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "store_settings")
public class StoreSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean isOpen;

    private LocalTime closingTime = LocalTime.of(21, 0);

    private boolean rainDiscountApproved = false;

    private String whatsappCommunityUrl = "https://chat.whatsapp.com/BakeAndBlissCommunity";

    private String instagramHandle = "@bakeandbliss";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isIsOpen() { return isOpen; }
    public void setIsOpen(boolean open) { isOpen = open; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public boolean isRainDiscountApproved() { return rainDiscountApproved; }
    public void setRainDiscountApproved(boolean rainDiscountApproved) { this.rainDiscountApproved = rainDiscountApproved; }

    public String getWhatsappCommunityUrl() { return whatsappCommunityUrl; }
    public void setWhatsappCommunityUrl(String whatsappCommunityUrl) { this.whatsappCommunityUrl = whatsappCommunityUrl; }
    public String getInstagramHandle() { return instagramHandle; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
}