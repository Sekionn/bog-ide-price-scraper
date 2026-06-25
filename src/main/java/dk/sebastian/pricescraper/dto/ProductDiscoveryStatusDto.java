package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.sebastian.pricescraper.records.ProductDiscoverySummary;

public class ProductDiscoveryStatusDto {

    private final boolean running;
    private final ProductDiscoverySummary lastSummary;
    private final String lastError;
    private final String lastFailedUrl;

    @JsonCreator
    public ProductDiscoveryStatusDto(
            @JsonProperty("running") boolean running,
            @JsonProperty("lastSummary") ProductDiscoverySummary lastSummary,
            @JsonProperty("lastError") String lastError,
            @JsonProperty("lastFailedUrl") String lastFailedUrl
    ) {
        this.running = running;
        this.lastSummary = lastSummary;
        this.lastError = lastError;
        this.lastFailedUrl = lastFailedUrl;
    }

    public boolean isRunning() {
        return running;
    }

    public ProductDiscoverySummary getLastSummary() {
        return lastSummary;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastFailedUrl() {
        return lastFailedUrl;
    }
}
