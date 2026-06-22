package dk.sebastian.pricescraper.records;

public record RefreshResult(int refreshedCount, int skippedFreshCount, int failedCount) {
}
