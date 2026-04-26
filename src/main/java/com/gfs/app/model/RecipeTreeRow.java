package com.gfs.app.model;

public class RecipeTreeRow {
    private final String type; // "RECIPE" or "INGREDIENT"
    private final String recipeName;
    private final String recipeActive;
    private final String sales;
    private final String purchased;
    private final String stocked;
    private final String production;
    private final String uom;
    private final String itemName;
    private final String recQty;
    private final String recipeUom;
    private final String invQty;
    private final String invUom;
    private final String unitCost;
    private final String totalCost;
    private final String costPerProduction;

    // Constructor for recipe node
    public RecipeTreeRow(String type, String recipeName, String recipeActive, String sales, String purchased,
                         String stocked, String production, String uom, String totalCost, String costPerProduction) {
        this.type = type;
        this.recipeName = recipeName;
        this.recipeActive = recipeActive;
        this.sales = sales;
        this.purchased = purchased;
        this.stocked = stocked;
        this.production = production;
        this.uom = uom;
        this.totalCost = totalCost;
        this.costPerProduction = costPerProduction;
        this.itemName = null;
        this.recQty = null;
        this.recipeUom = null;
        this.invQty = null;
        this.invUom = null;
        this.unitCost = null;
    }

    // Constructor for ingredient node
    public RecipeTreeRow(String type, String itemName, String recQty, String recipeUom,
                         String invQty, String invUom, String unitCost, String totalCost) {
        this.type = type;
        this.itemName = itemName;
        this.recQty = recQty;
        this.recipeUom = recipeUom;
        this.invQty = invQty;
        this.invUom = invUom;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        // recipe fields
        this.recipeName = null;
        this.recipeActive = null;
        this.sales = null;
        this.purchased = null;
        this.stocked = null;
        this.production = null;
        this.uom = null;
        this.costPerProduction = null;
    }

    // Getters
    public String getType() { return type; }
    public String getRecipeName() { return recipeName == null ? "" : recipeName; }
    public String getRecipeActive() { return recipeActive == null ? "" : recipeActive; }
    public String getSales() { return sales == null ? "" : sales; }
    public String getPurchased() { return purchased == null ? "" : purchased; }
    public String getStocked() { return stocked == null ? "" : stocked; }
    public String getProduction() { return production == null ? "" : production; }
    public String getUom() { return uom == null ? "" : uom; }
    public String getItemName() { return itemName == null ? "" : itemName; }
    public String getRecQty() { return recQty == null ? "" : recQty; }
    public String getRecipeUom() { return recipeUom == null ? "" : recipeUom; }
    public String getInvQty() { return invQty == null ? "" : invQty; }
    public String getInvUom() { return invUom == null ? "" : invUom; }
    public String getUnitCost() { return unitCost == null ? "" : unitCost; }
    public String getTotalCost() { return totalCost == null ? "" : totalCost; }
    public String getCostPerProduction() { return costPerProduction == null ? "" : costPerProduction; }
}