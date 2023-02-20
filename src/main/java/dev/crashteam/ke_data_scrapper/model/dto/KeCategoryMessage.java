package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KeCategoryMessage implements Serializable {

    private Long id;
    private Boolean adult;
    private Boolean eco;
    private String title;
    private List<KeCategoryMessage> children;
    private LocalDateTime timestamp;
}
