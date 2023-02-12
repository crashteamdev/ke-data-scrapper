package dev.crashteam.ke_data_scrapper.job;

import dev.crashteam.ke_data_scrapper.exception.KeGqlRequestException;
import dev.crashteam.ke_data_scrapper.model.Constant;
import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import dev.crashteam.ke_data_scrapper.service.MessagePublisher;
import dev.crashteam.ke_data_scrapper.service.integration.KeService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Component
@DisallowConcurrentExecution
public class CategoryGqlJob implements Job {

    @Autowired
    List<MessagePublisher> messagePublishers;

    @Autowired
    KeService keService;

    @Autowired
    RetryTemplate retryTemplate;

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Long categoryId = (Long) jobDetail.getJobDataMap().get(Constant.CATEGORY_ID_KEY);
        jobDetail.getJobDataMap().put("offset", new AtomicLong(0));
        log.info("Starting job with category id - {}", categoryId);
        AtomicLong offset = (AtomicLong) jobDetail.getJobDataMap().get("offset");
        long limit = 48;
        while (true) {
            try {
                KeGQLResponse gqlResponse = retryTemplate.execute((RetryCallback<KeGQLResponse, KeGqlRequestException>) retryContext -> {
                    KeGQLResponse response = keService.getGQLSearchResponse(String.valueOf(categoryId), offset.get(), limit);
                    if (!CollectionUtils.isEmpty(response.getErrors())) {
                        for (KeGQLResponse.GQLError error : response.getErrors()) {
                            if (error.getMessage().contains("offset")) {
                                log.info("Finished collecting data for id - {}, " +
                                        "because of response error object with message - {}", categoryId, error.getMessage());
                            } else {
                                offset.addAndGet(limit);
                                jobExecutionContext.getJobDetail().getJobDataMap().put("offset", offset);
                                throw new KeGqlRequestException("Request ended with error message - %s".formatted(error.getMessage()));
                            }
                        }
                    }
                    return response;
                });

                if (gqlResponse == null || !CollectionUtils.isEmpty(gqlResponse.getErrors())) {
                    break;
                }

                var productItems = Optional.ofNullable(gqlResponse.getData()
                                .getMakeSearch()).map(KeGQLResponse.MakeSearch::getItems)
                        .orElseThrow(() -> new KeGqlRequestException("Item's can't be null!"));
                log.info("Iterate through products for itemsCount={};categoryId={}", productItems.size(), categoryId);

                for (KeGQLResponse.CatalogCardWrapper productItem : productItems) {
                    Long itemId = Optional.ofNullable(productItem.getCatalogCard()).map(KeGQLResponse.CatalogCard::getId)
                            .orElseThrow(() -> new KeGqlRequestException("Catalog card can't be null"));
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

                    if (productData == null) {
                        log.info("Product data with id - %s returned null, continue with next item, if it exists...".formatted(itemId));
                        continue;
                    }
                    KeProduct.ProductSeller productSeller = productData.getSeller();
                    KeProductMessage.KeProductSeller seller = KeProductMessage.KeProductSeller.builder()
                            .accountId(productSeller.getSellerAccountId())
                            .id(productSeller.getId())
                            .rating(productSeller.getRating())
                            .registrationDate(productSeller.getRegistrationDate())
                            .reviews(productSeller.getReviews())
                            .sellerLink(productSeller.getLink())
                            .sellerTitle(productSeller.getTitle())
                            .orders(productSeller.getOrders())
                            .build();

                    List<KeProductMessage.KeItemSku> skuList = productData.getSkuList()
                            .stream()
                            .map(sku -> {
//                                sku.getCharacteristics().stream()
//                                        .filter()
                                return KeProductMessage.KeItemSku.builder()
                                        .skuId(sku.getId())
                                        .availableAmount(sku.getAvailableAmount())
                                        .fullPrice(sku.getFullPrice())
                                        .purchasePrice(sku.getPurchasePrice())
                                        .build();
                            }).toList();

                    KeProductMessage productMessage = KeProductMessage.builder()
                            .rating(productData.getRating())
                            .category(productData.getCategory())
                            .orders(productData.getOrdersAmount())
                            .productId(productData.getId())
                            .reviewsAmount(productData.getReviewsAmount())
                            .time(LocalDateTime.now())
                            .title(productData.getTitle())
                            .totalAvailableAmount(productData.getTotalAvailableAmount())
                            .seller(seller)
                            .skuList(skuList)
                            .build();


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
