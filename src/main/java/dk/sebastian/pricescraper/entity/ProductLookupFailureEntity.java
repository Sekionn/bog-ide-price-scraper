package dk.sebastian.pricescraper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "product_lookup_failures",
        indexes = {
                @Index(name = "idx_product_lookup_failures_last_failed_at", columnList = "last_failed_at"),
                @Index(name = "idx_product_lookup_failures_attempt_count", columnList = "attempt_count")
        }
)
public class ProductLookupFailureEntity {

    @Id
    @Column(name = "product_number", nullable = false, length = 255)
    private String productNumber;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "first_failed_at", nullable = false)
    private Instant firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    @Column(name = "last_attempted_url", length = 768)
    private String lastAttemptedUrl;

    @Column(name = "last_failure_reason", length = 1000)
    private String lastFailureReason;

    protected ProductLookupFailureEntity() {
    }

    public ProductLookupFailureEntity(
            String productNumber,
            Instant failedAt,
            String attemptedUrl,
            String failureReason
    ) {
        this.productNumber = productNumber;
        this.attemptCount = 1;
        this.firstFailedAt = failedAt;
        this.lastFailedAt = failedAt;
        this.lastAttemptedUrl = attemptedUrl;
        this.lastFailureReason = failureReason;
    }

    public void recordAttempt(Instant failedAt, String attemptedUrl, String failureReason) {
        this.attemptCount++;
        this.lastFailedAt = failedAt;
        this.lastAttemptedUrl = attemptedUrl;
        this.lastFailureReason = failureReason;
    }

    public String getProductNumber() {
        return productNumber;
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
