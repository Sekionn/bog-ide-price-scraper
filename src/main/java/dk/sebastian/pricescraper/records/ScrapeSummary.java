package dk.sebastian.pricescraper.records;

import java.time.Instant;

public record ScrapeSummary(
        Instant startedAt,
        Instant finishedAt,
        int refreshedProductCount,
        int skippedFreshProductCount,
        int failedProductCount,
        boolean timeLimitReached
) {
}
