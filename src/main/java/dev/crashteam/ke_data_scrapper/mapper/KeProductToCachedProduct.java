package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.cache.CachedProductData;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;

public class KeProductToCachedProduct {

    public static CachedProductData toCachedData(KeProduct.ProductData data) {
        CachedProductData cachedProductData = new CachedProductData();
        cachedProductData.setCharacteristics(data.getCharacteristics());
        cachedProductData.setSkuList(data.getSkuList());
        return cachedProductData;
    }
}
