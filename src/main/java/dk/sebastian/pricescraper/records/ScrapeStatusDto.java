package dk.sebastian.pricescraper.records;

public record ScrapeStatusDto(
        boolean running,
        ScrapeSummary lastSummary,
        String lastError,
        String lastFailedUrl,
        Integer lastFailedStatusCode
) {
}
