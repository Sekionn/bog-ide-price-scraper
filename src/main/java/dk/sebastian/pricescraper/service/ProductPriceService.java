package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.records.ProductPrice;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.repository.ProductLookupFailureRepository;
import dk.sebastian.pricescraper.repository.ProductPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProductPriceService {

    private static final int MAX_BATCH_SIZE = 100;

    private final ProductPriceRepository productPriceRepository;
    private final ProductLookupFailureRepository productLookupFailureRepository;
    private final ProductPriceCacheService productPriceCacheService;
    private final ScraperProperties properties;

    public ProductPriceService(
            ProductPriceRepository productPriceRepository,
            ProductLookupFailureRepository productLookupFailureRepository,
            ProductPriceCacheService productPriceCacheService,
            ScraperProperties properties
    ) {
        this.productPriceRepository = productPriceRepository;
        this.productLookupFailureRepository = productLookupFailureRepository;
        this.productPriceCacheService = productPriceCacheService;
        this.properties = properties;
    }

    @Transactional
    public ProductPriceDto save(ProductPrice productPrice) {
        Optional<ProductPriceEntity> existingProduct = productPriceRepository.findById(productPrice.productNumber());
        Instant lastRequestedAt = existingProduct.map(ProductPriceEntity::getLastRequestedAt).orElse(null);
        int staleRequestCount = existingProduct.map(ProductPriceEntity::getStaleRequestCount).orElse(0);
        boolean checked = existingProduct.map(ProductPriceEntity::isChecked).orElse(false);
        ProductPriceEntity entity = new ProductPriceEntity(
                productPrice.productNumber(),
                productPrice.url(),
                productPrice.eanNumber(),
                productPrice.title(),
                productPrice.author(),
                productPrice.normalPrice(),
                productPrice.specialOfferPrice(),
                productPrice.currency(),
                productPrice.availability(),
                productPrice.scrapedAt(),
                staleRequestCount,
                lastRequestedAt,
                checked
        );

        ProductPriceDto savedProduct = toDto(productPriceRepository.save(entity));
        clearLookupFailure(productPrice.productNumber());
        writeThroughAfterCommit(savedProduct);
        return savedProduct;
    }

    @Transactional
    public void clearPrices(String productNumber) {
        if (productNumber == null || productNumber.isBlank()) {
            return;
        }

        productPriceRepository.findById(productNumber).ifPresent(product -> {
            product.clearPrices();
            ProductPriceDto clearedProduct = toDto(product);
            evictAfterCommit(clearedProduct);
        });
    }

    @Transactional
    public void trackProduct(String productNumber, String eanNumber) {
        trackProduct(productNumber, null, eanNumber);
    }

    @Transactional
    public boolean trackProduct(String productNumber, String url, String eanNumber) {
        return trackProduct(productNumber, url, eanNumber, false);
    }

    @Transactional
    public boolean trackProduct(
            String productNumber,
            String url,
            String eanNumber,
            boolean overwriteExistingUrl
    ) {
        return trackProduct(productNumber, url, eanNumber, null, null, null, null, overwriteExistingUrl);
    }

    @Transactional
    public boolean trackProduct(String productNumber, String url, String eanNumber, String title) {
        return trackProduct(productNumber, url, eanNumber, title, null, null, null, false);
    }

    @Transactional
    public boolean trackProduct(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            String productType,
            String bookType
    ) {
        return trackProduct(productNumber, url, eanNumber, title, author, productType, bookType, false);
    }

    @Transactional
    public boolean trackProduct(
            String productNumber,
            String url,
            String eanNumber,
            String title,
            String author,
            String productType,
            String bookType,
            boolean overwriteExistingUrl
    ) {
        Optional<ProductPriceEntity> existingProduct = productPriceRepository.findById(productNumber);
        if (existingProduct.isPresent()) {
            ProductPriceEntity product = existingProduct.get();
            if (hasText(url) && (overwriteExistingUrl || !hasText(product.getUrl()))) {
                product.setUrl(url);
            }
            return false;
        }

        productPriceRepository.save(new ProductPriceEntity(productNumber, url, eanNumber, title, author, productType, bookType));
        return true;
    }

    @Transactional
    public void recordStaleRequests(Set<String> productNumbers, Instant requestedAt) {
        if (productNumbers.isEmpty()) {
            return;
        }

        productPriceRepository.recordStaleRequests(productNumbers, requestedAt);
    }

    @Transactional(readOnly = true)
    public List<ProductPriceDto> findLatest() {
        return productPriceRepository.findTop100ByScrapedAtIsNotNullOrderByScrapedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPriceDto> findLatestByProductNumberOrEanNumber(List<String> identifiers) {
        List<String> normalizedIdentifiers = normalizeIdentifiers(identifiers);
        return latestByProduct(productPriceRepository
                .findByProductNumberInOrEanNumberInOrderByBestSnapshotFirst(normalizedIdentifiers, normalizedIdentifiers))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPriceEntity> findLatestEntitiesByProductNumberOrEanNumber(List<String> identifiers) {
        List<String> normalizedIdentifiers = normalizeIdentifiers(identifiers);
        return latestByProduct(productPriceRepository
                .findByProductNumberInOrEanNumberInOrderByBestSnapshotFirst(normalizedIdentifiers, normalizedIdentifiers));
    }

    @Transactional(readOnly = true)
    public List<ProductPriceEntity> findEligibleKnownProductsOldestFirst(Instant refreshBefore, Instant retryFailuresBefore) {
        return productPriceRepository.findAllEligibleKnownProductsByRefreshPriority(refreshBefore, retryFailuresBefore);
    }

    public boolean isFresh(ProductPriceEntity entity, java.time.Instant now, java.time.Duration refreshAfter) {
        if (entity.getScrapedAt() == null) {
            return false;
        }

        return entity.getScrapedAt().plus(refreshAfter).isAfter(now);
    }

    private ProductPriceDto toDto(ProductPriceEntity entity) {
        return new ProductPriceDto(
                entity.getId(),
                entity.getUrl(),
                entity.getProductNumber(),
                entity.getEanNumber(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getPrice(),
                entity.isSpecialOffer(),
                isStalePrice(entity)
        );
    }

    private boolean isStalePrice(ProductPriceEntity entity) {
        return !isFresh(entity, Instant.now(), properties.getRefreshAfter());
    }

    private void writeThroughAfterCommit(ProductPriceDto savedProduct) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productPriceCacheService.write(savedProduct);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productPriceCacheService.write(savedProduct);
            }
        });
    }

    private void evictAfterCommit(ProductPriceDto product) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            productPriceCacheService.evict(product);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                productPriceCacheService.evict(product);
            }
        });
    }

    private void clearLookupFailure(String productNumber) {
        if (productLookupFailureRepository != null && productNumber != null && !productNumber.isBlank()) {
            productLookupFailureRepository.deleteById(productNumber);
        }
    }

    public List<String> normalizeIdentifiers(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("Request must contain at least one product number or EAN number");
        }

        if (identifiers.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch lookup accepts at most " + MAX_BATCH_SIZE + " identifiers");
        }

        List<String> normalized = identifiers.stream()
                .map(identifier -> identifier == null ? "" : identifier.trim())
                .filter(identifier -> !identifier.isBlank())
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Request must contain at least one non-blank product number or EAN number");
        }

        return normalized;
    }

    private List<ProductPriceEntity> latestByProduct(List<ProductPriceEntity> matches) {
        Map<String, ProductPriceEntity> latestByProduct = new LinkedHashMap<>();
        for (ProductPriceEntity match : matches) {
            latestByProduct.putIfAbsent(match.getProductNumber(), match);
        }

        return List.copyOf(latestByProduct.values());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
