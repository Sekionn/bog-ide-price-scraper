package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.records.RobotsRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsServiceTest {

    @Test
    void allowsPublicProductPagesAndBlocksDisallowedPaths() {
        RobotsRules rules = RobotsService.parse("""
                User-agent: *
                Allow: /
                Disallow: /cart/
                Disallow: /checkout
                """, "BogIdePriceScraper");

        assertThat(rules.isAllowed("/products/example-book-123")).isTrue();
        assertThat(rules.isAllowed("/sitemap_products_1.xml?from=1&to=2")).isTrue();
        assertThat(rules.isAllowed("/cart/add")).isFalse();
        assertThat(rules.isAllowed("/checkout")).isFalse();
    }

    @Test
    void prefersLongerAllowRuleWhenRulesOverlap() {
        RobotsRules rules = RobotsService.parse("""
                User-agent: *
                Disallow: /products/
                Allow: /products/public-book
                """, "BogIdePriceScraper");

        assertThat(rules.isAllowed("/products/public-book")).isTrue();
        assertThat(rules.isAllowed("/products/hidden-book")).isFalse();
    }
}
