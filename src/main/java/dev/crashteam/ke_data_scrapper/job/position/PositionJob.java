package dev.crashteam.ke_data_scrapper.job.position;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.ke_data_scrapper.exception.KeGqlRequestException;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.ProductPositionMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.JobUtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class PositionJob implements Job {

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    RedisStreamCommands streamCommands;

    @Autowired
    ObjectMapper objectMapper;

    private final ThreadPoolTaskExecutor jobExecutor;

    @Value("${app.stream.position.key}")
    public String streamKey;

    @Value("${app.stream.position.maxlen}")
    public Long maxlen;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Instant start = Instant.now();
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = Long.valueOf(jobDetail.getJobDataMap().get(Constant.POSITION_CATEGORY_KEY).toString());
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        log.info("Starting position job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        long limit = 100;
        AtomicLong position = new AtomicLong(0);
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
                    log.warn("Skipping position job gql request for categoryId - {} with offset - {}, cause items is empty", categoryId, offset);
                    offset.addAndGet(limit);
                    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                    continue;
                }
                log.info("Iterate through products for position itemsCount={};categoryId={}", productItems.size(), categoryId);
                List<Callable<Void>> callables = new ArrayList<>();
                for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                    callables.add(postPositionRecord(productItem, position, categoryId));
                }
                callables.stream()
                        .map(jobExecutor::submit)
                        .toList()
                        .forEach(voidFuture -> {
                            try {
                                voidFuture.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                offset.addAndGet(limit);
                jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
            } catch (Exception e) {
                log.error("Search for position with category id [{}] finished with exception - [{}]", categoryId,
                        Optional.ofNullable(e.getCause()).orElse(e).getMessage());
                break;
            }
        }
        Instant end = Instant.now();
        log.info("Position job - Finished collecting for category id - {}, in {} seconds", categoryId,
                Duration.between(start, end).toSeconds());
    }

    private Callable<Void> postPositionRecord(KeGQLResponse.CatalogCardWrapper productItem, AtomicLong position, Long categoryId) {
        return () -> {
            position.incrementAndGet();
            Long itemId = Optional.ofNullable(productItem.getCatalogCard()).map(KeGQLResponse.CatalogCard::getProductId)
                    .orElseThrow(() -> new KeGqlRequestException("Catalog card can't be null"));
            KeGQLResponse.CatalogCard productItemCard = productItem.getCatalogCard();
            List<KeGQLResponse.CharacteristicValue> productItemCardCharacteristics = productItemCard.getCharacteristicValues();
            KeProduct.ProductData productResponse = jobUtilService.getProductData(itemId);
            if (productResponse == null) {
                log.info("Product data with id - %s returned null, continue with next item, if it exists...".formatted(itemId));
                return null;
            }
            if (!CollectionUtils.isEmpty(productItemCardCharacteristics)) {
                var productItemCardCharacteristic = productItemCardCharacteristics.get(0);
                var characteristicId = productItemCardCharacteristic.getId();
                var productCharacteristics = productResponse.getCharacteristics();

                Integer indexOfCharacteristic = null;
                CHARACTERISTICS:
                for (KeProduct.CharacteristicsData productCharacteristic : productCharacteristics) {
                    List<KeProduct.Characteristic> characteristicValues = productCharacteristic.getValues();
                    if (CollectionUtils.isEmpty(characteristicValues)) continue;
                    for (int index = 0; index < characteristicValues.size(); index++) {
                        KeProduct.Characteristic characteristic = characteristicValues.get(index);
                        if (characteristic != null && characteristic.getId().equals(characteristicId)) {
                            indexOfCharacteristic = index;
                            break CHARACTERISTICS;
                        }
                    }
                }

                if (indexOfCharacteristic == null) {
                    log.warn("Something goes wrong. Can't find index of characteristic." +
                            " productId={}; characteristicId={}", productItemCard.getProductId(), characteristicId);
                    return null;
                }
                Integer finalIndexOfCharacteristic = indexOfCharacteristic;
                List<Long> skuIds = productResponse.getSkuList()
                        .stream()
                        .filter(productSku -> {
                            if (!CollectionUtils.isEmpty(productSku.getCharacteristics())) {
                                boolean anyMatch = productSku.getCharacteristics().stream()
                                        .anyMatch(it -> finalIndexOfCharacteristic.equals(it.getValueIndex()));
                                return anyMatch && productSku.getAvailableAmount() > 0;
                            }
                            return false;
                        }).map(KeProduct.SkuData::getId).toList();
                for (Long skuId : skuIds) {
                    ProductPositionMessage positionMessage = ProductPositionMessage.builder()
                            .position(position.get())
                            .productId(productItemCard.getProductId())
                            .skuId(skuId)
                            .categoryId(categoryId)
                            .time(Instant.now().toEpochMilli())
                            .build();
                    RecordId recordId = streamCommands.xAdd(MapRecord.create(streamKey.getBytes(StandardCharsets.UTF_8),
                            Collections.singletonMap("position".getBytes(StandardCharsets.UTF_8),
                                    objectMapper.writeValueAsBytes(positionMessage))), RedisStreamCommands.XAddOptions.maxlen(maxlen));
                    log.info("Posted [stream={}] position record with id - [{}] for category id - [{}]",
                            streamKey, recordId, categoryId);
                }
            } else {
                List<Long> skuIds = productResponse.getSkuList()
                        .stream()
                        .filter(sku -> sku.getAvailableAmount() > 0)
                        .map(KeProduct.SkuData::getId)
                        .toList();

                for (Long skuId : skuIds) {
                    ProductPositionMessage positionMessage = ProductPositionMessage.builder()
                            .position(position.get())
                            .productId(productItemCard.getProductId())
                            .skuId(skuId)
                            .categoryId(categoryId)
                            .time(Instant.now().toEpochMilli())
                            .build();
                    RecordId recordId = streamCommands.xAdd(MapRecord.create(streamKey.getBytes(StandardCharsets.UTF_8),
                            Collections.singletonMap("position".getBytes(StandardCharsets.UTF_8),
                                    objectMapper.writeValueAsBytes(positionMessage))), RedisStreamCommands.XAddOptions.maxlen(maxlen));
                    log.info("Posted [stream={}] position record with id - [{}], for category id - [{}]",
                            streamKey, recordId, categoryId);
                }
            }
            return null;
        };
    }
}
