package dev.crashteam.ke_data_scrapper.model;

public enum ProxySource {
    PROXY_LINE("PROXY_LINE"),
    MOBILE_PROXY("MOBILE_PROXY"),
    EXTERNAL_SOURCE("External source");

    ProxySource(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
