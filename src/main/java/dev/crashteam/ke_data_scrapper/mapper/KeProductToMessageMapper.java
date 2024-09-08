package dev.crashteam.ke_data_scrapper.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.ke.scrapper.data.v1.KeProductChange;
import dev.crashteam.ke.scrapper.data.v1.KeSkuCharacteristic;
import dev.crashteam.ke.scrapper.data.v1.KeValue;
import dev.crashteam.ke_data_scrapper.model.dto.KeProductMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import lombok.val;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
public class KeProductToMessageMapper {

    public KeProductChange mapToMessage(KeProduct.ProductData productData) {
        List<KeProductChange.KeProductCharacteristic> characteristicsData = new ArrayList<>();
        for (KeProduct.CharacteristicsData characteristic : productData.getCharacteristics()) {
            List<KeValue> characteristicsValues = new ArrayList<>();
            for (KeProduct.Characteristic characteristicValue : characteristic.getValues()) {
                var keValue = KeValue.newBuilder()
                        .setValue(characteristicValue.getValue())
                        .setTitle(characteristicValue.getTitle())
                        .setId(characteristicValue.getId().toString())
                        .build();
                characteristicsValues.add(keValue);
            }
            var productCharacteristic = KeProductChange.KeProductCharacteristic.newBuilder()
                    .setId(characteristic.getId())
                    .setTitle(characteristic.getTitle())
                    .addAllValues(characteristicsValues)
                    .build();
            characteristicsData.add(productCharacteristic);
        }
        AtomicBoolean isCorrupted = new AtomicBoolean(false);
        List<KeProductChange.KeProductSku> skuList = productData.getSkuList()
                .stream()
                .map(sku -> {
                    List<KeSkuCharacteristic> characteristics = sku.getCharacteristics()
                            .stream()
                            .map(it -> {
                                var productCharacteristic = productData
                                        .getCharacteristics().get(it.getCharIndex());
                                var characteristicValue = productCharacteristic
                                        .getValues().get(it.getValueIndex());
                                return KeSkuCharacteristic.newBuilder()
                                        .setType(productCharacteristic.getTitle())
                                        .setTitle(characteristicValue.getTitle())
                                        .setValue(characteristicValue.getValue()).build();
                            }).toList();
                    KeProduct.ProductPhoto productPhoto = extractProductPhoto(productData, sku);
                    if (productPhoto == null) {
                        isCorrupted.set(true);
                    }
                    KeProductChange.Restriction restriction;
                    KeProduct.Restriction skuRestriction = sku.getRestriction();
                    val keProductBuilder = KeProductChange.KeProductSku.newBuilder()
                            .setSkuId(sku.getId().toString())
                            .setAvailableAmount(sku.getAvailableAmount())
                            .setPurchasePrice(sku.getPurchasePrice())
                            .addAllCharacteristics(characteristics);
                    if (productPhoto != null) {
                        keProductBuilder.setPhotoKey(productPhoto.getPhotoKey());
                    }
                    if (skuRestriction != null && skuRestriction.getRestrictedAmount() != null
                            && skuRestriction.getRestrictedAmount() > 0) {
                        restriction = KeProductChange.Restriction
                                .newBuilder()
                                .setBoughtAmount(skuRestriction.getBoughtAmount())
                                .setRestrictedAmount(skuRestriction.getRestrictedAmount())
                                .build();
                        keProductBuilder.setRestriction(restriction);
                    }

                    if (sku.getFullPrice() != null) {
                        keProductBuilder.setFullPrice(sku.getFullPrice());
                    }
                    return keProductBuilder.build();
                }).toList();
        if (isCorrupted.get()) {
            throw new ProductCorruptedException("Corrupted item. productId=%s".formatted(productData.getId()));
        }

        KeProductChange.Builder builder = KeProductChange.newBuilder()
                .setRating(Double.parseDouble(productData.getRating()))
                .setCategory(mapCategory(productData.getCategory()))
                .setOrders(productData.getOrdersAmount())
                .setProductId(productData.getId().toString())
                .setReviewsAmount(productData.getReviewsAmount())
                .addAllTags(productData.getTags())
                .addAllAttributes(productData.getAttributes())
                .setTitle(productData.getTitle())
                .setTotalAvailableAmount(productData.getTotalAvailableAmount())
                .setSeller(mapSeller(productData))
                .addAllSkus(skuList)
                .addAllCharacteristics(characteristicsData)
                .setIsEco(productData.isEco())
                .setIsAdult(productData.isAdultCategory());
        if (productData.getDescription() != null) {
            builder.setDescription(productData.getDescription());
        }
        return builder.build();
    }

