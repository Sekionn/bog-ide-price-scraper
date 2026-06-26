package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.dto.ProductLookupFailureDto;
import dk.sebastian.pricescraper.entity.ProductLookupFailureEntity;
import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.repository.ProductLookupFailureRepository;
import dk.sebastian.pricescraper.repository.ProductPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductLookupFailureService {

    private final ProductLookupFailureRepository productLookupFailureRepository;
    private final ProductPriceRepository productPriceRepository;

    public ProductLookupFailureService(
            ProductLookupFailureRepository productLookupFailureRepository,
            ProductPriceRepository productPriceRepository
    ) {
        this.productLookupFailureRepository = productLookupFailureRepository;
        this.productPriceRepository = productPriceRepository;
    }

    @Transactional
    public void recordFailure(String productNumber, String attemptedUrl, String failureReason) {
        if (productNumber == null || productNumber.isBlank()) {
            return;
        }

        if (!productPriceRepository.existsById(productNumber)) {
            return;
        }

        Instant now = Instant.now();
        ProductLookupFailureEntity failure = productLookupFailureRepository.findById(productNumber).orElse(null);
        if (failure == null) {
            failure = new ProductLookupFailureEntity(productNumber, now, attemptedUrl, truncate(failureReason));
        } else {
            failure.recordAttempt(now, attemptedUrl, truncate(failureReason));
        }

        productLookupFailureRepository.save(failure);
    }

    @Transactional(readOnly = true)
    public List<ProductLookupFailureDto> findAll() {
        List<ProductLookupFailureEntity> failures = productLookupFailureRepository.findAllByOrderByLastFailedAtDesc();
        Map<String, ProductPriceEntity> productsByProductNumber = productPriceRepository.findAllById(
                        failures.stream().map(ProductLookupFailureEntity::getProductNumber).toList()
                )
                .stream()
                .collect(Collectors.toMap(ProductPriceEntity::getProductNumber, Function.identity()));

        return failures.stream()
                .map(failure -> toDto(failure, productsByProductNumber.get(failure.getProductNumber())))
                .toList();
    }

    private ProductLookupFailureDto toDto(ProductLookupFailureEntity failure, ProductPriceEntity product) {
        return new ProductLookupFailureDto(
                failure.getProductNumber(),
                product == null ? null : product.getEanNumber(),
                product == null ? null : product.getTitle(),
                product == null ? null : product.getAuthor(),
                product == null ? null : product.getProductType(),
                product == null ? null : product.getBookType(),
                failure.getAttemptCount(),
                failure.getFirstFailedAt(),
                failure.getLastFailedAt(),
                failure.getLastAttemptedUrl(),
                failure.getLastFailureReason()
        );
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }

        return value.substring(0, 1000);
    }
}
