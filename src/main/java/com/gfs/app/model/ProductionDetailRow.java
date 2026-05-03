package com.gfs.app.model;

import java.time.LocalDateTime;

/**
 * One row from the Production Detail report.
 *
 * The JRXML has two nested groups:
 *   - Outer group on id       → shows notes in footer (once per production)
 *   - Inner group on ProductName → shows product header with qty/uom
 *   - Detail band             → one recipe ingredient line per row
 *
 * Header fields (id, date, warehouse, ProductName, Category,
 * ProductQty, ProductUom, productDescription, notes) repeat on
 * every detail row — grouping is done in the controller.
 */
public class ProductionDetailRow {

    // ── Production header (repeats per row) ──────────────────────
    private final long          id;
    private final LocalDateTime date;
    private final String        warehouse;
    private final String        productName;
    private final String        category;
    private final double        productQty;
    private final String        productUom;
    private final String        productDescription;
    private final String        notes;

    // ── Recipe ingredient line ────────────────────────────────────
    private final String recipeName;
    private final String recipeCategory;
    private final double recipeQty;
    private final String recipeUom;
    private final String recipeDescription;

    public ProductionDetailRow(long id, LocalDateTime date, String warehouse,
                               String productName, String category,
                               double productQty, String productUom, String productDescription,
                               String recipeName, String recipeCategory,
                               double recipeQty, String recipeUom, String recipeDescription,
                               String notes) {
        this.id                 = id;
        this.date               = date;
        this.warehouse          = warehouse;
        this.productName        = productName;
        this.category           = category;
        this.productQty         = productQty;
        this.productUom         = productUom;
        this.productDescription = productDescription;
        this.recipeName         = recipeName;
        this.recipeCategory     = recipeCategory;
        this.recipeQty          = recipeQty;
        this.recipeUom          = recipeUom;
        this.recipeDescription  = recipeDescription;
        this.notes              = notes;
    }

    public long          getId()                 { return id; }
    public LocalDateTime getDate()               { return date; }
    public String        getWarehouse()          { return s(warehouse); }
    public String        getProductName()        { return s(productName); }
    public String        getCategory()           { return s(category); }
    public double        getProductQty()         { return productQty; }
    public String        getProductUom()         { return s(productUom); }
    public String        getProductDescription() { return s(productDescription); }
    public String        getNotes()              { return s(notes); }
    public String        getRecipeName()         { return s(recipeName); }
    public String        getRecipeCategory()     { return s(recipeCategory); }
    public double        getRecipeQty()          { return recipeQty; }
    public String        getRecipeUom()          { return s(recipeUom); }
    public String        getRecipeDescription()  { return s(recipeDescription); }

    public String getDateFormatted() {
        return date != null
            ? date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            : "";
    }

    private static String s(String v) { return v != null ? v : ""; }
}