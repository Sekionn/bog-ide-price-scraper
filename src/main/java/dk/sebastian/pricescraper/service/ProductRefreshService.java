package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.records.ProductPrice;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.records.RefreshResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ProductRefreshService.class);
    private static final Set<Integer> EAN_LENGTHS = Set.of(8, 12, 13, 14);

    private final ProductPriceService productPriceService;
    private final ProductPriceCacheService productPriceCacheService;
    private final ProductPageScraperService productPageScraper;
    private final SitemapService sitemapService;
    private final ScraperProperties properties;

    public ProductRefreshService(
            ProductPriceService productPriceService,
            ProductPriceCacheService productPriceCacheService,
            ProductPageScraperService productPageScraper,
            SitemapService sitemapService,
            ScraperProperties properties
    ) {
        this.productPriceService = productPriceService;
        this.productPriceCacheService = productPriceCacheService;
        this.productPageScraper = productPageScraper;
        this.sitemapService = sitemapService;
        this.properties = properties;
    }

    public List<ProductPriceDto> findFreshByProductNumberOrEanNumber(List<String> identifiers) {
        Instant now = Instant.now();
        List<String> normalizedIdentifiers = productPriceService.normalizeIdentifiers(identifiers);
        Map<String, ProductPriceDto> cachedProductsByIdentifier = productPriceCacheService.findByIdentifiers(normalizedIdentifiers);
        Set<String> uncachedIdentifiers = uncachedIdentifiers(normalizedIdentifiers, cachedProductsByIdentifier);
        if (uncachedIdentifiers.isEmpty()) {
            return productPriceCacheService.orderForIdentifiers(normalizedIdentifiers, cachedProductsByIdentifier, List.of());
        }

        List<ProductPriceEntity> latestMatches = productPriceService.findLatestEntitiesByProductNumberOrEanNumber(List.copyOf(uncachedIdentifiers));
        Set<String> unknownIdentifiers = unknownIdentifiers(uncachedIdentifiers, latestMatches);
        Set<String> staleRequestedProductNumbers = staleRequestedProductNumbers(latestMatches, now);
        productPriceService.recordStaleRequests(staleRequestedProductNumbers, now);

        refreshUnknownProductNumbers(unknownIdentifiers);

        List<ProductPriceDto> databaseProducts = productPriceService.findLatestByProductNumberOrEanNumber(List.copyOf(uncachedIdentifiers));
        productPriceCacheService.writeAll(databaseProducts);
        return productPriceCacheService.orderForIdentifiers(normalizedIdentifiers, cachedProductsByIdentifier, databaseProducts);
    }

    private Set<String> staleRequestedProductNumbers(List<ProductPriceEntity> latestMatches, Instant now) {
        Set<String> productNumbers = new LinkedHashSet<>();
        for (ProductPriceEntity latestMatch : latestMatches) {
            if (!latestMatch.hasScrapedPrice()
                    || (latestMatch.getScrapedAt() != null
                    && !productPriceService.isFresh(latestMatch, now, properties.getRefreshAfter()))) {
                productNumbers.add(latestMatch.getProductNumber());
            }
        }

        return productNumbers;
    }

    public RefreshResult refreshKnownProductsUntil(Instant deadline) {
        int refreshed = 0;
        int skippedFresh = 0;
        int failed = 0;
        Instant now = Instant.now();

        for (ProductPriceEntity latestProduct : productPriceService.findLatestKnownProductsOldestFirst()) {
            if (Instant.now().isAfter(deadline)) {
                break;
            }

            if (productPriceService.isFresh(latestProduct, now, properties.getRefreshAfter())) {
                skippedFresh++;
                continue;
            }

            int productLimit = properties.getMaxProductsPerRun();
            if (productLimit > 0 && refreshed + failed >= productLimit) {
                break;
            }

            try {
                refresh(latestProduct);
                refreshed++;
            } catch (Exception e) {
                failed++;
                log.warn("Could not refresh known product {}", latestProduct.getUrl(), e);
            }
        }

        return new RefreshResult(refreshed, skippedFresh, failed);
    }

    public ProductPriceDto refresh(String productUrl) {
        ProductPrice productPrice = productPageScraper.scrape(productUrl);
        return productPriceService.save(productPrice);
    }

    private void refresh(ProductPriceEntity product) {
        if (product.getUrl() != null && !product.getUrl().isBlank()) {
            refresh(product.getUrl());
            return;
        }

        Map<String, String> discoveredUrlsByProductNumber = sitemapService.findProductUrlsByProductNumbers(List.of(product.getProductNumber()));
        String discoveredUrl = discoveredUrlsByProductNumber.get(product.getProductNumber());
        if (discoveredUrl == null) {
            throw new IllegalStateException("Could not discover product URL for Varenr. " + product.getProductNumber());
        }

        refresh(discoveredUrl);
    }

    private void refreshUnknownProductNumbers(Set<String> unknownIdentifiers) {
        Set<String> unknownProductNumbers = possibleProductNumbers(unknownIdentifiers);
        if (unknownProductNumbers.isEmpty()) {
            return;
        }

        for (String productNumber : unknownProductNumbers) {
            productPriceService.trackProduct(productNumber, null);
        }

        Map<String, String> discoveredUrlsByProductNumber = sitemapService.findProductUrlsByProductNumbers(unknownProductNumbers);
        for (Map.Entry<String, String> discoveredProduct : discoveredUrlsByProductNumber.entrySet()) {
            try {
                refresh(discoveredProduct.getValue());
            } catch (Exception e) {
                log.warn("Could not scrape newly discovered product {} at {}", discoveredProduct.getKey(), discoveredProduct.getValue(), e);
            }
        }
    }

    private static Set<String> unknownIdentifiers(Set<String> identifiers, List<ProductPriceEntity> latestMatches) {
        Set<String> unknownIdentifiers = new LinkedHashSet<>(identifiers);
        for (ProductPriceEntity latestMatch : latestMatches) {
            unknownIdentifiers.remove(latestMatch.getProductNumber());
            unknownIdentifiers.remove(latestMatch.getEanNumber());
        }

        return unknownIdentifiers;
    }

    private static Set<String> uncachedIdentifiers(
            List<String> identifiers,
            Map<String, ProductPriceDto> cachedProductsByIdentifier
    ) {
        Set<String> uncachedIdentifiers = new LinkedHashSet<>(identifiers);
        uncachedIdentifiers.removeAll(cachedProductsByIdentifier.keySet());
        return uncachedIdentifiers;
    }

    static Set<String> possibleProductNumbers(Set<String> unknownIdentifiers) {
        Set<String> productNumbers = new LinkedHashSet<>();
        for (String unknownIdentifier : unknownIdentifiers) {
            if (!isEanLike(unknownIdentifier)) {
                productNumbers.add(unknownIdentifier);
            }
        }

        return productNumbers;
    }

    private static boolean isEanLike(String identifier) {
        return identifier.chars().allMatch(Character::isDigit) && EAN_LENGTHS.contains(identifier.length());
    }
}
