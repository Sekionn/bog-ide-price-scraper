package dk.sebastian.pricescraper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "product_prices",
        indexes = {
                @Index(name = "idx_product_prices_url", columnList = "url"),
                @Index(name = "idx_product_prices_ean_number", columnList = "ean_number"),
                @Index(name = "idx_product_prices_scraped_at", columnList = "scraped_at")
        }
)
public class ProductPriceEntity {

    @Id
    @Column(name = "product_number", nullable = false, length = 255)
    private String productNumber;

    @Column(length = 768)
    private String url;

    @Column(name = "ean_number", length = 255)
    private String eanNumber;

    @Column(length = 500)
    private String title;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency;

    @Column(length = 100)
    private String availability;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    protected ProductPriceEntity() {
    }

    public ProductPriceEntity(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            BigDecimal price,
            String currency,
            String availability,
            Instant scrapedAt
    ) {
        this.productNumber = productNumber;
        this.url = url;
        this.eanNumber = eanNumber;
        this.title = title;
        this.price = price;
        this.currency = currency;
        this.availability = availability;
        this.scrapedAt = scrapedAt;
    }

    public ProductPriceEntity(String productNumber, String eanNumber) {
        this.productNumber = productNumber;
        this.eanNumber = eanNumber;
    }

    public String getId() {
        return productNumber;
    }

    public String getUrl() {
        return url;
    }

    public String getProductNumber() {
        return productNumber;
    }

    public String getEanNumber() {
        return eanNumber;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAvailability() {
        return availability;
    }

    public Instant getScrapedAt() {
        return scrapedAt;
    }

    public boolean hasScrapedPrice() {
        return url != null
                && title != null
                && price != null
                && currency != null
                && scrapedAt != null;
    }
}
