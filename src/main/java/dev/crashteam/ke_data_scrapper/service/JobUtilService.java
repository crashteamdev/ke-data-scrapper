package dev.crashteam.ke_data_scrapper.service;

import dev.crashteam.ke_data_scrapper.exception.KeGqlRequestException;
import dev.crashteam.ke_data_scrapper.mapper.cache.KeGraphToCachedGraph;
import dev.crashteam.ke_data_scrapper.mapper.cache.KeProductToCachedProduct;
import dev.crashteam.ke_data_scrapper.model.cache.CachedProductData;
import dev.crashteam.ke_data_scrapper.model.cache.GraphQlCacheData;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobUtilService {

    private final KeService keService;
    private final RetryTemplate retryTemplate;
    private final MetricService metricService;

    public KeProduct.ProductData getProductData(Long itemId) {
        Instant start = Instant.now();
        KeProduct.ProductData productData = retryTemplate.execute((RetryCallback<KeProduct.ProductData, KeGqlRequestException>) retryContext -> {
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
        Instant end = Instant.now();
        metricService.recordResponseTime(Duration.between(start, end).toMillis(), "mm_product_data");
        return productData;
    }


    @Cacheable(value = "productCache")
    public CachedProductData getCachedProductData(Long itemId) {
        return KeProductToCachedProduct.toCachedData(getProductData(itemId));
    }

    @CachePut(value = "productCache", key = "#itemId")
    public CachedProductData putCachedProductData(KeProduct.ProductData productData, Long itemId) {
        return KeProductToCachedProduct.toCachedData(productData);
    }

    @CacheEvict(value = "productCache", allEntries = true)
    public void evictProductCache() {
        log.info("Deleting product cache");
    }

    @Cacheable(value = "graphQlCache", key = "#offset + #categoryId + #limit")
    public GraphQlCacheData getCachedGraphData(AtomicLong offset, Long categoryId, Long limit) {
        return KeGraphToCachedGraph.keGQLResponseToCachedGraph(getResponse(offset, categoryId, limit));
    }

    public GraphQlCacheData getSimplifiedGraphData(AtomicLong offset, Long categoryId, Long limit) {
        return KeGraphToCachedGraph.keGQLResponseToCachedGraph(getResponse(offset, categoryId, limit));
    }

    @CachePut(value = "graphQlCache", key = "#offset + #categoryId + #limit")
    public GraphQlCacheData putCachedGraphData(KeGQLResponse keGQLResponse, AtomicLong offset, Long categoryId, Long limit) {
        return KeGraphToCachedGraph.keGQLResponseToCachedGraph(keGQLResponse);
    }

    @CacheEvict(value = "graphQlCache", allEntries = true)
    public void evictGraphCache() {
        log.info("Deleting graph cache");
    }

    public KeGQLResponse getResponse(JobExecutionContext jobExecutionContext, AtomicLong offset, Long categoryId, Long limit) {
        Instant start = Instant.now();
        KeGQLResponse gqlResponse = retryTemplate.execute((RetryCallback<KeGQLResponse, KeGqlRequestException>) retryContext -> {
            try {
                KeGQLResponse response = keService.getLightGqlSearchResponse(String.valueOf(categoryId), offset.get(), limit);
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
        Instant end = Instant.now();
        metricService.recordResponseTime(Duration.between(start, end).toMillis(), "mm_gql_data");
        return gqlResponse;
    }

    public KeGQLResponse getResponse(AtomicLong offset, Long categoryId, Long limit) {
        Instant start = Instant.now();
        KeGQLResponse gqlResponse = retryTemplate.execute((RetryCallback<KeGQLResponse, KeGqlRequestException>) retryContext -> {
            try {
                KeGQLResponse response = keService.getLightGqlSearchResponse(String.valueOf(categoryId), offset.get(), limit);
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
        Instant end = Instant.now();
        metricService.recordResponseTime(Duration.between(start, end).toMillis(), "mm_gql_data");
        return gqlResponse;
    }
}
