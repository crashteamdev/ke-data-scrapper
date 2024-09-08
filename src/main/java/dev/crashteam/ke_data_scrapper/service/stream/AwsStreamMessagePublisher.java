package dev.crashteam.ke_data_scrapper.service.stream;

import com.amazonaws.services.kinesis.model.PutRecordsResult;
import dev.crashteam.ke_data_scrapper.aws.AwsStreamClient;
import dev.crashteam.ke_data_scrapper.model.stream.AwsStreamMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AwsStreamMessagePublisher implements MessagePublisher<AwsStreamMessage> {

    private final AwsStreamClient awsStreamClient;

    @SneakyThrows
    @Override
    public PutRecordsResult publish(AwsStreamMessage message) {
        return awsStreamClient.sendMessage(
                message.getTopic(),
                message.getMessage()
        );
    }
}
