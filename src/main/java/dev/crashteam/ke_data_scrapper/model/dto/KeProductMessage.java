package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KeProductMessage implements Serializable {

    private Long productId;
    private LocalDateTime time;
    private String title;
    private Long totalAvailableAmount;
    private Long orders;
    private Long reviewsAmount;
    private String rating;
    private List<KeItemSku> skuList;
    private KeProductSeller seller;
    private ProductCategory category;

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
    }

    @Data
    public static class ProductCategory implements Serializable {
        private Long id;
        private String title;
        private Long productAmount;
        private ProductCategory parent;
    }
}
