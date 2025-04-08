package dev.crashteam.ke_data_scrapper.model.cache;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class GraphQlCacheData implements Serializable {

    private GraphQlCacheData.ResponseData data;

    @Data
    public static class ResponseData implements Serializable {
        private GraphQlCacheData.MakeSearch makeSearch;
    }

    @Data
    public static class MakeSearch implements Serializable {
        private List<GraphQlCacheData.CatalogCardWrapper> items;
        private Long total;
    }

    @Data
    public static class CatalogCardWrapper implements Serializable {
        private GraphQlCacheData.CatalogCard catalogCard;
    }

    @Data
    public static class CatalogCard implements Serializable {
        private Long productId;
        private List<GraphQlCacheData.CharacteristicValue> characteristicValues;
    }

    @Data
    public static class CharacteristicValue implements Serializable {
        private Long id;
    }

}
