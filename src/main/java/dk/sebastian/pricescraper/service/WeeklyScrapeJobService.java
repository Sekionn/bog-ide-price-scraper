package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.records.RefreshResult;
import dk.sebastian.pricescraper.records.ScrapeSummary;
import dk.sebastian.pricescraper.scraper.HttpFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WeeklyScrapeJobService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyScrapeJobService.class);

    private final ProductRefreshService productRefreshService;
    private final ScraperProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ScrapeSummary lastSummary;
    private volatile String lastError;
    private volatile String lastFailedUrl;
    private volatile Integer lastFailedStatusCode;

    public WeeklyScrapeJobService(
            ProductRefreshService productRefreshService,
            ScraperProperties properties
    ) {
        this.productRefreshService = productRefreshService;
        this.properties = properties;
    }

    @Scheduled(cron = "${scraper.schedule.cron}", zone = "${scraper.schedule.zone}")
    public void runScheduled() {
        runOnce();
    }

    public ScrapeSummary runOnce() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Scraper is already running");
        }

        Instant startedAt = Instant.now();
        lastError = null;
        lastFailedUrl = null;
        lastFailedStatusCode = null;

        try {
            Instant deadline = startedAt.plus(properties.getMaxRunTime());
            log.info("Starting known-product price refresh. Deadline: {}", deadline);
            RefreshResult result = productRefreshService.refreshKnownProductsUntil(deadline);
            Instant finishedAt = Instant.now();

            ScrapeSummary summary = new ScrapeSummary(
                    startedAt,
                    finishedAt,
                    result.refreshedCount(),
                    result.skippedFreshCount(),
                    result.failedCount(),
                    !finishedAt.isBefore(deadline)
            );

            lastSummary = summary;
            log.info("Finished price scrape: {}", summary);
            return summary;
        } catch (RuntimeException e) {
            lastError = e.getMessage();
            if (e instanceof HttpFetchException fetchException) {
                lastFailedUrl = fetchException.getUrl();
                lastFailedStatusCode = fetchException.getStatusCode() == 0 ? null : fetchException.getStatusCode();
            }
            log.error("Price scrape failed before completion", e);
            throw e;
        } finally {
            running.set(false);
        }
    }

    public boolean isRunning() {
        return running.get();
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
