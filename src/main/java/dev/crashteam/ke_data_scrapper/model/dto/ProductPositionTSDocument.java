package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductPositionTSDocument implements Serializable {
    private Long position;
    private Long productId;
    private Long skuId;
    private Long categoryId;
    private LocalDateTime time;
}
