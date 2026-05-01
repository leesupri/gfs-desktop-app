package com.gfs.app.model;

public class ItemSalesTreeRow {
    private final String type;
    private final String department;
    private final String category;
    private final String itemName;
    private final String itemCode;
    private final String quantity;
    private final String subtotal;
    private final String cost;
    private final String discount;
    private final String serviceCharge;
    private final String tax;
    private final String profit;
    private final String total;
    private final String costPercentage;

    // Single constructor – all fields
    public ItemSalesTreeRow(String type, String department, String category, String itemName, String itemCode,
                            String quantity, String subtotal, String cost, String discount,
                            String serviceCharge, String tax, String profit, String total,String costPercentage) {
        this.type = type;
        this.department = department;
        this.category = category;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.cost = cost;
        this.discount = discount;
        this.serviceCharge = serviceCharge;
        this.tax = tax;
        this.profit = profit;
        this.total = total;
        this.costPercentage = costPercentage;
    }

    // Getters (return empty string for null values)
    public String getType() { return type == null ? "" : type; }
    public String getDepartment() { return department == null ? "" : department; }
    public String getCategory() { return category == null ? "" : category; }
    public String getItemName() { return itemName == null ? "" : itemName; }
    public String getItemCode() { return itemCode == null ? "" : itemCode; }
    public String getQuantity() { return quantity == null ? "" : quantity; }
    public String getSubtotal() { return subtotal == null ? "" : subtotal; }
    public String getCost() { return cost == null ? "" : cost; }
    public String getDiscount() { return discount == null ? "" : discount; }
    public String getServiceCharge() { return serviceCharge == null ? "" : serviceCharge; }
    public String getTax() { return tax == null ? "" : tax; }
    public String getProfit() { return profit == null ? "" : profit; }
    public String getTotal() { return total == null ? "" : total; }
    public String getCostPercentage() { return costPercentage == null ? "" : costPercentage; }
}