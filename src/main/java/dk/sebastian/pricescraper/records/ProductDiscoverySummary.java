package dk.sebastian.pricescraper.records;

import java.time.Instant;

public record ProductDiscoverySummary(
        Instant startedAt,
        Instant finishedAt,
        int discoveredProductCount,
        int alreadyKnownProductCount,
        int invalidUrlCount,
        int failedProductCount,
        boolean timeLimitReached
) {
}
