package dev.crashteam.ke_data_scrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.component.GzipRedisSerializer;
import dev.crashteam.ke_data_scrapper.model.cache.GraphQlCacheData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestGzipRedisSerializer {

    @Test
    public void testGzipRedis() {
        Assertions.assertDoesNotThrow(() -> {
            GzipRedisSerializer gzipRedisSerializer = new GzipRedisSerializer();
            GraphQlCacheData graphQlCacheData = new GraphQlCacheData();
            GraphQlCacheData.ResponseData responseData = new GraphQlCacheData.ResponseData();
            GraphQlCacheData.MakeSearch makeSearch = new GraphQlCacheData.MakeSearch();
            List<GraphQlCacheData.CatalogCardWrapper> items = new ArrayList<>();
            GraphQlCacheData.CatalogCardWrapper catalogCardWrapper = new GraphQlCacheData.CatalogCardWrapper();
            catalogCardWrapper.setCatalogCard(new GraphQlCacheData.CatalogCard());
            items.add(catalogCardWrapper);
            makeSearch.setTotal(10000L);
            makeSearch.setItems(items);
            responseData.setMakeSearch(makeSearch);
            graphQlCacheData.setData(responseData);

            byte[] serialize = gzipRedisSerializer.serialize(graphQlCacheData);
            gzipRedisSerializer.deserialize(serialize);
        });
    }
}
