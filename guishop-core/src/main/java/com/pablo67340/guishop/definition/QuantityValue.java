package com.pablo67340.guishop.definition;

public class QuantityValue {
    public int quantity = 1;

    public QuantityValue setDisabled(boolean disabled) {
        if (disabled) {
            this.quantity = 1;
        } else {
            this.quantity = -1;
        }
        return this;
    }

    public QuantityValue setQuantity(int quantity) {
        this.quantity = Math.max(quantity, -1);

        if (this.quantity == 0) {
            this.quantity = 1;
        }

        if (this.quantity > 64) {
            this.quantity = 64;
        }

        return this;
    }

    public int getQuantity() {
        if (this.quantity == 0) {
            this.quantity = 1;
        }

        if (this.quantity > 64) {
            this.quantity = 64;
        }

        return Math.max(this.quantity, -1);
    }

    public boolean isDisabled() {
        return this.quantity > 0;
    }
}
