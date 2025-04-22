package dev.crashteam.ke_data_scrapper.model.ke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeProduct implements Serializable {

    private Payload payload;
    private List<ProductError> errors;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload implements Serializable {
        private ProductData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductData implements Serializable {
        private Long id;
        private String title;
        private ProductCategory category;
        private String rating;
        private Long reviewsAmount;
        private Long ordersAmount;
        private Long totalAvailableAmount;
        private Long charityCommission;
        private String description;
        private List<String> attributes;
        private List<String> tags;
        private List<ProductPhoto> photos;
        private List<CharacteristicsData> characteristics;
        private List<SkuData> skuList;
        private ProductSeller seller;
        private Feedback topFeedback;
        private boolean isEco;
        private boolean adultCategory;

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductCategory implements Serializable {
        private Long id;
        private String title;
        private Long productAmount;
        private ProductCategory parent;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductPhoto implements Serializable {
        private String color;
        private String photoKey;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CharacteristicsData implements Serializable {
        private Long id;
        private String title;
        private List<Characteristic> values;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Characteristic implements Serializable{
        private Long id;
        private String title;
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkuData implements Serializable {
        private Long id;
        private List<ScuCharacteristic> characteristics;
        private Long availableAmount;
        private String fullPrice;
        private String charityProfit;
        private String purchasePrice;
        private String barcode;
        private Long sellPrice;
        private Restriction restriction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScuCharacteristic implements Serializable {
        private Integer charIndex;
        private Integer valueIndex;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductSeller implements Serializable {
        private Long id;
        private String title;
        private String link;
        private String description;
        private Long registrationDate;
        private String rating;
        private Long reviews;
        private Long orders;
        private Long sellerAccountId;
        private List<Contact> contacts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feedback implements Serializable {
        private Long reviewId;
        private Long productId;
        private Long date;
        private Boolean edited;
        private String customer;
        private FeedBackReply reply;
        private Long rating;
        private String content;
        private String status;
        private Long id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeedBackReply implements Serializable {
        private Long id;
        private Long date;
        private Boolean edited;
        private String content;
        private String shop;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductError implements Serializable {
        private String code;
        private String message;
        private String detailMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact implements Serializable {
        private String type;
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Restriction implements Serializable {
        private Long restrictedAmount;
        private Long boughtAmount;
        private Boolean restricted;
    }
}
