package com.kostyali.divide_and_pay.model;

import java.util.HashSet;
import java.util.Set;

public class Product implements Comparable<Product> {
    private String product;
    private double price;
    private int count;
    private Set<String> persons = new HashSet<>();

    public Product(String product, double price, int count) {
        this.product = product;
        this.price = price;
        this.count = count;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getProduct() {
        return product;
    }

    public double getPrice() {
        return price;
    }

    public int getCount() {
        return count;
    }

    public Set<String> getPersons() {
        return persons;
    }

    public void addPerson(String person) {
        persons.add(person.toLowerCase().trim());
    }

    public void clearPersons() {
        persons.clear();
    }

    @Override
    public int compareTo(Product product) {
        return this.product.compareToIgnoreCase(product.getProduct());
    }
}
