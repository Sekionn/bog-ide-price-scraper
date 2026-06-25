package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.controller.ApiIndexController;
import dk.sebastian.pricescraper.controller.ProductPriceController;
import dk.sebastian.pricescraper.controller.ProductDiscoveryController;
import dk.sebastian.pricescraper.controller.ScrapeController;
import dk.sebastian.pricescraper.dto.ApiIndexDto;
import dk.sebastian.pricescraper.dto.EndpointDescriptorDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
public class ApiIndexService {

    public EntityModel<ApiIndexDto> index() {
        ApiIndexDto apiIndex = new ApiIndexDto(
                "Bog & ide price scraper API",
                "Discover available API endpoints, methods, request bodies, response bodies, and constraints.",
                List.of(
                        latestPrices(),
                        batchPrices(),
                        runDiscovery(),
                        discoveryStatus(),
                        runScrape(),
                        scrapeStatus()
                )
        );

        return EntityModel.of(apiIndex,
                linkTo(methodOn(ApiIndexController.class).index()).withSelfRel(),
                linkTo(methodOn(ProductPriceController.class).latest()).withRel("latest-prices"),
                Link.of("/api/prices/batch").withRel("batch-prices"),
                Link.of("/api/discovery/run").withRel("run-product-discovery"),
                linkTo(methodOn(ProductDiscoveryController.class).status()).withRel("product-discovery-status"),
                linkTo(methodOn(ScrapeController.class).runNow()).withRel("run-refresh"),
                linkTo(methodOn(ScrapeController.class).status()).withRel("refresh-status")
        );
    }

    private EndpointDescriptorDto latestPrices() {
        return new EndpointDescriptorDto(
                "latest-prices",
                "GET",
                "Returns up to 100 latest stored product price rows.",
                null,
                List.of(productPriceResponseExample()),
                Map.of()
        );
    }

    private EndpointDescriptorDto batchPrices() {
        return new EndpointDescriptorDto(
                "batch-prices",
                "POST",
                "Looks up products by Varenr. or EAN. Unknown Varenr. values may be discovered through product sitemaps; EAN values are never used for sitemap URL discovery.",
                List.of("2287895", "9788711477960"),
                List.of(productPriceResponseExample()),
                Map.of(
                        "contentType", "application/json",
                        "maxItems", 100,
                        "itemType", "string",
                        "emptyItems", "ignored after trimming",
                        "freshness", "Matched products older than scraper.refresh-after are refreshed before returning.",
                        "placeholderRows", "Tracked products that have not been scraped yet are returned with null price fields."
                )
        );
    }

    private EndpointDescriptorDto runScrape() {
        return new EndpointDescriptorDto(
                "run-refresh",
                "POST",
                "Starts a background refresh job for known database products.",
                null,
                Map.of(
                        "started", true,
                        "message", "Scrape started. Check /api/scrape/status for progress."
                ),
                Map.of(
                        "conflictStatus", 409,
                        "conflictReason", "Returned when a refresh job is already running."
                )
        );
    }

    private EndpointDescriptorDto runDiscovery() {
        return new EndpointDescriptorDto(
                "run-product-discovery",
                "POST",
                "Starts a background product discovery job that scans product sitemaps and stores missing product numbers and URLs.",
                null,
                Map.of(
                        "started", true,
                        "message", "Product discovery started. Check /api/discovery/status for progress."
                ),
                Map.of(
                        "query", "?limit=10",
                        "limit", "Optional. Number of new products to add. 0 means no limit.",
                        "maxRunTime", "Stops after scraper.discovery-max-run-time.",
                        "conflictStatus", 409,
                        "conflictReason", "Returned when product discovery is already running."
                )
        );
    }

    private EndpointDescriptorDto discoveryStatus() {
        return new EndpointDescriptorDto(
                "product-discovery-status",
                "GET",
                "Returns current product discovery status and the last discovery summary.",
                null,
                productDiscoveryStatusResponseExample(),
                Map.of()
        );
    }

    private EndpointDescriptorDto scrapeStatus() {
        return new EndpointDescriptorDto(
                "refresh-status",
                "GET",
                "Returns current refresh status, last summary, and last top-level failure details.",
                null,
                scrapeStatusResponseExample(),
                Map.of()
        );
    }

    private Map<String, Object> productPriceResponseExample() {
        return Map.of(
                "id", "2287895",
                "url", "https://www.bog-ide.dk/products/egholms-gud-johannes-buchholtz-ebog-2287895",
                "productNumber", "2287895",
                "eanNumber", "9788711477960",
                "title", "Egholms Gud",
                "author", "Johannes Buchholtz",
                "price", "69.95"
        );
    }

    private Map<String, Object> scrapeStatusResponseExample() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("running", false);
        response.put("lastSummary", Map.of(
                "startedAt", "2026-06-21T20:00:00Z",
                "finishedAt", "2026-06-21T20:10:00Z",
                "refreshedProductCount", 10,
                "skippedFreshProductCount", 25,
                "failedProductCount", 0,
                "timeLimitReached", false
        ));
        response.put("lastError", null);
        response.put("lastFailedUrl", null);
        response.put("lastFailedStatusCode", null);
        return response;
    }

    private Map<String, Object> productDiscoveryStatusResponseExample() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("running", false);
        response.put("lastSummary", Map.of(
                "startedAt", "2026-06-23T01:00:00Z",
                "finishedAt", "2026-06-23T01:20:00Z",
                "discoveredProductCount", 10,
                "alreadyKnownProductCount", 125,
                "invalidUrlCount", 0,
                "failedProductCount", 0,
                "timeLimitReached", false
        ));
        response.put("lastError", null);
        response.put("lastFailedUrl", null);
        return response;
    }
}
