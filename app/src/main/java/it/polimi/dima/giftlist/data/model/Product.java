package it.polimi.dima.giftlist.data.model;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;

/**
 * Created by Alessandro on 08/01/16.
 */
public class Product {

    String name;
    float price;
    float convertedPrice;
    long id;
    CurrencyType currencyType;
    String description;
    String imageUrl;
    long wishlistId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public String getDescription() {
        return description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public long getId() {
        return id;
    }

    public long getWishlistId() {
        return wishlistId;
    }

    public void setWishlistId(long wishlistId) {
        this.wishlistId = wishlistId;
    }
}