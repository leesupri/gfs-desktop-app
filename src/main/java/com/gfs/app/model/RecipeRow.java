package com.gfs.app.model;

public class RecipeRow {
    private final long recipeId;
    private final boolean recipeActive;
    private final String sales;
    private final String purchased;
    private final String stocked;
    private final String recipeName;
    private final double production;
    private final String uom;
    private final long itemId;
    private final String itemName;
    private final double recQty;
    private final String recipeUom;
    private final double invQty;
    private final String invUom;
    private final double unitCost;
    private final double totalCost;
    private final int idx;

    public RecipeRow(long recipeId, boolean recipeActive, String sales, String purchased, String stocked,
                     String recipeName, double production, String uom,
                     long itemId, String itemName, double recQty, String recipeUom,
                     double invQty, String invUom, double unitCost, double totalCost, int idx) {
        this.recipeId = recipeId;
        this.recipeActive = recipeActive;
        this.sales = sales;
        this.purchased = purchased;
        this.stocked = stocked;
        this.recipeName = recipeName;
        this.production = production;
        this.uom = uom;
        this.itemId = itemId;
        this.itemName = itemName;
        this.recQty = recQty;
        this.recipeUom = recipeUom;
        this.invQty = invQty;
        this.invUom = invUom;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        this.idx = idx;
    }

    // Getters
    public long getRecipeId() { return recipeId; }
    public boolean isRecipeActive() { return recipeActive; }
    public String getSales() { return sales; }
    public String getPurchased() { return purchased; }
    public String getStocked() { return stocked; }
    public String getRecipeName() { return recipeName; }
    public double getProduction() { return production; }
    public String getUom() { return uom; }
    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public double getRecQty() { return recQty; }
    public String getRecipeUom() { return recipeUom; }
    public double getInvQty() { return invQty; }
    public String getInvUom() { return invUom; }
    public double getUnitCost() { return unitCost; }
    public double getTotalCost() { return totalCost; }
    public int getIdx() { return idx; }
}