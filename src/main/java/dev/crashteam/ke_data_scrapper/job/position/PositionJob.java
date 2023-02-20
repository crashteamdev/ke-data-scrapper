package dev.crashteam.ke_data_scrapper.job.position;

import dev.crashteam.ke_data_scrapper.exception.KeGqlRequestException;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.ProductPositionMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.JobUtilService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@DisallowConcurrentExecution
public class PositionJob implements Job {

    @Autowired
    JobUtilService jobUtilService;

    @Autowired
    RedisStreamCommands streamCommands;

    @Value("${app.stream.position.key}")
    public String streamKey;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = (Long) jobDetail.getJobDataMap().get(Constant.POSITION_CATEGORY_KEY);
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        log.info("Starting position job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        long limit = 48;
        long position = 0;
        while (true) {
            try {
                KeGQLResponse gqlResponse = jobUtilService.getResponse(jobExecutionContext, offset, categoryId, limit);
                if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                    break;
                }
                var productItems = Optional.ofNullable(gqlResponse.getData()
                                .getMakeSearch()).map(KeGQLResponse.MakeSearch::getItems)
                        .orElseThrow(() -> new KeGqlRequestException("Item's can't be null!"));
                log.info("Iterate through products for position itemsCount={};categoryId={}", productItems.size(), categoryId);

                for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                    position += 1;
                    Long itemId = Optional.ofNullable(productItem.getCatalogCard()).map(KeGQLResponse.CatalogCard::getProductId)
                            .orElseThrow(() -> new KeGqlRequestException("Catalog card can't be null"));
                    KeGQLResponse.CatalogCard productItemCard = productItem.getCatalogCard();
                    List<KeGQLResponse.CharacteristicValue> productItemCardCharacteristics = productItemCard.getCharacteristicValues();
                    KeProduct.ProductData productResponse = jobUtilService.getProductData(itemId);
                    if (productResponse == null) {
                        log.info("Product data with id - %s returned null, continue with next item, if it exists...".formatted(itemId));
                        continue;
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
                            continue;
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
                                    .position(position)
                                    .productId(productItemCard.getProductId())
                                    .skuId(skuId)
                                    .categoryId(categoryId)
                                    .time(Instant.now().toEpochMilli())
                                    .build();
                            RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                                    Collections.singletonMap("position".getBytes(StandardCharsets.UTF_8),
                                            SerializationUtils.serialize(positionMessage)));
                            log.info("Posted [stream={}] position record with id - [{}]",
                                    streamKey, recordId);
                        }
                    } else {
                        List<Long> skuIds = productResponse.getSkuList()
                                .stream()
                                .filter(sku -> sku.getAvailableAmount() > 0)
                                .map(KeProduct.SkuData::getId)
                                .toList();

                        for (Long skuId : skuIds) {
                            ProductPositionMessage positionMessage = ProductPositionMessage.builder()
                                    .position(position)
                                    .productId(productItemCard.getProductId())
                                    .skuId(skuId)
                                    .categoryId(categoryId)
                                    .time(Instant.now().toEpochMilli())
                                    .build();
                            RecordId recordId = streamCommands.xAdd(streamKey.getBytes(StandardCharsets.UTF_8),
                                    Collections.singletonMap("position".getBytes(StandardCharsets.UTF_8),
                                            SerializationUtils.serialize(positionMessage)));
                            log.info("Posted [stream={}] position record with id - [{}]",
                                    streamKey, recordId);
                        }
                    }
                    offset.addAndGet(limit);
                    jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                }
            } catch (Exception e) {
                log.error("Gql search for catalog with id [{}] finished with exception - [{}]", categoryId,
                        Optional.ofNullable(e.getCause()).orElse(e).getMessage());
                break;
            }
        }
    }
}
