package dev.crashteam.ke_data_scrapper.model.ke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeCategoryChild {

    private CategoryData payload;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryData {
        private KeCategory.Data category;
    }
}
