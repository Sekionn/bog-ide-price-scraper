package dk.sebastian.pricescraper.records;

import java.math.BigDecimal;

public record PriceDetails(BigDecimal price, String currency, String availability) {
}
