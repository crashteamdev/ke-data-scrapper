package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


public class KeProductToMessageMapper {

    public static KeProductMessage productToMessage(KeProduct.ProductData productData) {
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
                    List<KeProductMessage.KeItemCharacteristic> characteristics = sku.getCharacteristics()
                            .stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                return KeProductMessage.KeItemCharacteristic.builder()
                                        .type(productCharacteristic.getTitle())
                                        .title(characteristicValue.getTitle())
                                        .value(characteristicValue.getValue()).build();
                            }).toList();
                    KeProduct.ProductPhoto productPhoto = sku.getCharacteristics().stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                var value = characteristicValue.getValue();
                                return productData.getPhotos().stream()
                                        .filter(photo -> photo.getColor() != null)
                                        .filter(photo -> photo.getColor().equals(value))
                                        .findFirst()
                                        .orElse(null);
                            }).filter(Objects::nonNull).findFirst().orElse(productData.getPhotos()
                                    .stream().findFirst().orElse(null));
                    return KeProductMessage.KeItemSku.builder()
                            .skuId(sku.getId())
                            .availableAmount(sku.getAvailableAmount())
                            .fullPrice(sku.getFullPrice())
                            .purchasePrice(sku.getPurchasePrice())
                            .characteristics(characteristics)
                            .photoKey(productPhoto != null ? productPhoto.getPhotoKey() : null)
                            .build();
                }).toList();

        return KeProductMessage.builder()
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
}