    private KeProduct.ProductPhoto extractProductPhoto(KeProduct.ProductData product, KeProduct.SkuData sku) {
        return sku.getCharacteristics().stream()
                .map(it -> {
                    var productCharacteristic = product
                            .getCharacteristics().get(it.getCharIndex());
                    var characteristicValue = productCharacteristic
                            .getValues().get(it.getValueIndex());
                    var value = characteristicValue.getValue();
                    return product.getPhotos().stream()
                            .filter(photo -> photo.getColor() != null)
                            .filter(photo -> photo.getColor().equals(value))
                            .findFirst()
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(product.getPhotos()
                        .stream().findFirst().orElse(null));
    }

    private KeProductChange.KeProductCategory mapCategory(KeProduct.ProductCategory productCategory) {
        KeProductChange.KeProductCategory.Builder builder = KeProductChange.KeProductCategory.newBuilder()
                .setId(productCategory.getId())
                .setProductAmount(productCategory.getProductAmount())
                .setTitle(productCategory.getTitle());
        if (productCategory.getParent() != null) {
            builder.setParent(mapCategory(productCategory.getParent()));
        }
        return builder.build();
    }

    private KeProductChange.KeProductSeller mapSeller(KeProduct.ProductData productData) {
        KeProduct.ProductSeller productSeller = productData.getSeller();
        if (productSeller != null) {
            List<KeProductChange.KeProductContact> contacts = new ArrayList<>();
            for (KeProduct.Contact contact : productSeller.getContacts()) {
                var keProductContact = KeProductChange.KeProductContact.newBuilder()
                        .setType(contact.getType())
                        .setValue(contact.getValue())
                        .build();
                contacts.add(keProductContact);
            }
            return KeProductChange.KeProductSeller.newBuilder()
                    .setAccountId(productSeller.getSellerAccountId())
                    .setId(productSeller.getId())
                    .setRating(productSeller.getRating())
                    .setRegistrationDate(Timestamp.newBuilder().setSeconds(productSeller.getRegistrationDate()).build())
                    .setReviews(productSeller.getReviews())
                    .setSellerLink(productSeller.getLink())
                    .setSellerTitle(productSeller.getTitle())
                    .setOrders(productSeller.getOrders())
                    .addAllContacts(contacts)
                    .build();
        }
        return null;
    }

    public KeProductMessage productToMessage(KeProduct.ProductData productData) {

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
        AtomicBoolean isCorrupted = new AtomicBoolean(false);
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
                            })
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(productData.getPhotos()
                                    .stream().findFirst().orElse(null));
                    if (productPhoto == null) {
                        isCorrupted.set(true);
                    }
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
                .isAdult(productData.isAdultCategory())
                .isCorrupted(isCorrupted.get())
                .build();

    }

    private KeProductMessage.ProductCategory getCategory(KeProduct.ProductCategory productCategory) {
        KeProductMessage.ProductCategory category = new KeProductMessage.ProductCategory();
        category.setId(productCategory.getId());
        category.setProductAmount(productCategory.getProductAmount());
        category.setTitle(productCategory.getTitle());
        if (productCategory.getParent() != null) {
            category.setParent(getCategory(productCategory.getParent()));
        }
        return category;
    }

    private KeProductMessage.KeProductSeller getSeller(KeProduct.ProductData productData) {
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
