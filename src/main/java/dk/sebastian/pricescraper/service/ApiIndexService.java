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
                        lookupFailures(),
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
                linkTo(methodOn(ProductPriceController.class).lookupFailures()).withRel("lookup-failures"),
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
                "Looks up products by Varenr. or EAN. Unknown Varenr. values are tracked for a later refresh job.",
                List.of(Map.of(
                        "identifier", "2287895",
                        "name", "Egholms Gud",
                        "productType", "BOG"
                )),
                List.of(productPriceResponseExample()),
                Map.of(
                        "contentType", "application/json",
                        "maxItems", 100,
                        "itemType", "object",
                        "emptyItems", "ignored after trimming",
                        "freshness", "Matched products older than scraper.refresh-after are prioritized for a later refresh.",
                        "placeholderRows", "Tracked products without a price are not returned.",
                        "unknownProducts", "Unknown product numbers are stored as placeholder rows and are not scraped during the request.",
                        "lookupFailures", "Products that cannot be found after all lookup strategies are listed at /api/prices/lookup-failures.",
                        "productType", "Any value is accepted. BOG gets book fallback URL handling; all other values use normal product fallback."
                )
        );
    }

    private EndpointDescriptorDto lookupFailures() {
        return new EndpointDescriptorDto(
                "lookup-failures",
                "GET",
                "Returns tracked products that could not be found after all lookup strategies, including attempt counts.",
                null,
                List.of(Map.of(
                        "productNumber", "000607",
                        "title", "Some Product",
                        "attemptCount", 2,
                        "lastAttemptedUrl", "https://www.bog-ide.dk/products/some-product-000607",
                        "lastFailureReason", "Unexpected HTTP 404 while fetching fallback URL"
                )),
                Map.of()
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
                        "query", "?limit=10&overwriteExistingUrls=false",
                        "limit", "Optional. Number of new products to add. 0 means no limit.",
                        "overwriteExistingUrls", "Optional. Defaults to false. Set true to replace stored URLs with sitemap URLs.",
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
                "price", "69.95",
                "specialOffer", false
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
