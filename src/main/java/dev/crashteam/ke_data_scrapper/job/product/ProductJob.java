package dev.crashteam.ke_data_scrapper.job.product;

import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import dev.crashteam.ke.scrapper.data.v1.KeProductChange;
import dev.crashteam.ke.scrapper.data.v1.KeScrapperEvent;
import dev.crashteam.ke_data_scrapper.mapper.KeProductToMessageMapper;
import dev.crashteam.ke_data_scrapper.mapper.ProductCorruptedException;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.model.stream.AwsStreamMessage;
import dev.crashteam.ke_data_scrapper.service.JobUtilService;
import dev.crashteam.ke_data_scrapper.service.MetricService;
import dev.crashteam.ke_data_scrapper.service.ProductDataService;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import dev.crashteam.ke_data_scrapper.service.stream.AwsStreamMessagePublisher;
import dev.crashteam.ke_data_scrapper.service.stream.RedisStreamMessagePublisher;
import dev.crashteam.ke_data_scrapper.util.ScrapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class ProductJob implements InterruptableJob {

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisStreamMessagePublisher messagePublisher;

    @Autowired
    KeProductToMessageMapper messageMapper;

    @Autowired
    ProductDataService productDataService;

    @Autowired
    MetricService metricService;

    @Autowired
    AwsStreamMessagePublisher awsStreamMessagePublisher;

    @Autowired
    KeService keService;

    @Value("${app.aws-stream.ke-stream.name}")
    public String streamName;

    ExecutorService jobExecutor = Executors.newFixedThreadPool(2);

    @Value("${app.stream.product.key}")
    public String streamKey;

    @Value("${app.stream.product.maxlen}")
    public Long maxlen;

    @Value("${app.stream.product.waitPending}")
    public Long waitPending;

    private boolean jobRunning = true;

    private Long jobCategoryId;

    private static final String JOB_TYPE = "PRODUCT_JOB";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Instant start = Instant.now();
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = Long.valueOf(jobDetail.getJobDataMap().get(Constant.CATEGORY_ID_KEY).toString());
        jobCategoryId = categoryId;

        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        jobDetail.getJobDataMap().put("totalItemProcessed", new AtomicLong(0));
        log.info("Starting job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        AtomicLong totalItemProcessed = (AtomicLong) jobDetail.getJobDataMap().get("totalItemProcessed");
        long limit = 100;
        try {
            while (jobRunning) {
                try {
                    KeGQLResponse gqlResponse = jobUtilService.getResponse(jobExecutionContext, offset, categoryId, limit);
                    if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                        break;
                    }
                    jobUtilService.putCachedGraphData(gqlResponse, offset, categoryId, limit);
                    if (gqlResponse.getData().getMakeSearch().getTotal() <= totalItemProcessed.get()) {
                        log.info("Total GQL response items - [{}] less or equal than total processed items - [{}] of category - [{}], " +
                                "skipping further parsing... ", gqlResponse.getData().getMakeSearch().getTotal(), totalItemProcessed.get(), categoryId);
                        break;
                    }
                    var productItems = Optional.ofNullable(gqlResponse.getData()
                                    .getMakeSearch())
                            .map(KeGQLResponse.MakeSearch::getItems)
                            .filter(it -> !CollectionUtils.isEmpty(it))
                            .orElse(Collections.emptyList());
                    if (CollectionUtils.isEmpty(productItems)) {
                        log.warn("Skipping all product job gql requests for categoryId - {} with offset - {}, cause items are empty", categoryId, offset);
                        offset.addAndGet(limit);
                        jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                        break;
                    }
                    log.info("Iterate through products for itemsCount={};categoryId={}", productItems.size(), categoryId);

                    List<Callable<PutRecordsRequestEntry>> callables = new ArrayList<>();
                    List<PutRecordsRequestEntry> entries = new ArrayList<>();
                    for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                        if (jobRunning) {
                            Long productId = Optional.ofNullable(productItem.getCatalogCard())
                                    .map(KeGQLResponse.CatalogCard::getProductId).orElse(null);
                            if (productId == null) continue;
                            if (productDataService.save(productId)) {
                                callables.add(postProductRecordAsync(categoryId, productItem));
                                //entries.add(postProductRecord(productItem));
                            }
                        }
                    }
                    List<Future<PutRecordsRequestEntry>> futures = jobExecutor.invokeAll(callables);
                    futures.forEach(it -> {
                        try {
                            if (it.get() != null) {
                                entries.add(it.get());
                            }
                        } catch (Exception e) {
                            log.error("Error while trying to fill AWS entries:", e);
                        }
                    });

                    try {
                        for (List<PutRecordsRequestEntry> batch : ScrapperUtils.getBatches(entries, 50)) {
                            PutRecordsResult recordsResult = awsStreamMessagePublisher.publish(new AwsStreamMessage(streamName, batch));
                            log.info("PRODUCT JOB : Posted [{}] records to AWS stream - [{}] for categoryId - [{}]",
                                    recordsResult.getRecords().size(), streamName, categoryId);
                        }
                    } catch (Exception e) {
                        log.error("PRODUCT JOB : AWS ERROR, couldn't publish to stream - [{}] for category - [{}]", streamName, categoryId, e);
                    }

                    offset.addAndGet(limit);
                    totalItemProcessed.addAndGet(productItems.size());
                    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                    jobExecutionContext.getJobDetail().getJobDataMap().put("totalItemProcessed", totalItemProcessed);
                } catch (Exception e) {
                    log.error("Gql search for catalog with id [{}] finished with exception - [{}] on offset - {}",
                            categoryId, Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()),
                            offset.get(), e);
                    metricService.incrementErrorJob(JOB_TYPE);
                    break;
                }
            }
        } finally {
            //jobExecutor.shutdown();
        }
        Instant end = Instant.now();
        log.debug("Product job - Finished collecting for category id - {}, total items processed - {} in {} seconds",
                categoryId, totalItemProcessed.get(), Duration.between(start, end).toSeconds());
        metricService.incrementFinishJob(JOB_TYPE);
        log.info("Starting CHILDREN jobs for category id - {}", categoryId);
        Map<Long, Set<Long>> rootIdsMap = keService.getRootIdsMap();
        for (Long childId : rootIdsMap.get(categoryId)) {
            processCategory(childId);
        }
    }

    private void processCategory(Long categoryId) {
        Instant start = Instant.now();
        log.info("Starting CHILD job with category id - {}", categoryId);
        AtomicLong offset = new AtomicLong(0);
        AtomicLong totalItemProcessed = new AtomicLong(0);
        long limit = 100;
        try {
            while (jobRunning) {
                try {
                    KeGQLResponse gqlResponse = jobUtilService.getResponse(offset, categoryId, limit);
                    if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                        break;
                    }
                    jobUtilService.putCachedGraphData(gqlResponse, offset, categoryId, limit);
                    if (gqlResponse.getData().getMakeSearch().getTotal() <= totalItemProcessed.get()) {
                        log.info("Total GQL response items - [{}] less or equal than total processed items - [{}] of category - [{}], " +
                                "skipping further parsing... ", gqlResponse.getData().getMakeSearch().getTotal(), totalItemProcessed.get(), categoryId);
                        break;
                    }
                    var productItems = Optional.ofNullable(gqlResponse.getData()
                                    .getMakeSearch())
                            .map(KeGQLResponse.MakeSearch::getItems)
                            .filter(it -> !CollectionUtils.isEmpty(it))
                            .orElse(Collections.emptyList());
                    if (CollectionUtils.isEmpty(productItems)) {
                        log.warn("Skipping all product job gql requests for categoryId - {} with offset - {}, cause items are empty", categoryId, offset);
                        offset.addAndGet(limit);
                        break;
                    }
                    log.info("Iterate through products for itemsCount={};categoryId={}", productItems.size(), categoryId);

                    List<Callable<PutRecordsRequestEntry>> callables = new ArrayList<>();
                    List<PutRecordsRequestEntry> entries = new ArrayList<>();
                    for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                        if (jobRunning) {
                            Long productId = Optional.ofNullable(productItem.getCatalogCard())
                                    .map(KeGQLResponse.CatalogCard::getProductId).orElse(null);
                            if (productId == null) continue;
                            if (productDataService.save(productId)) {
                                callables.add(postProductRecordAsync(categoryId, productItem));
                                //entries.add(postProductRecord(productItem));
                            }
                        }
                    }
                    List<Future<PutRecordsRequestEntry>> futures = jobExecutor.invokeAll(callables);
                    futures.forEach(it -> {
                        try {
                            if (it.get() != null) {
                                entries.add(it.get());
                            }
                        } catch (Exception e) {
                            log.error("Error while trying to fill AWS entries:", e);
                        }
                    });

                    try {
                        for (List<PutRecordsRequestEntry> batch : ScrapperUtils.getBatches(entries, 50)) {
                            PutRecordsResult recordsResult = awsStreamMessagePublisher.publish(new AwsStreamMessage(streamName, batch));
                            log.info("CHILD PRODUCT JOB : Posted [{}] records to AWS stream - [{}] for categoryId - [{}]",
                                    recordsResult.getRecords().size(), streamName, categoryId);
                        }
                    } catch (Exception e) {
                        log.error("CHILD PRODUCT JOB : AWS ERROR, couldn't publish to stream - [{}] for category - [{}]", streamName, categoryId, e);
                    }

                    offset.addAndGet(limit);
                    totalItemProcessed.addAndGet(productItems.size());
                } catch (Exception e) {
                    log.error("Gql search for catalog with id [{}] finished with exception - [{}] on offset - {}",
                            categoryId, Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()),
                            offset.get(), e);
                    metricService.incrementErrorJob(JOB_TYPE);
                    break;
                }
            }
        } finally {
            //jobExecutor.shutdown();
        }
        Instant end = Instant.now();
        log.debug("Child Product job - Finished collecting for child category id - {}, total items processed - {} in {} seconds",
                categoryId, totalItemProcessed.get(), Duration.between(start, end).toSeconds());
        metricService.incrementFinishJob(JOB_TYPE);
    }

    private PutRecordsRequestEntry postProductRecord(Long categoryId, KeGQLResponse.CatalogCardWrapper productItem) {
        Long itemId = Optional.ofNullable(productItem.getCatalogCard())
                .map(KeGQLResponse.CatalogCard::getProductId)
                .orElse(null);
        if (itemId == null) {
            log.warn("Product id is null continue with next item, if it exists...");
            return null;
        }
        KeProduct.ProductData productData = jobUtilService.getProductData(itemId);
        jobUtilService.putCachedProductData(productData, itemId);

        if (productData == null) {
            log.warn("Product data with id - {} returned null, continue with next item, if it exists...", itemId);
            return null;
        }

        KeProductMessage productMessage = messageMapper.productToMessage(productData);
        if (productMessage.isCorrupted()) {
            log.warn("Product with id - {} is corrupted", productMessage.getProductId());
            return null;
        }

        return getAwsMessageEntry(categoryId, productData.getId().toString(), productData);
    }

    private Callable<PutRecordsRequestEntry> postProductRecordAsync(Long categoryId, KeGQLResponse.CatalogCardWrapper productItem) {
        return () -> postProductRecord(categoryId, productItem);
    }

    private PutRecordsRequestEntry getAwsMessageEntry(Long categoryId, String partitionKey, KeProduct.ProductData productData) {
        try {
            Instant now = Instant.now();
            KeProductChange keProductChange = messageMapper.mapToMessage(productData);
            KeScrapperEvent scrapperEvent = KeScrapperEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setScrapTime(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .setEventPayload(KeScrapperEvent.EventPayload.newBuilder()
                            .setKeProductChange(keProductChange)
                            .build())
                    .build();
            PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
            requestEntry.setPartitionKey(partitionKey);
            requestEntry.setData(ByteBuffer.wrap(scrapperEvent.toByteArray()));
            log.info("PRODUCT JOB - filling AWS entries for categoryId - [{}] productId - [{}]",
                    productData.getCategory().getId(), productData.getId());
            return requestEntry;
        } catch (ProductCorruptedException ex) {
            log.warn("Corrupted product item, ignoring it", ex);
        } catch (Exception ex) {
            log.error("Unexpected exception during publish AWS stream message", ex);
        }
        log.warn("AWS message for categoryId - [{}] productId - [{}] is null",
                categoryId, productData.getId());
        return null;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        log.info("Interrupting Product job with id - {}", jobCategoryId);
        this.jobRunning = false;
        jobExecutor.shutdown();
    }
}
