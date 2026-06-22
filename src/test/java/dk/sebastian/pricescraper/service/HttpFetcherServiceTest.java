package dk.sebastian.pricescraper.service;

import com.sun.net.httpserver.HttpServer;
import dk.sebastian.pricescraper.config.ScraperProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class HttpFetcherServiceTest {

    @Test
    void enforcesMinimumDelayBetweenRequestStarts() throws IOException {
        List<Long> requestTimes = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requestTimes.add(System.nanoTime());
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            ScraperProperties properties = new ScraperProperties();
            properties.setRequestDelay(Duration.ZERO);
            properties.setMinimumRequestDelay(Duration.ofMillis(200));

            HttpFetcherService fetcher = new HttpFetcherService(properties);
            String baseUrl = "http://localhost:" + server.getAddress().getPort();

            fetcher.fetch(baseUrl + "/first");
            fetcher.fetch(baseUrl + "/second");

            long elapsedMillis = Duration.ofNanos(requestTimes.get(1) - requestTimes.get(0)).toMillis();
            assertThat(elapsedMillis).isGreaterThanOrEqualTo(150);
        } finally {
            server.stop(0);
        }
    }
}
