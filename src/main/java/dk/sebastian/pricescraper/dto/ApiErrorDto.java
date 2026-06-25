package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiErrorDto {

    private final String error;
    private final String message;

    @JsonCreator
    public ApiErrorDto(
            @JsonProperty("error") String error,
            @JsonProperty("message") String message
    ) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
