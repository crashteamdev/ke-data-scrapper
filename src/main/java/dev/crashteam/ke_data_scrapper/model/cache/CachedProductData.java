package dev.crashteam.ke_data_scrapper.model.cache;

import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import lombok.Data;

import java.util.List;

@Data
public class CachedProductData {
    private List<KeProduct.CharacteristicsData> characteristics;
    private List<KeProduct.SkuData> skuList;
}
