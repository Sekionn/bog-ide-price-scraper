package dk.sebastian.pricescraper.records;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductPrice(
        String url,
        String productNumber,
        String eanNumber,
        String title,
        String author,
        BigDecimal price,
        String currency,
        String availability,
        Instant scrapedAt
) {
}
