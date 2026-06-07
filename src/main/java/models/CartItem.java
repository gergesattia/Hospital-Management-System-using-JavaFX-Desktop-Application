package models;

import javafx.beans.property.*;

public class CartItem {
    private final Medicine medicine;
    private final IntegerProperty quantity;
    private final DoubleProperty price;

    public CartItem(Medicine medicine, int quantity) {
        this.medicine = medicine;
        this.quantity = new SimpleIntegerProperty(quantity);
        this.price = new SimpleDoubleProperty(medicine.getUnitPrice());
    }

    public Medicine getMedicine() { return medicine; }
    
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int q) { this.quantity.set(q); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getPrice() { return price.get(); }
    public DoubleProperty priceProperty() { return price; }
    
    public double getTotal() {
        return getQuantity() * getPrice();
    }
}
