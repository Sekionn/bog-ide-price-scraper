package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.records.ProductDiscoveryResult;
import dk.sebastian.pricescraper.records.ProductDiscoverySummary;
import dk.sebastian.pricescraper.scraper.HttpFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ProductDiscoveryJobService {

    private static final Logger log = LoggerFactory.getLogger(ProductDiscoveryJobService.class);

    private final ProductDiscoveryService productDiscoveryService;
    private final ScraperProperties properties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ProductDiscoverySummary lastSummary;
    private volatile String lastError;
    private volatile String lastFailedUrl;

    public ProductDiscoveryJobService(ProductDiscoveryService productDiscoveryService, ScraperProperties properties) {
        this.productDiscoveryService = productDiscoveryService;
        this.properties = properties;
    }

    @Scheduled(cron = "${scraper.discovery-cron}", zone = "${scraper.discovery-zone}")
    public void runScheduled() {
        runOnce(properties.getMaxDiscoveredProductsPerRun());
    }

    public ProductDiscoverySummary runOnce(Integer requestedLimit) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Product discovery is already running");
        }

        Instant startedAt = Instant.now();
        lastError = null;
        lastFailedUrl = null;

        try {
            Instant deadline = startedAt.plus(properties.getDiscoveryMaxRunTime());
            int productLimit = normalizedLimit(requestedLimit);
            log.info("Starting product sitemap discovery. Limit: {}, deadline: {}", productLimit, deadline);

            ProductDiscoveryResult result = productDiscoveryService.discoverProductsUntil(deadline, productLimit);
            Instant finishedAt = Instant.now();
            ProductDiscoverySummary summary = new ProductDiscoverySummary(
                    startedAt,
                    finishedAt,
                    result.discoveredCount(),
                    result.alreadyKnownCount(),
                    result.invalidUrlCount(),
                    result.failedCount(),
                    !finishedAt.isBefore(deadline)
            );

            lastSummary = summary;
            log.info("Finished product sitemap discovery: {}", summary);
            return summary;
        } catch (RuntimeException e) {
            lastError = e.getMessage();
            if (e instanceof HttpFetchException fetchException) {
                lastFailedUrl = fetchException.getUrl();
            }
            log.error("Product sitemap discovery failed before completion", e);
            throw e;
        } finally {
            running.set(false);
        }
    }

    public boolean isRunning() {
        return running.get();
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

    private int normalizedLimit(Integer requestedLimit) {
        if (requestedLimit != null) {
            if (requestedLimit < 0) {
                throw new IllegalArgumentException("Discovery limit must be 0 or greater");
            }
            return requestedLimit;
        }

        return Math.max(0, properties.getMaxDiscoveredProductsPerRun());
    }
}
