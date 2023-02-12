package dev.crashteam.ke_data_scrapper.model.ke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeCategory {

    private List<Data> payload;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {
        private long id;
        private Long productAmount;
        private String title;
        private String icon;
        private String iconSvg;
        private String iconLink;
        private boolean adult;
        private boolean eco;
        private String seoMetaTag;
        private String seoHeader;
        private List<Data> children;
    }
}
