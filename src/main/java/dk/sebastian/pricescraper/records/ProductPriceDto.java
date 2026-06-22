package dk.sebastian.pricescraper.records;

import java.math.BigDecimal;

public record ProductPriceDto(
        String id,
        String url,
        String productNumber,
        String eanNumber,
        String title,
        BigDecimal price
) {
}
