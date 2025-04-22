package dev.crashteam.ke_data_scrapper.mapper.cache;

import dev.crashteam.ke_data_scrapper.model.cache.GraphQlCacheData;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;

import java.util.ArrayList;
import java.util.List;

public class KeGraphToCachedGraph {

    public static GraphQlCacheData keGQLResponseToCachedGraph(KeGQLResponse keGQLResponse) {
        if (keGQLResponse == null || keGQLResponse.getData() == null) return null;
        KeGQLResponse.MakeSearch keSearch = keGQLResponse.getData().getMakeSearch();

        GraphQlCacheData graphQlCacheData = new GraphQlCacheData();

        GraphQlCacheData.MakeSearch makeSearch = new GraphQlCacheData.MakeSearch();
        makeSearch.setTotal(keSearch.getTotal());

        List<GraphQlCacheData.CatalogCardWrapper> items = new ArrayList<>();
        for (KeGQLResponse.CatalogCardWrapper keItem : keSearch.getItems()) {
            if (keItem.getCatalogCard() == null) continue;
            GraphQlCacheData.CatalogCardWrapper item = getCatalogCardWrapper(keItem);
            items.add(item);
        }
        makeSearch.setItems(items);

        GraphQlCacheData.ResponseData responseData = new GraphQlCacheData.ResponseData();
        responseData.setMakeSearch(makeSearch);
        graphQlCacheData.setData(responseData);

        return graphQlCacheData;
    }

    private static GraphQlCacheData.CatalogCardWrapper getCatalogCardWrapper(KeGQLResponse.CatalogCardWrapper keItem) {
        GraphQlCacheData.CatalogCardWrapper item = new GraphQlCacheData.CatalogCardWrapper();

        GraphQlCacheData.CatalogCard catalogCard = new GraphQlCacheData.CatalogCard();
        List<GraphQlCacheData.CharacteristicValue> characteristicValues = new ArrayList<>();
        catalogCard.setProductId(keItem.getCatalogCard().getProductId());

        for (KeGQLResponse.CharacteristicValue keCharacteristicValue : keItem.getCatalogCard().getCharacteristicValues()) {
            GraphQlCacheData.CharacteristicValue characteristicValue = new GraphQlCacheData.CharacteristicValue();
            characteristicValue.setId(keCharacteristicValue.getId());
            characteristicValues.add(characteristicValue);
        }
        catalogCard.setCharacteristicValues(characteristicValues);

        item.setCatalogCard(catalogCard);
        return item;
    }

}
