package dev.crashteam.ke_data_scrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.exception.CategoryRequestException;
import dev.crashteam.ke_data_scrapper.mapper.KeCategoryToMessageMapper;
import dev.crashteam.ke_data_scrapper.model.dto.KeCategoryMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

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

    @Value("${app.stream.category.key}")
    public String streamKey;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        KeCategory.Data data = retryTemplate.execute((RetryCallback<KeCategory.Data, CategoryRequestException>) retryContext -> {
            var categoryData = keService.getCategoryData(1L);
            if (categoryData.getPayload() == null || !CollectionUtils.isEmpty(categoryData.getErrors())) {
                throw new CategoryRequestException();
            }
            return categoryData.getPayload().getCategory();
        });
        KeCategoryMessage categoryMessage = KeCategoryToMessageMapper.categoryToMessage(data);
        RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                        objectMapper.writeValueAsBytes(categoryMessage)));
        log.info("Posted [stream={}] category record with id - [{}]",
                streamKey, recordId);

    }
}
