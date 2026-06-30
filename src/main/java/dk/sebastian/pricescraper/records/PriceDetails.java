package dk.sebastian.pricescraper.records;

import java.math.BigDecimal;

public record PriceDetails(
        BigDecimal normalPrice,
        BigDecimal specialOfferPrice,
        String currency,
        String availability
) {
    public PriceDetails(BigDecimal normalPrice, String currency, String availability) {
        this(normalPrice, null, currency, availability);
    }

    public BigDecimal price() {
        return specialOfferPrice != null ? specialOfferPrice : normalPrice;
    }
}
