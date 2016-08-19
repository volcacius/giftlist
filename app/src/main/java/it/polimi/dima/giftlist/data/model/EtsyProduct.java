package it.polimi.dima.giftlist.data.model;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

/**
 * Created by Alessandro on 08/01/16.
 */

public class EtsyProduct extends Product {

    int categoryId;

    public EtsyProduct(String name, String description, long id, float price, CurrencyType currencyType, String imageUrl, String imageUri) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.price = price;
        this.currencyType = currencyType;
        this.imageUrl = imageUrl;
        this.imageUri = imageUri;
    }

    public EtsyProduct(String name, String description, long id, float price, CurrencyType currencyType, String imageUrl, String imageUri, long wishlistId) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.price = price;
        this.currencyType = currencyType;
        this.imageUrl = imageUrl;
        this.imageUri = imageUri;
        this.wishlistId = wishlistId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}
