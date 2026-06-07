package models;

import java.time.LocalDate;

public class Medicine {
    private int id;
    private String name;
    private String description;
    private int stockQuantity;
    private int minStockAlert;
    private double unitPrice;
    private double costPrice;
    private String supplier;
    private LocalDate expiryDate;

    public Medicine() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public int getMinStockAlert() { return minStockAlert; }
    public void setMinStockAlert(int minStockAlert) { this.minStockAlert = minStockAlert; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
