package com.gfs.app.model;

public class ItemSalesRow {
    private final String department;
    private final String category;
    private final String itemName;
    private final String itemCode;
    private final double quantity;
    private final double subtotal;
    private final double cost;
    private final double discount;
    private final double serviceCharge;
    private final double tax;
    private final double profit;
    private final double total;
    private final double costPercentage;

    public ItemSalesRow(String department, String category, String itemName, String itemCode,
                        double quantity, double subtotal, double cost, double discount,
                        double serviceCharge, double tax, double profit, double total , double costPercentage) {
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

    // Getters
    public String getDepartment() { return department; }
    public String getCategory() { return category; }
    public String getItemName() { return itemName; }
    public String getItemCode() { return itemCode; }
    public double getQuantity() { return quantity; }
    public double getSubtotal() { return subtotal; }
    public double getCost() { return cost; }
    public double getDiscount() { return discount; }
    public double getServiceCharge() { return serviceCharge; }
    public double getTax() { return tax; }
    public double getProfit() { return profit; }
    public double getTotal() { return total; }
    public double getCostPercentage() { return costPercentage; }
}