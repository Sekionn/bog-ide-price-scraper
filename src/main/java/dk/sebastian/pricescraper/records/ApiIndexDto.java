package dk.sebastian.pricescraper.records;

import java.util.List;

public record ApiIndexDto(
        String name,
        String description,
        List<EndpointDescriptorDto> endpoints
) {
}
