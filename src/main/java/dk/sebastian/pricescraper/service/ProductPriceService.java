package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.records.ProductPrice;
import dk.sebastian.pricescraper.records.ProductPriceDto;
import dk.sebastian.pricescraper.repository.ProductPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductPriceService {

    private static final int MAX_BATCH_SIZE = 100;

    private final ProductPriceRepository productPriceRepository;
    private final ProductPriceCacheService productPriceCacheService;

    public ProductPriceService(
            ProductPriceRepository productPriceRepository,
            ProductPriceCacheService productPriceCacheService
    ) {
        this.productPriceRepository = productPriceRepository;
        this.productPriceCacheService = productPriceCacheService;
    }

    @Transactional
    public ProductPriceDto save(ProductPrice productPrice) {
        ProductPriceEntity entity = new ProductPriceEntity(
                productPrice.productNumber(),
                productPrice.url(),
                productPrice.eanNumber(),
                productPrice.title(),
                productPrice.price(),
                productPrice.currency(),
                productPrice.availability(),
                productPrice.scrapedAt()
        );

        ProductPriceDto savedProduct = toDto(productPriceRepository.save(entity));
        writeThroughAfterCommit(savedProduct);
        return savedProduct;
    }

    @Transactional
    public void trackProduct(String productNumber, String eanNumber) {
        if (productPriceRepository.existsByProductNumber(productNumber)) {
            return;
        }

        productPriceRepository.save(new ProductPriceEntity(productNumber, eanNumber));
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
    public List<ProductPriceEntity> findLatestKnownProductsOldestFirst() {
        return productPriceRepository.findAllKnownProductsByRefreshPriority();
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
                entity.getPrice()
        );
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
}
