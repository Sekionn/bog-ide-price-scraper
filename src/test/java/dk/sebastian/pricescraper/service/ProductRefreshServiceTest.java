package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.dto.ProductPriceLookupRequestDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                new TestProductLookupFailureService(),
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
                new TestProductLookupFailureService(),
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

    @Test
    void tracksUnknownProductWithRequestNameWithoutScrapingDuringRequestLookup() {
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
                new TestProductLookupFailureService(),
                properties
        );

        List<ProductPriceDto> result = productRefreshService.findFreshByProductNumberOrEanNumberRequests(List.of(
                new ProductPriceLookupRequestDto("000607", "Some Product", "VARE")
        ));

        assertThat(result).isEmpty();
        assertThat(productPriceService.trackedTitlesByProductNumber).containsExactly(
                Map.entry("000607", "Some Product")
        );
        assertThat(productPriceService.trackedAuthorsByProductNumber).containsEntry("000607", null);
        assertThat(productPriceService.trackedProductTypesByProductNumber).containsExactly(Map.entry("000607", "VARE"));
        assertThat(productPriceService.trackedBookTypesByProductNumber).containsEntry("000607", null);
        assertThat(sitemapService.discoveryRequests).isEmpty();
        assertThat(productPageScraper.scrapedUrls).isEmpty();
    }

    @Test
    void tracksUnknownProductWithoutLookupFailureDuringRequestLookup() {
        ScraperProperties properties = new ScraperProperties();
        TestProductPriceService productPriceService = new TestProductPriceService(properties);
        TestProductPriceCacheService productPriceCacheService = new TestProductPriceCacheService(properties);
        TestProductPageScraperService productPageScraper = new TestProductPageScraperService();
        TestSitemapService sitemapService = new TestSitemapService();
        TestProductLookupFailureService productLookupFailureService = new TestProductLookupFailureService();
        ProductRefreshService productRefreshService = new ProductRefreshService(
                productPriceService,
                productPriceCacheService,
                productPageScraper,
                sitemapService,
                productLookupFailureService,
                properties
        );

        List<ProductPriceDto> result = productRefreshService.findFreshByProductNumberOrEanNumberRequests(List.of(
                new ProductPriceLookupRequestDto("000607", null, "VARE")
        ));

        assertThat(result).isEmpty();
        assertThat(productPageScraper.scrapedUrls).isEmpty();
        assertThat(sitemapService.discoveryRequests).isEmpty();
        assertThat(productLookupFailureService.recordedProductNumbers).isEmpty();
    }

    @Test
    void rejectsLookupRequestsWithUnknownProductType() {
        ProductRefreshService productRefreshService = new ProductRefreshService(
                new TestProductPriceService(new ScraperProperties()),
                new TestProductPriceCacheService(new ScraperProperties()),
                new TestProductPageScraperService(),
                new TestSitemapService(),
                new TestProductLookupFailureService(),
                new ScraperProperties()
        );

        assertThatThrownBy(() -> productRefreshService.findFreshByProductNumberOrEanNumberRequests(List.of(
                new ProductPriceLookupRequestDto("000607", "Some Product", "books")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productType must be either VARE or BOG");
    }

    @Test
    void buildsBookFallbackUrlsForAllKnownBookTypes() {
        ProductRefreshService productRefreshService = new ProductRefreshService(
                new TestProductPriceService(new ScraperProperties()),
                new TestProductPriceCacheService(new ScraperProperties()),
                new TestProductPageScraperService(),
                new TestSitemapService(),
                new TestProductLookupFailureService(),
                new ScraperProperties()
        );

        List<String> fallbackUrls = productRefreshService.fallbackProductUrls(
                new ProductRefreshService.LookupMetadata("Some Book", "Some Author", "BOG"),
                "123456"
        );

        assertThat(fallbackUrls).containsExactly(
                "https://www.bog-ide.dk/products/some-book-some-author-indbundet-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-haeftet-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-hardback-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-paperback-123456"
        );
    }

    @Test
    void usesStoredAuthorWhenTryingBookFallbackUrls() {
        ScraperProperties properties = new ScraperProperties();
        TestProductPriceService productPriceService = new TestProductPriceService(properties);
        TestProductPriceCacheService productPriceCacheService = new TestProductPriceCacheService(properties);
        TestProductPageScraperService productPageScraper = new TestProductPageScraperService();
        productPageScraper.failingUrls = Set.of(
                "https://www.bog-ide.dk/products/some-book-some-author-indbundet-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-haeftet-123456"
        );
        productPriceService.latestKnownEntities = List.of(new ProductPriceEntity(
                "123456",
                null,
                null,
                "Some Book",
                "Some Author",
                "BOG",
                null
        ));
        TestSitemapService sitemapService = new TestSitemapService();
        ProductRefreshService productRefreshService = new ProductRefreshService(
                productPriceService,
                productPriceCacheService,
                productPageScraper,
                sitemapService,
                new TestProductLookupFailureService(),
                properties
        );

        productRefreshService.refreshKnownProductsUntil(Instant.now().plusSeconds(60));

        assertThat(productPageScraper.scrapedUrls).containsExactly(
                "https://www.bog-ide.dk/products/some-book-some-author-indbundet-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-haeftet-123456",
                "https://www.bog-ide.dk/products/some-book-some-author-hardback-123456"
        );
        assertThat(productPriceService.savedProducts).hasSize(1);
    }

    @Test
    void recordsLookupFailureWhenBookAuthorIsMissingFromStoredData() {
        ScraperProperties properties = new ScraperProperties();
        TestProductPriceService productPriceService = new TestProductPriceService(properties);
        TestProductPriceCacheService productPriceCacheService = new TestProductPriceCacheService(properties);
        TestProductPageScraperService productPageScraper = new TestProductPageScraperService();
        TestProductLookupFailureService productLookupFailureService = new TestProductLookupFailureService();
        productPriceService.latestKnownEntities = List.of(new ProductPriceEntity(
                "123456",
                null,
                null,
                "Some Book",
                null,
                "BOG",
                null
        ));
        ProductRefreshService productRefreshService = new ProductRefreshService(
                productPriceService,
                productPriceCacheService,
                productPageScraper,
                new TestSitemapService(),
                productLookupFailureService,
                properties
        );

        productRefreshService.refreshKnownProductsUntil(Instant.now().plusSeconds(60));

        assertThat(productPageScraper.scrapedUrls).isEmpty();
        assertThat(productLookupFailureService.recordedProductNumbers).containsExactly("123456");
        assertThat(productLookupFailureService.recordedAttemptedUrls).containsExactly((String) null);
        assertThat(productLookupFailureService.recordedReasons)
                .containsExactly("Could not build book fallback URL because product author is missing");
    }

    @Test
    void fallbackProductUrlIsOnlyBuiltWhenRequiredFieldsExist() {
        ProductRefreshService productRefreshService = new ProductRefreshService(
                new TestProductPriceService(new ScraperProperties()),
                new TestProductPriceCacheService(new ScraperProperties()),
                new TestProductPageScraperService(),
                new TestSitemapService(),
                new TestProductLookupFailureService(),
                new ScraperProperties()
        );

        assertThat(productRefreshService.fallbackProductUrl((String) null, "000607")).isNull();
        assertThat(productRefreshService.fallbackProductUrl("  ", "000607")).isNull();
        assertThat(productRefreshService.fallbackProductUrl("Some Product", "000607"))
                .isEqualTo("https://www.bog-ide.dk/products/some-product-000607");
        assertThat(productRefreshService.fallbackProductUrl(
                new ProductRefreshService.LookupMetadata("Some Book", null, "BOG"),
                "123456"
        )).isNull();
    }

    private static class TestProductPriceService extends ProductPriceService {

        private final ScraperProperties properties;
        private List<ProductPriceEntity> latestEntities = List.of();
        private List<ProductPriceEntity> latestKnownEntities = List.of();
        private List<ProductPriceDto> latestDtos = List.of();
        private Set<String> recordedStaleRequests = Set.of();
        private List<ProductPrice> savedProducts = List.of();
        private Map<String, String> trackedTitlesByProductNumber = Map.of();
        private Map<String, String> trackedAuthorsByProductNumber = Map.of();
        private Map<String, String> trackedProductTypesByProductNumber = Map.of();
        private Map<String, String> trackedBookTypesByProductNumber = Map.of();

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
            if (entity.getScrapedAt() == null) {
                return false;
            }

            return entity.getScrapedAt().plus(properties.getRefreshAfter()).isAfter(now);
        }

        @Override
        public List<ProductPriceEntity> findLatestKnownProductsOldestFirst() {
            return latestKnownEntities;
        }

        @Override
        public ProductPriceDto save(ProductPrice productPrice) {
            savedProducts = List.of(productPrice);
            return null;
        }

        @Override
        public boolean trackProduct(
                String productNumber,
                String url,
                String eanNumber,
                String title,
                String author,
                String productType,
                String bookType
        ) {
            trackedTitlesByProductNumber = java.util.Collections.singletonMap(productNumber, title);
            trackedAuthorsByProductNumber = java.util.Collections.singletonMap(productNumber, author);
            trackedProductTypesByProductNumber = java.util.Collections.singletonMap(productNumber, productType);
            trackedBookTypesByProductNumber = java.util.Collections.singletonMap(productNumber, bookType);
            return true;
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
        private Set<String> failingUrls = Set.of();

        TestProductPageScraperService() {
            super(null, null);
        }

        @Override
        public ProductPrice scrape(String productUrl) {
            java.util.ArrayList<String> urls = new java.util.ArrayList<>(scrapedUrls);
            urls.add(productUrl);
            scrapedUrls = List.copyOf(urls);
            if (failingUrls.contains(productUrl)) {
                throw new IllegalStateException("Unexpected HTTP 404 while fetching fallback URL");
            }

            return new ProductPrice(
                    productUrl,
                    "000607",
                    null,
                    "Some Product",
                    null,
                    BigDecimal.TEN,
                    "DKK",
                    "InStock",
                    Instant.EPOCH
            );
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

    private static class TestProductLookupFailureService extends ProductLookupFailureService {

        private List<String> recordedProductNumbers = List.of();
        private List<String> recordedAttemptedUrls = List.of();
        private List<String> recordedReasons = List.of();

        TestProductLookupFailureService() {
            super(null, null);
        }

        @Override
        public void recordFailure(String productNumber, String attemptedUrl, String failureReason) {
            recordedProductNumbers = List.of(productNumber);
            recordedAttemptedUrls = java.util.Collections.singletonList(attemptedUrl);
            recordedReasons = List.of(failureReason);
        }
    }
}
