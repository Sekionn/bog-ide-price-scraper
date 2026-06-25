package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiIndexDto {

    private final String name;
    private final String description;
    private final java.util.List<EndpointDescriptorDto> endpoints;

    @JsonCreator
    public ApiIndexDto(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("endpoints") java.util.List<EndpointDescriptorDto> endpoints
    ) {
        this.name = name;
        this.description = description;
        this.endpoints = endpoints;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public java.util.List<EndpointDescriptorDto> getEndpoints() {
        return endpoints;
    }
}
