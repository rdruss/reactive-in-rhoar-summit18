package com.redhat.summit18;

import io.vertx.core.json.JsonArray;

public class Product {

    private final Integer id;
    private final String name;

    private double price;

    public Product(JsonArray objects) {
        String theName;
        this.id = objects.getInteger(0);
        theName = objects.getString(1);
        if (theName.endsWith("\n")) {
            theName = theName.substring(0, theName.length() -1);
        }
        this.name = theName;
    }

    public Product(String product, int id) {
        this.id = id;
        this.name = product;
    }

    public Product setPrice(double price) {
        this.price = price;
        return this;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

}
