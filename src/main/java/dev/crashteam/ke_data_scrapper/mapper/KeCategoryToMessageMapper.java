package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.dto.KeCategoryMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class KeCategoryToMessageMapper {

    public static KeCategoryMessage categoryToMessage(KeCategory.Data data) {
        KeCategoryMessage categoryMessage = KeCategoryMessage.builder()
                .id(data.getId())
                .adult(data.isAdult())
                .eco(data.isEco())
                .title(data.getTitle())
                .time(Instant.now().toEpochMilli())
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

    public static KeCategoryMessage categoryToMessage(KeCategory.Data category, List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
        KeCategoryMessage categoryMessage = KeCategoryMessage.builder()
                .id(category.getId())
                .adult(category.isAdult())
                .eco(category.isEco())
                .title(category.getTitle())
                .time(Instant.now().toEpochMilli())
                .children(new ArrayList<>())
                .build();
        if (!CollectionUtils.isEmpty(category.getChildren())) {
            List<KeCategoryMessage> childrenCategories = new ArrayList<>();
            for (KeCategory.Data child : category.getChildren()) {
                childrenCategories.add(categoryToMessage(child));
            }
            categoryMessage.getChildren().addAll(childrenCategories);
        }
        if (hasChildren(category.getId(), categoryTree)) {
            List<KeCategoryMessage> childrenCategories = new ArrayList<>();
            Set<KeGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), category.getId()))
                    .collect(Collectors.toSet());
            for (KeGQLResponse.ResponseCategoryWrapper responseCategory : responseCategories) {
                childrenCategories.add(mapChildrenCategory(responseCategory, categoryTree, category.isEco()));
            }
            categoryMessage.getChildren().addAll(childrenCategories);
        }
        return categoryMessage;
    }

    private static KeCategoryMessage mapChildrenCategory(
            KeGQLResponse.ResponseCategoryWrapper responseCategory,
            List<KeGQLResponse.ResponseCategoryWrapper> categoryTree,
            boolean isEco) {
        KeGQLResponse.ResponseCategory childCategory = responseCategory.getCategory();
        KeCategoryMessage categoryMessage = KeCategoryMessage.builder()
                .id(childCategory.getId())
                .adult(childCategory.isAdult())
                .eco(isEco)
                .title(childCategory.getTitle())
                .time(Instant.now().toEpochMilli())
                .children(new ArrayList<>())
                .build();
        log.info("CHILD CATEGORY - {}", childCategory.getId());
        if (hasChildren(childCategory.getId(), categoryTree)) {
            List<KeCategoryMessage> childrenCategories = new ArrayList<>();
            Set<KeGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), childCategory.getId()))
                    .collect(Collectors.toSet());
            for (KeGQLResponse.ResponseCategoryWrapper category : responseCategories) {
                childrenCategories.add(mapChildrenCategory(category, categoryTree, isEco));
            }
            categoryMessage.getChildren().addAll(childrenCategories);
        }

        return categoryMessage;
    }

    private static boolean hasChildren(Long categoryId, List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
        return categoryTree.stream().anyMatch(it -> it.getCategory().getParent() != null
                && Objects.equals(it.getCategory().getParent().getId(), categoryId));
    }
}
