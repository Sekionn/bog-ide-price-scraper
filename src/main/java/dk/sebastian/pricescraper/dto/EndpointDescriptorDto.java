package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class EndpointDescriptorDto {

    private final String rel;
    private final String method;
    private final String description;
    private final Object requestBody;
    private final Object responseBody;
    private final Map<String, Object> constraints;

    @JsonCreator
    public EndpointDescriptorDto(
            @JsonProperty("rel") String rel,
            @JsonProperty("method") String method,
            @JsonProperty("description") String description,
            @JsonProperty("requestBody") Object requestBody,
            @JsonProperty("responseBody") Object responseBody,
            @JsonProperty("constraints") Map<String, Object> constraints
    ) {
        this.rel = rel;
        this.method = method;
        this.description = description;
        this.requestBody = requestBody;
        this.responseBody = responseBody;
        this.constraints = constraints;
    }

    public String getRel() {
        return rel;
    }

    public String getMethod() {
        return method;
    }

    public String getDescription() {
        return description;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public Object getResponseBody() {
        return responseBody;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }
}
