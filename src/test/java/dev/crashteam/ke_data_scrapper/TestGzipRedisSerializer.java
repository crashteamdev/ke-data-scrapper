package dev.crashteam.ke_data_scrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.component.GzipRedisSerializer;
import dev.crashteam.ke_data_scrapper.model.cache.GraphQlCacheData;
import org.junit.jupiter.api.Test;

public class TestGzipRedisSerializer {

    @Test
    public void testGzipRedis() {
        ObjectMapper objectMapper = new ObjectMapper();
        GzipRedisSerializer gzipRedisSerializer = new GzipRedisSerializer(objectMapper);
        GraphQlCacheData graphQlCacheData = new GraphQlCacheData();
        GraphQlCacheData.ResponseData responseData = new GraphQlCacheData.ResponseData();
        GraphQlCacheData.MakeSearch makeSearch = new GraphQlCacheData.MakeSearch();
        makeSearch.setTotal(10000L);
        responseData.setMakeSearch(makeSearch);
        graphQlCacheData.setData(responseData);
        byte[] serialize = gzipRedisSerializer.serialize(graphQlCacheData);
        gzipRedisSerializer.deserialize(serialize);
    }
}
