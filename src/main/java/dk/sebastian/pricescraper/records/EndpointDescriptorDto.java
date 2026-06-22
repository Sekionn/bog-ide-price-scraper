package dk.sebastian.pricescraper.records;

import java.util.Map;

public record EndpointDescriptorDto(
        String rel,
        String method,
        String description,
        Object requestBody,
        Object responseBody,
        Map<String, Object> constraints
) {
}
