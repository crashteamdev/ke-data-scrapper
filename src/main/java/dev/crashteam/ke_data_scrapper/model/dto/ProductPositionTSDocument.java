package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ProductPositionTSDocument implements Serializable {
    private Long position;
    private Long productId;
    private Long skuId;
    private Long categoryId;
    private Long time;
}
