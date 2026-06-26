package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductPriceLookupRequestDto {

    private final String identifier;
    private final String name;
    private final String productType;

    @JsonCreator
    public ProductPriceLookupRequestDto(
            @JsonProperty("identifier") String identifier,
            @JsonProperty("name") String name,
            @JsonProperty("productType") String productType
    ) {
        this.identifier = identifier;
        this.name = name;
        this.productType = productType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getProductType() {
        return productType;
    }
}
