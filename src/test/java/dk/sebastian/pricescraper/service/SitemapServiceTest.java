package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SitemapServiceTest {

    @Test
    void extractsLocValuesFromSitemapXml() {
        SitemapService sitemapService = new SitemapService(null, new ScraperProperties());
        String xml = """
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
                    <url>
                        <loc>https://www.bog-ide.dk/products/example-book-123</loc>
                        <image:image>
                            <image:loc>https://cdn.shopify.com/example.jpg</image:loc>
                            <image:title>Example Book</image:title>
                        </image:image>
                    </url>
                </urlset>
                """;

        List<String> values = sitemapService.extractLocValues(xml);

        assertThat(values).containsExactly("https://www.bog-ide.dk/products/example-book-123");
    }

    @Test
    void onlyDiscoversAllowedProductSitemapsFromSitemapIndex() {
        ScraperProperties properties = new ScraperProperties();
        FakeHttpFetcher httpFetcher = new FakeHttpFetcher(properties, Map.of(
                properties.getRobotsTxtUrl(), """
                        User-agent: *
                        Allow: /
                        Disallow: /cart/
                        """,
                properties.getSitemapIndexUrl(), """
                        <sitemapindex>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_pages_1.xml</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_1.xml?from=1&amp;to=2</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_collections_1.xml</loc></sitemap>
                        </sitemapindex>
                        """
        ));
        SitemapService sitemapService = new SitemapService(
                httpFetcher,
                properties,
                new RobotsService(httpFetcher, properties)
        );

        List<String> productSitemaps = sitemapService.findProductSitemapUrls();

        assertThat(productSitemaps).containsExactly("https://www.bog-ide.dk/sitemap_products_1.xml?from=1&to=2");
    }

    @Test
    void refusesNonProductSitemapUrls() {
        SitemapService sitemapService = new SitemapService(null, new ScraperProperties());

        assertThatThrownBy(() -> sitemapService.findProductUrls("https://www.bog-ide.dk/sitemap_pages_1.xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refusing non-product sitemap URL");
    }

    @Test
    void findsProductUrlsByExactProductNumberSuffix() {
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
                            <url><loc>https://www.bog-ide.dk/products/other-book-912345</loc></url>
                            <url><loc>https://www.bog-ide.dk/products/egholms-gud-2287895</loc></url>
                        </urlset>
                        """
        ));
        SitemapService sitemapService = new SitemapService(httpFetcher, properties);

        Map<String, String> foundUrls = sitemapService.findProductUrlsByProductNumbers(List.of("12345", "2287895"));

        assertThat(foundUrls).containsExactly(
                Map.entry("12345", "https://www.bog-ide.dk/products/some-book-12345"),
                Map.entry("2287895", "https://www.bog-ide.dk/products/egholms-gud-2287895")
        );
    }

    @Test
    void binaryScansProductSitemapsUsingActualProductNumberBounds() {
        ScraperProperties properties = new ScraperProperties();
        String sitemap1 = "https://www.bog-ide.dk/sitemap_products_1.xml?from=1&to=2";
        String sitemap2 = "https://www.bog-ide.dk/sitemap_products_2.xml?from=3&to=4";
        String sitemap3 = "https://www.bog-ide.dk/sitemap_products_3.xml?from=5&to=6";
        String sitemap4 = "https://www.bog-ide.dk/sitemap_products_4.xml?from=7&to=8";
        String sitemap5 = "https://www.bog-ide.dk/sitemap_products_5.xml?from=9&to=10";
        RecordingHttpFetcher httpFetcher = new RecordingHttpFetcher(properties, Map.of(
                properties.getSitemapIndexUrl(), """
                        <sitemapindex>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_1.xml?from=1&amp;to=2</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_2.xml?from=3&amp;to=4</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_3.xml?from=5&amp;to=6</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_4.xml?from=7&amp;to=8</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_5.xml?from=9&amp;to=10</loc></sitemap>
                        </sitemapindex>
                        """,
                sitemap1, productSitemapXml("book-a-100", "book-b-199"),
                sitemap2, productSitemapXml("book-c-200", "book-d-299"),
                sitemap3, productSitemapXml("book-e-300", "book-f-399"),
                sitemap4, productSitemapXml("book-g-400", "book-h-450"),
                sitemap5, productSitemapXml("book-i-500", "book-j-599")
        ));
        SitemapService sitemapService = new SitemapService(httpFetcher, properties);

        Map<String, String> foundUrls = sitemapService.findProductUrlsByProductNumbers(List.of("450"));

        assertThat(foundUrls).containsExactly(
                Map.entry("450", "https://www.bog-ide.dk/products/book-h-450")
        );
        assertThat(httpFetcher.fetchedUrls).containsExactly(
                properties.getSitemapIndexUrl(),
                sitemap3,
                sitemap4
        );
    }

    @Test
    void stopsAfterBinaryScanWhenProductNumberBoundsDoNotFindTheProduct() {
        ScraperProperties properties = new ScraperProperties();
        String sitemap1 = "https://www.bog-ide.dk/sitemap_products_1.xml?from=1&to=2";
        String sitemap2 = "https://www.bog-ide.dk/sitemap_products_2.xml?from=3&to=4";
        String sitemap3 = "https://www.bog-ide.dk/sitemap_products_3.xml?from=5&to=6";
        RecordingHttpFetcher httpFetcher = new RecordingHttpFetcher(properties, Map.of(
                properties.getSitemapIndexUrl(), """
                        <sitemapindex>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_1.xml?from=1&amp;to=2</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_2.xml?from=3&amp;to=4</loc></sitemap>
                            <sitemap><loc>https://www.bog-ide.dk/sitemap_products_3.xml?from=5&amp;to=6</loc></sitemap>
                        </sitemapindex>
                        """,
                sitemap1, productSitemapXml("book-a-100", "book-b-199"),
                sitemap2, productSitemapXml("book-c-200", "book-d-299"),
                sitemap3, productSitemapXml("book-out-of-order-50", "book-z-60")
        ));
        SitemapService sitemapService = new SitemapService(httpFetcher, properties);

        Map<String, String> foundUrls = sitemapService.findProductUrlsByProductNumbers(List.of("50"));

        assertThat(foundUrls).isEmpty();
        assertThat(httpFetcher.fetchedUrls).containsExactly(
                properties.getSitemapIndexUrl(),
                sitemap2,
                sitemap1
        );
    }

    @Test
    void extractsProductNumberFromAllowedProductUrlSuffix() {
        SitemapService sitemapService = new SitemapService(null, new ScraperProperties());

        assertThat(sitemapService.extractProductNumber(
                "https://www.bog-ide.dk/products/egholms-gud-johannes-buchholtz-ebog-2287895"
        )).contains("2287895");
    }

    @Test
    void doesNotExtractProductNumberFromNonProductUrl() {
        SitemapService sitemapService = new SitemapService(null, new ScraperProperties());

        assertThat(sitemapService.extractProductNumber("https://www.bog-ide.dk/pages/egholms-gud-2287895"))
                .isEmpty();
    }

    private static String productSitemapXml(String firstProductPath, String secondProductPath) {
        return """
                <urlset>
                    <url><loc>https://www.bog-ide.dk/products/%s</loc></url>
                    <url><loc>https://www.bog-ide.dk/products/%s</loc></url>
                </urlset>
                """.formatted(firstProductPath, secondProductPath);
    }

    private static class RecordingHttpFetcher extends HttpFetcherService {

        private final Map<String, String> responses;
        private final List<String> fetchedUrls = new ArrayList<>();

        RecordingHttpFetcher(ScraperProperties properties, Map<String, String> responses) {
            super(properties);
            this.responses = responses;
        }

        @Override
        public String fetch(String url) {
            fetchedUrls.add(url);
            String response = responses.get(url);
            if (response == null) {
                throw new IllegalArgumentException("No fake response for " + url);
            }
            return response;
        }
    }
}
