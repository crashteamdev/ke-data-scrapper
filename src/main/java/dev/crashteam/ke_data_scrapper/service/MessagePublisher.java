package dev.crashteam.ke_data_scrapper.service;

@FunctionalInterface
public interface MessagePublisher {

    void publish(String topic, Object message);
}
