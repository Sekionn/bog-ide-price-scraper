package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductDiscoveryRunResponseDto {

    private final boolean started;
    private final String message;

    @JsonCreator
    public ProductDiscoveryRunResponseDto(
            @JsonProperty("started") boolean started,
            @JsonProperty("message") String message
    ) {
        this.started = started;
        this.message = message;
    }

    public boolean isStarted() {
        return started;
    }

    public String getMessage() {
        return message;
    }
}
