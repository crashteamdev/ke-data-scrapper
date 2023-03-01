package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class KeProductToMessageMapper {

    public static KeProductMessage productToMessage(KeProduct.ProductData productData) {

        List<KeProductMessage.CharacteristicsData> characteristicsData = new ArrayList<>();
        for (KeProduct.CharacteristicsData characteristic : productData.getCharacteristics()) {
            var messageCharacteristic = new KeProductMessage.CharacteristicsData();
            messageCharacteristic.setId(characteristic.getId());
            messageCharacteristic.setTitle(characteristic.getTitle());
            List<KeProductMessage.Characteristic> characteristicsValues = new ArrayList<>();
            for (KeProduct.Characteristic characteristicValue : characteristic.getValues()) {
                var messageCharacteristicValue = new KeProductMessage.Characteristic();
                messageCharacteristicValue.setValue(characteristicValue.getValue());
                messageCharacteristicValue.setTitle(characteristicValue.getTitle());
                messageCharacteristicValue.setId(characteristicValue.getId());
                characteristicsValues.add(messageCharacteristicValue);
            }
            messageCharacteristic.setValues(characteristicsValues);
            characteristicsData.add(messageCharacteristic);
        }

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
                .category(getCategory(productData.getCategory()))
                .orders(productData.getOrdersAmount())
                .productId(productData.getId())
                .reviewsAmount(productData.getReviewsAmount())
                .description(productData.getDescription())
                .tags(productData.getTags())
                .attributes(productData.getAttributes())
                .time(Instant.now().toEpochMilli())
                .title(productData.getTitle())
                .totalAvailableAmount(productData.getTotalAvailableAmount())
                .seller(getSeller(productData))
                .skuList(skuList)
                .characteristics(characteristicsData)
                .isEco(productData.isEco())
                .isPerishable(productData.isPerishable())
                .build();

    }

    private static KeProductMessage.ProductCategory getCategory(KeProduct.ProductCategory productCategory) {
        KeProductMessage.ProductCategory category = new KeProductMessage.ProductCategory();
        category.setId(productCategory.getId());
        category.setProductAmount(productCategory.getProductAmount());
        category.setTitle(productCategory.getTitle());
        if (productCategory.getParent() != null) {
            category.setParent(getCategory(productCategory.getParent()));
        }
        return category;
    }

    private static KeProductMessage.KeProductSeller getSeller(KeProduct.ProductData productData) {
        KeProduct.ProductSeller productSeller = productData.getSeller();
        if (productSeller != null) {
            List<KeProductMessage.Contact> contacts = new ArrayList<>();
            for (KeProduct.Contact contact : productSeller.getContacts()) {
                KeProductMessage.Contact messageContact = new KeProductMessage.Contact();
                messageContact.setType(contact.getType());
                messageContact.setValue(contact.getValue());
                contacts.add(messageContact);
            }
            return KeProductMessage.KeProductSeller.builder()
                    .accountId(productSeller.getSellerAccountId())
                    .id(productSeller.getId())
                    .rating(productSeller.getRating())
                    .registrationDate(productSeller.getRegistrationDate())
                    .reviews(productSeller.getReviews())
                    .sellerLink(productSeller.getLink())
                    .sellerTitle(productSeller.getTitle())
                    .orders(productSeller.getOrders())
                    .contacts(contacts)
                    .build();
        }
        return null;
    }
}
