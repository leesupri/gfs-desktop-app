package com.gfs.app.model;

public class MarketListRow {
    private final long itemId;
    private final String itemCode;
    private final String itemName;
    private final String category;
    private final String inventoryUom;
    private final String purchaseUom;
    private final String recipeUom;
    private final double purchasePrice;
    private final double averageCost;
    private final boolean active;
    private final boolean sales;
    private final boolean stocked;
    private final boolean purchased;

    public MarketListRow(long itemId,
                         String itemCode,
                         String itemName,
                         String category,
                         String inventoryUom,
                         String purchaseUom,
                         String recipeUom,
                         double purchasePrice,
                         double averageCost,
                         boolean active,
                         boolean sales,
                         boolean stocked,
                         boolean purchased) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.category = category;
        this.inventoryUom = inventoryUom;
        this.purchaseUom = purchaseUom;
        this.recipeUom = recipeUom;
        this.purchasePrice = purchasePrice;
        this.averageCost = averageCost;
        this.active = active;
        this.sales = sales;
        this.stocked = stocked;
        this.purchased = purchased;
    }

    public long getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategory() {
        return category;
    }

    public String getInventoryUom() {
        return inventoryUom;
    }

    public String getPurchaseUom() {
        return purchaseUom;
    }

    public String getRecipeUom() {
        return recipeUom;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public double getAverageCost() {
        return averageCost;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSales() {
        return sales;
    }

    public boolean isStocked() {
        return stocked;
    }

    public boolean isPurchased() {
        return purchased;
    }
}