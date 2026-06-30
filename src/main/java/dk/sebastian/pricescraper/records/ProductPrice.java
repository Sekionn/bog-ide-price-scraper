package dk.sebastian.pricescraper.records;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductPrice(
        String url,
        String productNumber,
        String eanNumber,
        String title,
        String author,
        BigDecimal normalPrice,
        BigDecimal specialOfferPrice,
        String currency,
        String availability,
        Instant scrapedAt
) {
    public ProductPrice(
            String url,
            String productNumber,
            String eanNumber,
            String title,
            String author,
            BigDecimal normalPrice,
            String currency,
            String availability,
            Instant scrapedAt
    ) {
        this(url, productNumber, eanNumber, title, author, normalPrice, null, currency, availability, scrapedAt);
    }

    public BigDecimal price() {
        return specialOfferPrice != null ? specialOfferPrice : normalPrice;
    }
}
