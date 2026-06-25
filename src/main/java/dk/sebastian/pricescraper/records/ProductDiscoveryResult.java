package dk.sebastian.pricescraper.records;

public record ProductDiscoveryResult(
        int discoveredCount,
        int alreadyKnownCount,
        int invalidUrlCount,
        int failedCount
) {
}
