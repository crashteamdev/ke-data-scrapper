package dev.crashteam.ke_data_scrapper.job.category;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import dev.crashteam.ke.scrapper.data.v1.KeCategoryChange;
import dev.crashteam.ke.scrapper.data.v1.KeScrapperEvent;
import dev.crashteam.ke_data_scrapper.mapper.KeCategoryToMessageMapper;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.stream.AwsStreamMessage;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import dev.crashteam.ke_data_scrapper.service.stream.AwsStreamMessagePublisher;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@DisallowConcurrentExecution
public class CategoryJob implements Job {

    @Autowired
    KeService keService;

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    ObjectMapper objectMapper;
    
    @Autowired
    KeCategoryToMessageMapper categoryMapper;

    @Autowired
    AwsStreamMessagePublisher awsStreamMessagePublisher;

    @Value("${app.stream.category.key}")
    public String streamKey;


    @Value("${app.aws-stream.ke-stream.name}")
    public String awsStreamName;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        List<KeCategory.Data> rootCategories = keService.getRootCategories();
        KeGQLResponse gqlResponse = keService.getGQLSearchResponse("1", 0, 0);
        List<KeGQLResponse.ResponseCategoryWrapper> categoryTree = gqlResponse.getData().getMakeSearch().getCategoryTree();
        List<PutRecordsRequestEntry> entries = new ArrayList<>();
        for (KeCategory.Data rootCategory : rootCategories) {
            entries.add(getAwsMessage(rootCategory, categoryTree));
        }
        try {
            PutRecordsResult recordsResult = awsStreamMessagePublisher.publish(new AwsStreamMessage(awsStreamName, entries));
            log.info("CATEGORY JOB : Posted [{}] records to AWS stream - [{}]",
                    recordsResult.getRecords().size(), awsStreamName);
        } catch (Exception e) {
            log.error("CATEGORY JOB : AWS ERROR, couldn't publish to stream - [{}]", awsStreamName, e);
        }
    }

    private PutRecordsRequestEntry getAwsMessage(
            KeCategory.Data category,
            List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
        try {
            Instant now = Instant.now();
            dev.crashteam.ke.scrapper.data.v1.KeCategory keCategory = categoryMapper.mapToMessage(category, categoryTree);
            KeScrapperEvent scrapperEvent = KeScrapperEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setScrapTime(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .setEventPayload(KeScrapperEvent.EventPayload.newBuilder()
                            .setKeCategoryChange(KeCategoryChange.newBuilder()
                                    .setCategory(keCategory).build())
                            .build())
                    .build();
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setPartitionKey(String.valueOf(category.getId()));
            requestEntry.setData(ByteBuffer.wrap(scrapperEvent.toByteArray()));
            return requestEntry;
        } catch (Exception ex) {
            log.error("Unexpected exception during publish AWS stream message returning NULL", ex);
        }
        return null;
    }
}
