package dev.crashteam.ke_data_scrapper.mapper;

import dev.crashteam.ke_data_scrapper.model.dto.KeCategoryMessage;
import dev.crashteam.ke_data_scrapper.model.ke.KeCategory;
import dev.crashteam.ke_data_scrapper.model.ke.KeGQLResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KeCategoryToMessageMapper {

    public dev.crashteam.ke.scrapper.data.v1.KeCategory mapToMessage(
            KeCategory.Data category,
            List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
        var categoryBuilder = dev.crashteam.ke.scrapper.data.v1.KeCategory.newBuilder()
                .setCategoryId(category.getId())
                .setIsAdult(category.isAdult())
                .setIsEco(category.isEco())
                .setTitle(category.getTitle());
        log.info("CATEGORY - {}", category.getId());

        if (!CollectionUtils.isEmpty(category.getChildren())) {
            List<dev.crashteam.ke.scrapper.data.v1.KeCategory> childrenCategories = new ArrayList<>();
            for (KeCategory.Data child : category.getChildren()) {
                childrenCategories.add(mapToMessage(child, categoryTree));
            }
            categoryBuilder.addAllChildren(childrenCategories);
        }

        if (hasChildren(category.getId(), categoryTree)) {
            List<dev.crashteam.ke.scrapper.data.v1.KeCategory> childrenCategories = new ArrayList<>();
            Set<KeGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), category.getId()))
                    .collect(Collectors.toSet());
            for (KeGQLResponse.ResponseCategoryWrapper responseCategory : responseCategories) {
                childrenCategories.add(mapAwsChildrenCategory(responseCategory, categoryTree, category.isEco()));
            }
            categoryBuilder.addAllChildren(childrenCategories);
        }

        return categoryBuilder.build();
    }

    private dev.crashteam.ke.scrapper.data.v1.KeCategory mapAwsChildrenCategory(
            KeGQLResponse.ResponseCategoryWrapper responseCategory,
            List<KeGQLResponse.ResponseCategoryWrapper> categoryTree,
            boolean isEco) {
        KeGQLResponse.ResponseCategory childCategory = responseCategory.getCategory();
        var categoryBuilder = dev.crashteam.ke.scrapper.data.v1.KeCategory.newBuilder()
                .setCategoryId(childCategory.getId())
                .setIsAdult(childCategory.isAdult())
                .setIsEco(isEco)
                .setTitle(childCategory.getTitle());
        log.info("CHILD CATEGORY - {}", childCategory.getId());
        if (hasChildren(childCategory.getId(), categoryTree)) {
            List<dev.crashteam.ke.scrapper.data.v1.KeCategory> childrenCategories = new ArrayList<>();
            Set<KeGQLResponse.ResponseCategoryWrapper> responseCategories = categoryTree.stream()
                    .filter(it -> it.getCategory().getParent() != null
                            && Objects.equals(it.getCategory().getParent().getId(), childCategory.getId()))
                    .collect(Collectors.toSet());
            for (KeGQLResponse.ResponseCategoryWrapper category : responseCategories) {
                childrenCategories.add(mapAwsChildrenCategory(category, categoryTree, isEco));
            }
            categoryBuilder.addAllChildren(childrenCategories);
        }

        return categoryBuilder.build();
    }

    @Deprecated
    public KeCategoryMessage categoryToMessage(KeCategory.Data data) {
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

    @Deprecated
    public KeCategoryMessage categoryToMessage(KeCategory.Data category, List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
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

    @Deprecated
    private KeCategoryMessage mapChildrenCategory(
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

    private boolean hasChildren(Long categoryId, List<KeGQLResponse.ResponseCategoryWrapper> categoryTree) {
        return categoryTree.stream().anyMatch(it -> it.getCategory().getParent() != null
                && Objects.equals(it.getCategory().getParent().getId(), categoryId));
    }
}
