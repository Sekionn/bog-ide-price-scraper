package dk.sebastian.pricescraper.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRefreshServiceTest {

    @Test
    void excludesEanLikeValuesFromSitemapProductNumberDiscovery() {
        Set<String> unknownIdentifiers = new LinkedHashSet<>(Set.of(
                "2287895",
                "9788711477960",
                "12345678",
                "123456789012",
                "12345678901234"
        ));

        Set<String> productNumbers = ProductRefreshService.possibleProductNumbers(unknownIdentifiers);

        assertThat(productNumbers).containsExactly("2287895");
    }
}
