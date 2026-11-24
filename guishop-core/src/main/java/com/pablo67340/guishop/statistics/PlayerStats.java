package com.pablo67340.guishop.statistics;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data class representing a player's shop statistics.
 */
public class PlayerStats {
    
    private final UUID uuid;
    private BigDecimal totalSpent;
    private BigDecimal totalEarned;
    private int itemsBought;
    private int itemsSold;
    
    // Top items: material -> quantity
    private Map<String, Integer> topBoughtItems;
    private Map<String, Integer> topSoldItems;
    
    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
        this.totalSpent = BigDecimal.ZERO;
        this.totalEarned = BigDecimal.ZERO;
        this.itemsBought = 0;
        this.itemsSold = 0;
        this.topBoughtItems = new LinkedHashMap<>();
        this.topSoldItems = new LinkedHashMap<>();
    }
    
    public PlayerStats(UUID uuid, BigDecimal totalSpent, BigDecimal totalEarned, 
                       int itemsBought, int itemsSold) {
        this.uuid = uuid;
        this.totalSpent = totalSpent;
        this.totalEarned = totalEarned;
        this.itemsBought = itemsBought;
        this.itemsSold = itemsSold;
        this.topBoughtItems = new LinkedHashMap<>();
        this.topSoldItems = new LinkedHashMap<>();
    }
    
    // Getters
    public UUID getUuid() { return uuid; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public BigDecimal getTotalEarned() { return totalEarned; }
    public int getItemsBought() { return itemsBought; }
    public int getItemsSold() { return itemsSold; }
    public Map<String, Integer> getTopBoughtItems() { return topBoughtItems; }
    public Map<String, Integer> getTopSoldItems() { return topSoldItems; }
    
    // Setters for loading from DB
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
    public void setTotalEarned(BigDecimal totalEarned) { this.totalEarned = totalEarned; }
    public void setItemsBought(int itemsBought) { this.itemsBought = itemsBought; }
    public void setItemsSold(int itemsSold) { this.itemsSold = itemsSold; }
    public void setTopBoughtItems(Map<String, Integer> topBoughtItems) { this.topBoughtItems = topBoughtItems; }
    public void setTopSoldItems(Map<String, Integer> topSoldItems) { this.topSoldItems = topSoldItems; }
    
    // Modifiers
    public void addSpent(BigDecimal amount) {
        this.totalSpent = this.totalSpent.add(amount);
    }
    
    public void addEarned(BigDecimal amount) {
        this.totalEarned = this.totalEarned.add(amount);
    }
    
    public void addItemsBought(int quantity) {
        this.itemsBought += quantity;
    }
    
    public void addItemsSold(int quantity) {
        this.itemsSold += quantity;
    }
    
    /**
     * Get the top item at specified rank (1-based).
     * @param rank 1, 2, or 3
     * @param bought true for bought items, false for sold items
     * @return "MATERIAL:quantity" or null if not available
     */
    public String getTopItem(int rank, boolean bought) {
        Map<String, Integer> items = bought ? topBoughtItems : topSoldItems;
        if (items.isEmpty() || rank < 1 || rank > items.size()) {
            return null;
        }
        
        int i = 1;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (i == rank) {
                return entry.getKey() + ":" + entry.getValue();
            }
            i++;
            if (i > 3) break;
        }
        return null;
    }
    
    /**
     * Get just the material name for top item at specified rank.
     */
    public String getTopItemMaterial(int rank, boolean bought) {
        Map<String, Integer> items = bought ? topBoughtItems : topSoldItems;
        if (items.isEmpty() || rank < 1 || rank > items.size()) {
            return null;
        }
        
        int i = 1;
        for (String material : items.keySet()) {
            if (i == rank) {
                return material;
            }
            i++;
            if (i > 3) break;
        }
        return null;
    }
    
    /**
     * Get just the quantity for top item at specified rank.
     */
    public Integer getTopItemQuantity(int rank, boolean bought) {
        Map<String, Integer> items = bought ? topBoughtItems : topSoldItems;
        if (items.isEmpty() || rank < 1 || rank > items.size()) {
            return null;
        }
        
        int i = 1;
        for (Integer quantity : items.values()) {
            if (i == rank) {
                return quantity;
            }
            i++;
            if (i > 3) break;
        }
        return null;
    }
}
