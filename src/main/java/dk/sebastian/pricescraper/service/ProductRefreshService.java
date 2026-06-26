package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.records.ProductPrice;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.dto.ProductPriceLookupRequestDto;
import dk.sebastian.pricescraper.records.RefreshResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProductRefreshService {

    private static final Logger log = LoggerFactory.getLogger(ProductRefreshService.class);
    private static final Set<Integer> EAN_LENGTHS = Set.of(8, 12, 13, 14);
    private static final String BOOK_PRODUCT_TYPE = "BOG";
    private static final String GENERAL_PRODUCT_TYPE = "VARE";
    private static final List<String> BOOK_TYPES = List.of("indbundet", "haeftet", "hardback", "paperback");

    private final ProductPriceService productPriceService;
    private final ProductPriceCacheService productPriceCacheService;
    private final ProductPageScraperService productPageScraper;
    private final SitemapService sitemapService;
    private final ProductLookupFailureService productLookupFailureService;
    private final ScraperProperties properties;

    public ProductRefreshService(
            ProductPriceService productPriceService,
            ProductPriceCacheService productPriceCacheService,
            ProductPageScraperService productPageScraper,
            SitemapService sitemapService,
            ProductLookupFailureService productLookupFailureService,
            ScraperProperties properties
    ) {
        this.productPriceService = productPriceService;
        this.productPriceCacheService = productPriceCacheService;
        this.productPageScraper = productPageScraper;
        this.sitemapService = sitemapService;
        this.productLookupFailureService = productLookupFailureService;
        this.properties = properties;
    }

    public List<ProductPriceDto> findFreshByProductNumberOrEanNumberRequests(List<ProductPriceLookupRequestDto> requests) {
        if (requests == null) {
            return findFreshByProductNumberOrEanNumber(List.of());
        }

        List<String> identifiers = requests.stream()
                .map(request -> request == null ? null : request.getIdentifier())
                .toList();
        Map<String, LookupMetadata> metadataByIdentifier = metadataByIdentifier(requests);
        return findFreshByProductNumberOrEanNumber(identifiers, metadataByIdentifier);
    }

    public List<ProductPriceDto> findFreshByProductNumberOrEanNumber(List<String> identifiers) {
        return findFreshByProductNumberOrEanNumber(identifiers, Map.of());
    }

    private List<ProductPriceDto> findFreshByProductNumberOrEanNumber(
            List<String> identifiers,
            Map<String, LookupMetadata> metadataByIdentifier
    ) {
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

        trackUnknownProductNumbers(unknownIdentifiers, metadataByIdentifier);

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

        String sitemapFailureReason = null;
        Map<String, String> discoveredUrlsByProductNumber = Map.of();
        try {
            discoveredUrlsByProductNumber = sitemapService.findProductUrlsByProductNumbers(List.of(product.getProductNumber()));
        } catch (Exception e) {
            sitemapFailureReason = e.getMessage();
            log.warn("Could not discover product URL for {} from sitemaps", product.getProductNumber(), e);
        }
        String discoveredUrl = discoveredUrlsByProductNumber.get(product.getProductNumber());
        if (discoveredUrl == null) {
            LookupMetadata metadata = new LookupMetadata(product.getTitle(), product.getAuthor(), product.getProductType());
            List<String> fallbackUrls = fallbackProductUrls(metadata, product.getProductNumber());
            if (!fallbackUrls.isEmpty()) {
                refreshFallbackProduct(product.getProductNumber(), fallbackUrls);
                return;
            }

            recordLookupFailure(
                    product.getProductNumber(),
                    null,
                    fallbackLookupFailureReason(metadata, sitemapFailureReason)
            );
            return;
        }

        refresh(discoveredUrl);
    }

    private void trackUnknownProductNumbers(Set<String> unknownIdentifiers, Map<String, LookupMetadata> metadataByIdentifier) {
        Set<String> unknownProductNumbers = possibleProductNumbers(unknownIdentifiers);
        if (unknownProductNumbers.isEmpty()) {
            return;
        }

        for (String productNumber : unknownProductNumbers) {
            LookupMetadata metadata = metadataByIdentifier.get(productNumber);
            productPriceService.trackProduct(
                    productNumber,
                    null,
                    null,
                    metadata == null ? null : metadata.name(),
                    null,
                    metadata == null ? null : metadata.productType(),
                    null
            );
        }
    }

    private void refreshFallbackProduct(String productNumber, List<String> fallbackUrls) {
        String lastAttemptedUrl = null;
        Exception lastFailure = null;
        for (String fallbackUrl : fallbackUrls) {
            lastAttemptedUrl = fallbackUrl;
            try {
                refresh(fallbackUrl);
                return;
            } catch (Exception e) {
                lastFailure = e;
                log.info("Could not scrape fallback product {} at {}: {}", productNumber, fallbackUrl, e.getMessage());
            }
        }

        if (lastFailure == null) {
            recordLookupFailure(productNumber, lastAttemptedUrl, "Fallback URL list was empty");
        } else {
            recordLookupFailure(productNumber, lastAttemptedUrl, lastFailure);
        }
    }

    private void recordLookupFailure(String productNumber, String attemptedUrl, Exception exception) {
        recordLookupFailure(productNumber, attemptedUrl, exception.getMessage());
    }

    private void recordLookupFailure(String productNumber, String attemptedUrl, String reason) {
        productLookupFailureService.recordFailure(productNumber, attemptedUrl, reason);
    }

    private static String finalLookupFailureReason(String sitemapFailureReason) {
        if (hasText(sitemapFailureReason)) {
            return "Could not discover product URL from sitemap and could not build fallback URL: " + sitemapFailureReason;
        }

        return "Could not discover product URL or build fallback URL";
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

    private static Map<String, LookupMetadata> metadataByIdentifier(List<ProductPriceLookupRequestDto> requests) {
        Map<String, LookupMetadata> metadataByIdentifier = new LinkedHashMap<>();
        for (ProductPriceLookupRequestDto request : requests) {
            if (request == null || !hasText(request.getIdentifier()) || !hasText(request.getName())) {
                continue;
            }

            metadataByIdentifier.putIfAbsent(request.getIdentifier().trim(), new LookupMetadata(
                    clean(request.getName()),
                    null,
                    normalizeRequestProductType(request.getProductType())
            ));
        }

        return metadataByIdentifier;
    }

    String fallbackProductUrl(String title, String productNumber) {
        return fallbackProductUrl(new LookupMetadata(title, null, null), productNumber);
    }

    String fallbackProductUrl(LookupMetadata metadata, String productNumber) {
        List<String> fallbackUrls = fallbackProductUrls(metadata, productNumber);
        return fallbackUrls.isEmpty() ? null : fallbackUrls.get(0);
    }

    List<String> fallbackProductUrls(LookupMetadata metadata, String productNumber) {
        if (metadata == null || !hasText(metadata.name()) || !hasText(productNumber)) {
            return List.of();
        }

        String baseProductUrl = properties.getAllowedProductUrlPrefix();
        if (!baseProductUrl.endsWith("/")) {
            baseProductUrl += "/";
        }

        List<String> productSlugs = fallbackProductSlugs(metadata);
        if (productSlugs.isEmpty()) {
            return List.of();
        }

        String finalBaseProductUrl = baseProductUrl;
        return productSlugs.stream()
                .map(productSlug -> finalBaseProductUrl + productSlug + "-" + productNumber.trim())
                .toList();
    }

    private static List<String> fallbackProductSlugs(LookupMetadata metadata) {
        if (!isBook(metadata.productType())) {
            String productSlug = slug(metadata.name());
            return hasText(productSlug) ? List.of(productSlug) : List.of();
        }

        String titleSlug = slug(metadata.name());
        String authorSlug = slug(metadata.author());
        if (!hasText(titleSlug) || !hasText(authorSlug)) {
            return List.of();
        }

        return BOOK_TYPES.stream()
                .map(bookType -> titleSlug + "-" + authorSlug + "-" + bookType)
                .toList();
    }

    private static boolean isBook(String productType) {
        return hasText(productType) && BOOK_PRODUCT_TYPE.equals(productType.trim().toUpperCase(Locale.ROOT));
    }

    private static String fallbackLookupFailureReason(LookupMetadata metadata, String sitemapFailureReason) {
        if (metadata != null && isBook(metadata.productType()) && !hasText(metadata.author())) {
            return "Could not build book fallback URL because product author is missing";
        }

        return finalLookupFailureReason(sitemapFailureReason);
    }

    private static String slug(String title) {
        if (!hasText(title)) {
            return "";
        }

        String value = title.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\u00e6", "ae")
                .replace("\u00f8", "oe")
                .replace("\u00e5", "aa");
        value = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return value;
    }

    private static boolean isEanLike(String identifier) {
        return identifier.chars().allMatch(Character::isDigit) && EAN_LENGTHS.contains(identifier.length());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String normalizeRequestProductType(String productType) {
        if (!hasText(productType)) {
            throw new IllegalArgumentException("productType must be either VARE or BOG");
        }

        String normalizedProductType = productType.trim().toUpperCase(Locale.ROOT);
        if (BOOK_PRODUCT_TYPE.equals(normalizedProductType) || GENERAL_PRODUCT_TYPE.equals(normalizedProductType)) {
            return normalizedProductType;
        }

        throw new IllegalArgumentException("productType must be either VARE or BOG");
    }

    record LookupMetadata(String name, String author, String productType) {
    }
}
