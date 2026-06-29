package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.records.ProductDiscoveryResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDiscoveryServiceTest {

    @Test
    void tracksMissingProductsFromProductSitemapsUntilLimit() {
        ScraperProperties properties = new ScraperProperties();
        String productSitemapUrl = "https://www.bog-ide.dk/sitemap_products_1.xml?from=1&to=3";
        FakeHttpFetcher httpFetcher = new FakeHttpFetcher(properties, Map.of(
                properties.getSitemapIndexUrl(), """
                        <sitemapindex>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_1.xml?from=1&amp;to=3</loc></sitemap>
                        </sitemapindex>
                        """,
                productSitemapUrl, """
                        <urlset>
                            <url><loc>https://www.bog-ide.dk/products/some-book-12345</loc></url>
                            <url><loc>https://www.bog-ide.dk/products/other-book-23456</loc></url>
                            <url><loc>https://www.bog-ide.dk/products/third-book-34567</loc></url>
                        </urlset>
                        """
        ));
        SitemapService sitemapService = new SitemapService(httpFetcher, properties);
        FakeProductPriceService productPriceService = new FakeProductPriceService(Map.of(
                "12345", true,
                "23456", false,
                "34567", true
        ));

        ProductDiscoveryService productDiscoveryService = new ProductDiscoveryService(sitemapService, productPriceService);

        ProductDiscoveryResult result = productDiscoveryService.discoverProductsUntil(
                Instant.now().plusSeconds(60),
                2,
                false
        );

        assertThat(result.discoveredCount()).isEqualTo(2);
        assertThat(result.alreadyKnownCount()).isEqualTo(1);
        assertThat(productPriceService.trackedProducts).containsExactly(
                Map.entry("12345", "https://www.bog-ide.dk/products/some-book-12345"),
                Map.entry("23456", "https://www.bog-ide.dk/products/other-book-23456"),
                Map.entry("34567", "https://www.bog-ide.dk/products/third-book-34567")
        );
    }

    private static class FakeProductPriceService extends ProductPriceService {

        private final Map<String, Boolean> insertedByProductNumber;
        private final Map<String, String> trackedProducts = new LinkedHashMap<>();

        FakeProductPriceService(Map<String, Boolean> insertedByProductNumber) {
            super(null, null);
            this.insertedByProductNumber = insertedByProductNumber;
        }

        @Override
        public boolean trackProduct(
                String productNumber,
                String url,
                String eanNumber,
                boolean overwriteExistingUrl
        ) {
            trackedProducts.put(productNumber, url);
            return insertedByProductNumber.getOrDefault(productNumber, false);
        }
    }
}
