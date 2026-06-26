package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.records.ProductPrice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    @Test
    void recordsStaleDatabaseMatchesWithoutScrapingDuringRequestLookup() {
        ScraperProperties properties = new ScraperProperties();
        TestProductPriceService productPriceService = new TestProductPriceService(properties);
        TestProductPriceCacheService productPriceCacheService = new TestProductPriceCacheService(properties);
        TestProductPageScraperService productPageScraper = new TestProductPageScraperService();
        TestSitemapService sitemapService = new TestSitemapService();
        ProductRefreshService productRefreshService = new ProductRefreshService(
                productPriceService,
                productPriceCacheService,
                productPageScraper,
                sitemapService,
                properties
        );

        ProductPriceEntity staleProduct = new ProductPriceEntity(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "9788711477960",
                "Example",
                "Author",
                BigDecimal.TEN,
                "DKK",
                "InStock",
                Instant.EPOCH
        );
        ProductPriceDto databaseProduct = new ProductPriceDto(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "123",
                "9788711477960",
                "Example",
                "Author",
                BigDecimal.TEN
        );

        List<String> identifiers = List.of("123");
        List<ProductPriceDto> databaseProducts = List.of(databaseProduct);
        productPriceService.latestEntities = List.of(staleProduct);
        productPriceService.latestDtos = databaseProducts;

        List<ProductPriceDto> result = productRefreshService.findFreshByProductNumberOrEanNumber(identifiers);

        assertThat(result).containsExactly(databaseProduct);
        assertThat(productPriceService.recordedStaleRequests).containsExactly("123");
        assertThat(productPriceService.savedProducts).isEmpty();
        assertThat(productPageScraper.scrapedUrls).isEmpty();
        assertThat(sitemapService.discoveryRequests).isEmpty();
    }

    @Test
    void doesNotReturnDatabaseMatchesWithoutAPrice() {
        ScraperProperties properties = new ScraperProperties();
        TestProductPriceService productPriceService = new TestProductPriceService(properties);
        TestProductPriceCacheService productPriceCacheService = new TestProductPriceCacheService(properties);
        TestProductPageScraperService productPageScraper = new TestProductPageScraperService();
        TestSitemapService sitemapService = new TestSitemapService();
        ProductRefreshService productRefreshService = new ProductRefreshService(
                productPriceService,
                productPriceCacheService,
                productPageScraper,
                sitemapService,
                properties
        );

        ProductPriceEntity unpricedProduct = new ProductPriceEntity("456", "9788711477960");
        ProductPriceDto unpricedDatabaseProduct = new ProductPriceDto(
                "456",
                null,
                "456",
                "9788711477960",
                null,
                null,
                null
        );

        List<String> identifiers = List.of("456");
        productPriceService.latestEntities = List.of(unpricedProduct);
        productPriceService.latestDtos = List.of(unpricedDatabaseProduct);

        List<ProductPriceDto> result = productRefreshService.findFreshByProductNumberOrEanNumber(identifiers);

        assertThat(result).isEmpty();
        assertThat(productPriceService.recordedStaleRequests).containsExactly("456");
        assertThat(productPriceService.savedProducts).isEmpty();
        assertThat(productPageScraper.scrapedUrls).isEmpty();
        assertThat(sitemapService.discoveryRequests).isEmpty();
    }

    @Test
    void cacheOrderingSkipsProductsWithoutAPrice() {
        ProductPriceCacheService productPriceCacheService = new ProductPriceCacheService(null, new ScraperProperties());
        ProductPriceDto unpricedCachedProduct = new ProductPriceDto("789", null, "789", null, null, null, null);
        ProductPriceDto unpricedDatabaseProduct = new ProductPriceDto("456", null, "456", null, null, null, null);
        ProductPriceDto pricedProduct = new ProductPriceDto(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "123",
                null,
                "Example",
                null,
                BigDecimal.TEN
        );

        List<ProductPriceDto> result = productPriceCacheService.orderForIdentifiers(
                List.of("789", "456", "123"),
                Map.of("789", unpricedCachedProduct),
                List.of(unpricedDatabaseProduct, pricedProduct)
        );

        assertThat(result).containsExactly(pricedProduct);
    }

    private static class TestProductPriceService extends ProductPriceService {

        private final ScraperProperties properties;
        private List<ProductPriceEntity> latestEntities = List.of();
        private List<ProductPriceDto> latestDtos = List.of();
        private Set<String> recordedStaleRequests = Set.of();
        private List<ProductPrice> savedProducts = List.of();

        TestProductPriceService(ScraperProperties properties) {
            super(null, null);
            this.properties = properties;
        }

        @Override
        public List<String> normalizeIdentifiers(List<String> identifiers) {
            return identifiers;
        }

        @Override
        public List<ProductPriceEntity> findLatestEntitiesByProductNumberOrEanNumber(List<String> identifiers) {
            return latestEntities;
        }

        @Override
        public void recordStaleRequests(Set<String> productNumbers, Instant requestedAt) {
            recordedStaleRequests = productNumbers;
        }

        @Override
        public boolean isFresh(ProductPriceEntity entity, Instant now, java.time.Duration refreshAfter) {
            return entity.getScrapedAt().plus(properties.getRefreshAfter()).isAfter(now);
        }

        @Override
        public ProductPriceDto save(ProductPrice productPrice) {
            savedProducts = List.of(productPrice);
            return null;
        }

        @Override
        public List<ProductPriceDto> findLatestByProductNumberOrEanNumber(List<String> identifiers) {
            return latestDtos;
        }
    }

    private static class TestProductPriceCacheService extends ProductPriceCacheService {

        TestProductPriceCacheService(ScraperProperties properties) {
            super(null, properties);
        }

        @Override
        public Map<String, ProductPriceDto> findByIdentifiers(List<String> identifiers) {
            return Map.of();
        }

        @Override
        public void writeAll(List<ProductPriceDto> productPrices) {
        }

        @Override
        public List<ProductPriceDto> orderForIdentifiers(
                List<String> identifiers,
                Map<String, ProductPriceDto> cachedProductsByIdentifier,
                List<ProductPriceDto> databaseProducts
        ) {
            Map<String, ProductPriceDto> productsByIdentifier = new java.util.LinkedHashMap<>(cachedProductsByIdentifier);
            for (ProductPriceDto product : databaseProducts) {
                productsByIdentifier.putIfAbsent(product.getProductNumber(), product);
                if (product.getEanNumber() != null && !product.getEanNumber().isBlank()) {
                    productsByIdentifier.putIfAbsent(product.getEanNumber(), product);
                }
            }

            return identifiers.stream()
                    .map(productsByIdentifier::get)
                    .filter(product -> product != null && product.getPrice() != null)
                    .distinct()
                    .toList();
        }
    }

    private static class TestProductPageScraperService extends ProductPageScraperService {

        private List<String> scrapedUrls = List.of();

        TestProductPageScraperService() {
            super(null, null);
        }

        @Override
        public ProductPrice scrape(String productUrl) {
            scrapedUrls = List.of(productUrl);
            return null;
        }
    }

    private static class TestSitemapService extends SitemapService {

        private Set<String> discoveryRequests = Set.of();

        TestSitemapService() {
            super(null, new ScraperProperties());
        }

        @Override
        public Map<String, String> findProductUrlsByProductNumbers(java.util.Collection<String> productNumbers) {
            discoveryRequests = new LinkedHashSet<>(productNumbers);
            return Map.of();
        }
    }
}
