package dk.sebastian.pricescraper.records;

public record ScrapeRunResponseDto(
        boolean started,
        String message
) {
}
