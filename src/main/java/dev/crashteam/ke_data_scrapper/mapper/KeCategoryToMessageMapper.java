package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.dto.KeCategoryMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KeCategoryToMessageMapper {

    public static KeCategoryMessage categoryToMessage(KeCategory.Data data) {
        KeCategoryMessage categoryMessage = KeCategoryMessage.builder()
                .id(data.getId())
                .adult(data.isAdult())
                .eco(data.isEco())
                .title(data.getTitle())
                .timestamp(LocalDateTime.now())
                .build();
        if (!CollectionUtils.isEmpty(data.getChildren())) {
            List<KeCategoryMessage> childrenCategories = new ArrayList<>();
            for (KeCategory.Data child : data.getChildren()) {
                childrenCategories.add(categoryToMessage(child));
            }
            categoryMessage.setChildren(childrenCategories);
        }
        return categoryMessage;
    }
}
