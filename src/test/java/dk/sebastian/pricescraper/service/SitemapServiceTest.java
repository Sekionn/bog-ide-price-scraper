package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import org.junit.jupiter.api.Test;

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
}
