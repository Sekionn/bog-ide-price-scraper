package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class ProductLookupFailureDto {

    private final String productNumber;
    private final String eanNumber;
    private final String title;
    private final String author;
    private final String productType;
    private final String bookType;
    private final int attemptCount;
    private final Instant firstFailedAt;
    private final Instant lastFailedAt;
    private final String lastAttemptedUrl;
    private final String lastFailureReason;

    @JsonCreator
    public ProductLookupFailureDto(
            @JsonProperty("productNumber") String productNumber,
            @JsonProperty("eanNumber") String eanNumber,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("productType") String productType,
            @JsonProperty("bookType") String bookType,
            @JsonProperty("attemptCount") int attemptCount,
            @JsonProperty("firstFailedAt") Instant firstFailedAt,
            @JsonProperty("lastFailedAt") Instant lastFailedAt,
            @JsonProperty("lastAttemptedUrl") String lastAttemptedUrl,
            @JsonProperty("lastFailureReason") String lastFailureReason
    ) {
        this.productNumber = productNumber;
        this.eanNumber = eanNumber;
        this.title = title;
        this.author = author;
        this.productType = productType;
        this.bookType = bookType;
        this.attemptCount = attemptCount;
        this.firstFailedAt = firstFailedAt;
        this.lastFailedAt = lastFailedAt;
        this.lastAttemptedUrl = lastAttemptedUrl;
        this.lastFailureReason = lastFailureReason;
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

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getFirstFailedAt() {
        return firstFailedAt;
    }

    public Instant getLastFailedAt() {
        return lastFailedAt;
    }

    public String getLastAttemptedUrl() {
        return lastAttemptedUrl;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }
}
