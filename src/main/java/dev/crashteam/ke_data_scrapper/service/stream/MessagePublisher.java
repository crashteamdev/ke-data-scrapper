package dev.crashteam.ke_data_scrapper.service.stream;

import dev.crashteam.ke_data_scrapper.model.stream.Message;

@FunctionalInterface
public interface MessagePublisher<T extends Message> {

    Object publish(T message);
}
