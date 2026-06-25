package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.sebastian.pricescraper.records.ScrapeSummary;

public class ScrapeStatusDto {

    private final boolean running;
    private final ScrapeSummary lastSummary;
    private final String lastError;
    private final String lastFailedUrl;
    private final Integer lastFailedStatusCode;

    @JsonCreator
    public ScrapeStatusDto(
            @JsonProperty("running") boolean running,
            @JsonProperty("lastSummary") ScrapeSummary lastSummary,
            @JsonProperty("lastError") String lastError,
            @JsonProperty("lastFailedUrl") String lastFailedUrl,
            @JsonProperty("lastFailedStatusCode") Integer lastFailedStatusCode
    ) {
        this.running = running;
        this.lastSummary = lastSummary;
        this.lastError = lastError;
        this.lastFailedUrl = lastFailedUrl;
        this.lastFailedStatusCode = lastFailedStatusCode;
    }

    public boolean isRunning() {
        return running;
    }

    public ScrapeSummary getLastSummary() {
        return lastSummary;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastFailedUrl() {
        return lastFailedUrl;
    }

    public Integer getLastFailedStatusCode() {
        return lastFailedStatusCode;
    }
}
