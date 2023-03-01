package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class KeProductMessage implements Serializable {

    private Long productId;
    private Long time;
    private String title;
    private Long totalAvailableAmount;
    private Long orders;
    private Long reviewsAmount;
    private String rating;
    private String description;
    private List<String> tags;
    private List<String> attributes;
    private List<KeItemSku> skuList;
    private KeProductSeller seller;
    private ProductCategory category;
    private List<CharacteristicsData> characteristics;
    private boolean isEco;
    private boolean isAdult;

    @Data
    @Builder
    public static class KeItemSku implements Serializable {
        private Long skuId;
        private String photoKey;
        private List<KeItemCharacteristic> characteristics;
        private String purchasePrice;
        private String fullPrice;
        private Long availableAmount;
    }

    @Data
    @Builder
    public static class KeItemCharacteristic implements Serializable {
        private String type;
        private String title;
        private String value;
    }

    @Data
    @Builder
    public static class KeProductSeller implements Serializable {
        private Long id;
        private Long accountId;
        private String sellerLink;
        private String sellerTitle;
        private Long reviews;
        private Long orders;
        private String rating;
        private Long registrationDate;
        private String description;
        private List<Contact> contacts;
    }

    @Data
    public static class ProductCategory implements Serializable {
        private Long id;
        private String title;
        private Long productAmount;
        private ProductCategory parent;
    }

    @Data
    public static class CharacteristicsData {
        private Long id;
        private String title;
        private List<Characteristic> values;
    }

    @Data
    public static class Characteristic {
        private Long id;
        private String title;
        private String value;
    }

    @Data
    public static class ProductPhoto {
        private String color;
        private String photoKey;
    }

    @Data
    public static class Contact {
        private String type;
        private String value;
    }
}
