package dev.crashteam.ke_data_scrapper.model;

public enum RedisKey {

    KE_PRODUCT("KE_PRODUCT");

    private final String key;

    RedisKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
