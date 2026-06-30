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
                @Index(name = "idx_product_prices_scraped_at", columnList = "scraped_at"),
                @Index(name = "idx_product_prices_stale_requests", columnList = "stale_request_count"),
                @Index(name = "idx_product_prices_last_requested_at", columnList = "last_requested_at")
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

    @Column(length = 500)
    private String author;

    @Column(name = "product_type", length = 50)
    private String productType;

    @Column(name = "book_type", length = 50)
    private String bookType;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency;

    @Column(length = 100)
    private String availability;

    @Column(name = "scraped_at")
    private Instant scrapedAt;

    @Column(name = "stale_request_count", nullable = false)
    private int staleRequestCount;

    @Column(name = "last_requested_at")
    private Instant lastRequestedAt;

    @Column(nullable = false)
    private boolean checked;

    protected ProductPriceEntity() {
    }

    public ProductPriceEntity(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            BigDecimal price,
            String currency,
            String availability,
            Instant scrapedAt
    ) {
        this(productNumber, url, eanNumber, title, author, price, currency, availability, scrapedAt, 0, null, false);
    }

    public ProductPriceEntity(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            BigDecimal price,
            String currency,
            String availability,
            Instant scrapedAt,
            int staleRequestCount,
            Instant lastRequestedAt
    ) {
        this(
                productNumber,
                url,
                eanNumber,
                title,
                author,
                price,
                currency,
                availability,
                scrapedAt,
                staleRequestCount,
                lastRequestedAt,
                false
        );
    }

    public ProductPriceEntity(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            BigDecimal price,
            String currency,
            String availability,
            Instant scrapedAt,
            int staleRequestCount,
            Instant lastRequestedAt,
            boolean checked
    ) {
        this.productNumber = productNumber;
        this.url = url;
        this.eanNumber = eanNumber;
        this.title = title;
        this.author = author;
        this.price = price;
        this.currency = currency;
        this.availability = availability;
        this.scrapedAt = scrapedAt;
        this.staleRequestCount = staleRequestCount;
        this.lastRequestedAt = lastRequestedAt;
        this.checked = checked;
    }

    public ProductPriceEntity(String productNumber, String eanNumber) {
        this.productNumber = productNumber;
        this.eanNumber = eanNumber;
    }

    public ProductPriceEntity(String productNumber, String url, String eanNumber) {
        this.productNumber = productNumber;
        this.url = url;
        this.eanNumber = eanNumber;
    }

    public ProductPriceEntity(String productNumber, String url, String eanNumber, String title) {
        this(productNumber, url, eanNumber, title, null, null, null);
    }

    public ProductPriceEntity(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            String productType,
            String bookType
    ) {
        this.productNumber = productNumber;
        this.url = url;
        this.eanNumber = eanNumber;
        this.title = title;
        this.author = author;
        this.productType = productType;
        this.bookType = bookType;
    }

    public String getId() {
        return productNumber;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getAuthor() {
        return author;
    }

    public String getProductType() {
        return productType;
    }

    public String getBookType() {
        return bookType;
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

    public int getStaleRequestCount() {
        return staleRequestCount;
    }

    public Instant getLastRequestedAt() {
        return lastRequestedAt;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean hasScrapedPrice() {
        return url != null
                && title != null
                && price != null
                && currency != null
                && scrapedAt != null;
    }
}
