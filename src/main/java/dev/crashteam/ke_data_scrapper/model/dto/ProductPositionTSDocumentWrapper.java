package dev.crashteam.ke_data_scrapper.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ProductPositionTSDocumentWrapper implements Serializable {

    private List<ProductPositionTSDocument> positionTSDocuments;
}
