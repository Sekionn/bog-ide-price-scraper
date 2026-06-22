package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.scraper.HttpFetchException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class HttpFetcherService {

    private final ScraperProperties properties;
    private final HttpClient httpClient;
    private long lastRequestFinishedAtNanos;

    public HttpFetcherService(ScraperProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public synchronized String fetch(String url) {
        waitForRequestSlot();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(properties.getRequestTimeout())
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 429) {
                throw new HttpFetchException("Rate limited by remote server while fetching " + url, url, statusCode);
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpFetchException("Unexpected HTTP " + statusCode + " while fetching " + url, url, statusCode);
            }

            return response.body();
        } catch (IOException e) {
            throw new HttpFetchException("Could not fetch " + url, url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpFetchException("Interrupted while fetching " + url, url, e);
        } finally {
            lastRequestFinishedAtNanos = System.nanoTime();
        }
    }

    private void waitForRequestSlot() {
        Duration delay = effectiveRequestDelay();
        if (delay.isZero() || lastRequestFinishedAtNanos == 0) {
            return;
        }

        long elapsedNanos = System.nanoTime() - lastRequestFinishedAtNanos;
        long remainingMillis = delay.minusNanos(Math.max(0, elapsedNanos)).toMillis();
        if (remainingMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(remainingMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpFetchException("Interrupted while waiting before next request", "", e);
        }
    }

    private Duration effectiveRequestDelay() {
        Duration configuredDelay = nonNegative(properties.getRequestDelay());
        Duration minimumDelay = nonNegative(properties.getMinimumRequestDelay());
        return configuredDelay.compareTo(minimumDelay) >= 0 ? configuredDelay : minimumDelay;
    }

    private static Duration nonNegative(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }
}
