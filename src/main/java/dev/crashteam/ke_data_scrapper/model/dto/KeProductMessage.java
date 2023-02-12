package dev.crashteam.ke_data_scrapper.model.dto;

import dev.crashteam.ke_data_scrapper.model.ke.KeProduct;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KeProductMessage {

    private Long productId;
    private LocalDateTime time;
    private String title;
    private Long totalAvailableAmount;
    private Long orders;
    private Long reviewsAmount;
    private String rating;
    private List<KeItemSku> skuList;
    private KeProductSeller seller;
    private KeProduct.ProductCategory category;

    @Data
    @Builder
    public static class KeItemSku {
        private Long skuId;
        private String photoKey;
        private KeItemCharacteristic characteristic;
        private String purchasePrice;
        private String fullPrice;
        private Long availableAmount;
    }

    @Data
    @Builder
    public static class KeItemCharacteristic {
        private String type;
        private String title;
        private String value;
    }

    @Data
    @Builder
    public static class KeProductSeller {
        private Long id;
        private Long accountId;
        private String sellerLink;
        private String sellerTitle;
        private Long reviews;
        private Long orders;
        private String rating;
        private Long registrationDate;
    }

}
