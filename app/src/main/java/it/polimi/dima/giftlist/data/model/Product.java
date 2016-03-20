package it.polimi.dima.giftlist.data.model;

/**
 * Created by Alessandro on 08/01/16.
 */
public class Product {

    String name;
    float price;
    float convertedPrice;
    String currency;
    String description;
    String imageUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public float getPrice() {
        return price;
    }

    public float getConvertedPrice() {
        return convertedPrice;
    }

    public void setConvertedPrice(float price) {
        this.convertedPrice = price;
    }
}