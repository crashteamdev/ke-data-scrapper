package dev.crashteam.ke_data_scrapper.job.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.mapper.KeProductToMessageMapper;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.JobUtilService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Component
@DisallowConcurrentExecution
public class ProductJob implements Job {

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${app.stream.product.key}")
    public String streamKey;

    @Value("${app.stream.product.maxlen}")
    public Long maxlen;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = Long.valueOf(jobDetail.getJobDataMap().get(Constant.CATEGORY_ID_KEY).toString());
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        log.info("Starting job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        long limit = 48;
        while (true) {
            try {
                KeGQLResponse gqlResponse = jobUtilService.getResponse(jobExecutionContext, offset, categoryId, limit);
                if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                    break;
                }
                var productItems = Optional.ofNullable(gqlResponse.getData()
                        .getMakeSearch())
                        .map(KeGQLResponse.MakeSearch::getItems)
                        .filter(it -> !CollectionUtils.isEmpty(it))
                        .orElse(Collections.emptyList());
                if (CollectionUtils.isEmpty(productItems)) {
                    log.warn("Skipping product job gql request for categoryId - {} with offset - {}, cause items is empty", categoryId, offset);
                    offset.addAndGet(limit);
                    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                    continue;
                }
                log.info("Iterate through products for itemsCount={};categoryId={}", productItems.size(), categoryId);

                for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                    Long itemId = Optional.ofNullable(productItem.getCatalogCard())
                            .map(KeGQLResponse.CatalogCard::getProductId)
                            .orElse(null);
                    if (itemId == null) {
                        log.warn("Product id is null continue with next item, if it exists...");
                        continue;
                    }
                    KeProduct.ProductData productData = jobUtilService.getProductData(itemId);

                    if (productData == null) {
                        log.warn("Product data with id - {} returned null, continue with next item, if it exists...", itemId);
                        continue;
                    }

                    KeProductMessage productMessage = KeProductToMessageMapper.productToMessage(productData);
                    if (productMessage.isCorrupted()) {
                        log.warn("Product with id - {} is corrupted", productMessage.getProductId());
                        continue;
                    }

                    RecordId recordId = streamCommands.xAdd(MapRecord.create(streamKey.getBytes(StandardCharsets.UTF_8),
                            Collections.singletonMap("item".getBytes(StandardCharsets.UTF_8),
                                    objectMapper.writeValueAsBytes(productMessage))), RedisStreamCommands.XAddOptions.maxlen(maxlen));
                    log.info("Posted product record with id - {}", recordId);
                }
                offset.addAndGet(limit);
                jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
            } catch (Exception e) {
                log.error("Gql search for catalog with id [{}] finished with exception - [{}]", categoryId, e.getMessage());
                break;
            }
        }
    }

}
