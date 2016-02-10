package it.polimi.dima.giftlist.product.Rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import it.polimi.dima.giftlist.model.EtsyProduct;

/**
 * Created by Elena on 28/01/2016.
 */
public class EtsyResultsDeserializer implements JsonDeserializer {
    @Override
    public List<EtsyProduct> deserialize(JsonElement je, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        List<EtsyProduct> etsyProductsList = new ArrayList<>();
        JsonElement results = je.getAsJsonObject().get("results");
        JsonArray resultsArray = results.getAsJsonArray();

        for(int currentProduct = 0; currentProduct < resultsArray.size(); currentProduct++) {

            JsonElement jsonProduct = resultsArray.get(currentProduct);

            String title = jsonProduct.getAsJsonObject().get("title").getAsString();
            String description = jsonProduct.getAsJsonObject().get("description").getAsString();
            float price = jsonProduct.getAsJsonObject().get("price").getAsFloat();
            int listing_id = jsonProduct.getAsJsonObject().get("listing_id").getAsInt();

            JsonElement images = jsonProduct.getAsJsonObject().get("Images");
            JsonArray imagesArray = images.getAsJsonArray();
            JsonElement jsonImage = imagesArray.get(0);
            String url_170x135 = jsonImage.getAsJsonObject().get("url_570xN").getAsString();

            //TODO later, to get more images
            /*
            for(int currentImage = 0; currentImage < imagesArray.size(); currentImage++) {

                JsonElement jsonImage = imagesArray.get(currentImage);
                String url_170x135 = jsonImage.getAsJsonObject().get("url_170x135").getAsString();

            }*/



            EtsyProduct p = new EtsyProduct(title, description, listing_id, price, url_170x135);
            etsyProductsList.add(p);
        }


        return  etsyProductsList;
    }


}

