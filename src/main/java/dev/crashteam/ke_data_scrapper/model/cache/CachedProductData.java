package dev.crashteam.ke_data_scrapper.model.cache;

import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CachedProductData implements Serializable {
    private List<KeProduct.CharacteristicsData> characteristics;
    private List<KeProduct.SkuData> skuList;
}
