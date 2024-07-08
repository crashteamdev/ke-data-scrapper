package dev.crashteam.ke_data_scrapper.model.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message<T> {

    private String topic;
    private T message;
}
