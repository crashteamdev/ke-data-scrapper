package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.exception.KeGqlRequestException;
import dev.crashteam.ke_data_scrapper.mapper.KeProductToCachedProduct;
import dev.crashteam.ke_data_scrapper.model.cache.CachedProductData;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobUtilService {

    private final KeService keService;
    private final RetryTemplate retryTemplate;

    public KeProduct.ProductData getProductData(Long itemId) {
        return retryTemplate.execute((RetryCallback<KeProduct.ProductData, KeGqlRequestException>) retryContext -> {
            KeProduct product = keService.getProduct(itemId);
            if (!CollectionUtils.isEmpty(product.getErrors())) {
                String errorMessage = product.getErrors()
                        .stream()
                        .map(KeProduct.ProductError::getDetailMessage)
                        .findFirst()
                        .orElse("");
                throw new KeGqlRequestException("Get product failed with message - %s".formatted(errorMessage));
            }
            return Optional.ofNullable(product.getPayload()).map(KeProduct.Payload::getData)
                    .orElseThrow(() -> new KeGqlRequestException("Product catalog can't be null"));
        });
    }


    @Cacheable(value = "productCache")
    public CachedProductData getCachedProductData(Long itemId) {
        return KeProductToCachedProduct.toCachedData(getProductData(itemId));
    }

    public KeGQLResponse getResponse(JobExecutionContext jobExecutionContext, AtomicLong offset, Long categoryId, Long limit) {
        return retryTemplate.execute((RetryCallback<KeGQLResponse, KeGqlRequestException>) retryContext -> {
            try {
                KeGQLResponse response = keService.getGQLSearchResponse(String.valueOf(categoryId), offset.get(), limit);
                if (!CollectionUtils.isEmpty(response.getErrors())) {
                    for (KeGQLResponse.GQLError error : response.getErrors()) {
                        if (error.getMessage().contains("offset")) {
                            log.warn("Finished collecting data for id - {}, " +
                                    "because of response error object with message - {}", categoryId, error.getMessage());
                            return null;
                        } else if (error.getMessage().contains("429")) {
                            log.warn("Got 429 http status from request for category id {}", categoryId);
                            throw new KeGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                        } else {
                            offset.addAndGet(limit);
                            jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                            throw new KeGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                        }
                    }
                }
                return response;
            } catch (Exception e) {
                log.error("GQL ERROR, retrying", e);
                throw new KeGqlRequestException();
            }
        });
    }
}
