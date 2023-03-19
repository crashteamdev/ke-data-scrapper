package dev.crashteam.ke_data_scrapper.job.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.mapper.KeProductToMessageMapper;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.model.stream.RedisStreamMessage;
import dev.crashteam.ke_data_scrapper.service.JobUtilService;
import dev.crashteam.ke_data_scrapper.service.RedisStreamMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ProductJob implements Job {

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisStreamMessagePublisher messagePublisher;

    @Value("${app.stream.product.key}")
    public String streamKey;

    @Value("${app.stream.product.maxlen}")
    public Long maxlen;

    @Value("${app.stream.product.waitPending}")
    public Long waitPending;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Instant start = Instant.now();
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = Long.valueOf(jobDetail.getJobDataMap().get(Constant.CATEGORY_ID_KEY).toString());
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        log.info("Starting job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        long limit = 100;
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
                    RecordId recordId = messagePublisher
                            .publish(new RedisStreamMessage(streamKey, productMessage, maxlen, "item", waitPending));
                    log.info("Posted product record [stream={}] with id - {}, for category id - [{}], product id - [{}]", streamKey,
                            recordId, categoryId, itemId);
                }
                offset.addAndGet(limit);
                jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
            } catch (Exception e) {
                log.error("Gql search for catalog with id [{}] finished with exception - [{}]", categoryId, e.getMessage());
                break;
            }
        }
        Instant end = Instant.now();
        log.info("Product job - Finished collecting for category id - {}, in {} seconds", categoryId,
                Duration.between(start, end).toSeconds());
    }

}
