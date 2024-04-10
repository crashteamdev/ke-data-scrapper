package dev.crashteam.ke_data_scrapper.job.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.mapper.KeCategoryToMessageMapper;
import dev.crashteam.ke_data_scrapper.model.dto.KeCategoryMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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
        List<KeCategory.Data> rootCategories = keService.getRootCategories();
        KeGQLResponse gqlResponse = keService.getGQLSearchResponse("1", 0, 0);
        List<KeGQLResponse.ResponseCategoryWrapper> categoryTree = gqlResponse.getData().getMakeSearch().getCategoryTree();
        for (KeCategory.Data rootCategory : rootCategories) {
            KeCategoryMessage keCategoryMessage = KeCategoryToMessageMapper.categoryToMessage(rootCategory, categoryTree);
            RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                    Collections.singletonMap("category".getBytes(StandardCharsets.UTF_8),
                            objectMapper.writeValueAsBytes(keCategoryMessage)));
            log.debug("Posted [stream={}] category record with id - [{}]",
                    streamKey, recordId);
        }
    }
}
