package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductPriceCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductPriceCacheService.class);
    private static final String PRODUCT_NUMBER_KEY_PART = "product-number";
    private static final String EAN_NUMBER_KEY_PART = "ean";
    private static final String CACHE_SCHEMA_VERSION = "v3";

    private final RedisTemplate<String, ProductPriceDto> redisTemplate;
    private final ScraperProperties properties;
    private boolean redisReadFailureLogged;
    private boolean redisWriteFailureLogged;

    public ProductPriceCacheService(
            RedisTemplate<String, ProductPriceDto> redisTemplate,
            ScraperProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public Optional<ProductPriceDto> findByIdentifier(String identifier) {
        try {
            ProductPriceDto product = redisTemplate.opsForValue().get(productNumberKey(identifier));
            if (hasPrice(product)) {
                return Optional.of(product);
            }

            return Optional.ofNullable(redisTemplate.opsForValue().get(eanNumberKey(identifier)))
                    .filter(ProductPriceCacheService::hasPrice);
        } catch (RedisConnectionFailureException e) {
            logRedisReadFailure(identifier, e);
            return Optional.empty();
        }
    }

    public Map<String, ProductPriceDto> findByIdentifiers(List<String> identifiers) {
        Map<String, ProductPriceDto> productsByIdentifier = new LinkedHashMap<>();
        for (String identifier : identifiers) {
            findByIdentifier(identifier).ifPresent(product -> productsByIdentifier.put(identifier, product));
        }

        return productsByIdentifier;
    }

    public void write(ProductPriceDto productPrice) {
        writeOne(productPrice);
    }

    public void evict(ProductPriceDto productPrice) {
        if (productPrice == null) {
            return;
        }

        try {
            if (hasText(productPrice.getProductNumber())) {
                redisTemplate.delete(productNumberKey(productPrice.getProductNumber()));
            }
            if (hasText(productPrice.getEanNumber())) {
                redisTemplate.delete(eanNumberKey(productPrice.getEanNumber()));
            }
        } catch (RedisConnectionFailureException ignored) {
            logRedisWriteFailure(productPrice, ignored);
        }
    }

    public void writeAll(List<ProductPriceDto> productPrices) {
        int writtenKeys = 0;
        int skippedWithoutPrice = 0;
        for (ProductPriceDto productPrice : productPrices) {
            WriteResult result = writeOne(productPrice);
            writtenKeys += result.writtenKeys();
            if (result.skippedWithoutPrice()) {
                skippedWithoutPrice++;
            }
        }

        if (!productPrices.isEmpty()) {
            log.info(
                    "Redis cache write completed: {} key(s) written for {} product(s), {} product(s) skipped without a price.",
                    writtenKeys,
                    productPrices.size(),
                    skippedWithoutPrice
            );
        }
    }

    private WriteResult writeOne(ProductPriceDto productPrice) {
        if (!hasPrice(productPrice)) {
            return WriteResult.skipped();
        }

        Duration ttl = properties.getRefreshAfter();
        int writtenKeys = 0;
        try {
            if (hasText(productPrice.getProductNumber())) {
                redisTemplate.opsForValue().set(productNumberKey(productPrice.getProductNumber()), productPrice, ttl);
                writtenKeys++;
            }
            if (hasText(productPrice.getEanNumber())) {
                redisTemplate.opsForValue().set(eanNumberKey(productPrice.getEanNumber()), productPrice, ttl);
                writtenKeys++;
            }
            return WriteResult.written(writtenKeys);
        } catch (RedisConnectionFailureException ignored) {
            // MySQL remains the source of truth if Redis is temporarily unavailable.
            logRedisWriteFailure(productPrice, ignored);
            return WriteResult.writeFailed();
        }
    }

    public List<ProductPriceDto> orderForIdentifiers(
            List<String> identifiers,
            Map<String, ProductPriceDto> cachedProductsByIdentifier,
            List<ProductPriceDto> databaseProducts
    ) {
        Map<String, ProductPriceDto> productsByIdentifier = new LinkedHashMap<>(cachedProductsByIdentifier);
        for (ProductPriceDto product : databaseProducts) {
            productsByIdentifier.putIfAbsent(product.getProductNumber(), product);
            if (hasText(product.getEanNumber())) {
                productsByIdentifier.putIfAbsent(product.getEanNumber(), product);
            }
        }

        return identifiers.stream()
                .map(productsByIdentifier::get)
                .filter(ProductPriceCacheService::hasPrice)
                .distinct()
                .toList();
    }

    private String productNumberKey(String productNumber) {
        return key(PRODUCT_NUMBER_KEY_PART, productNumber);
    }

    private String eanNumberKey(String eanNumber) {
        return key(EAN_NUMBER_KEY_PART, eanNumber);
    }

    private String key(String keyPart, String identifier) {
        return properties.getCachePrefix() + ":" + CACHE_SCHEMA_VERSION + ":" + keyPart + ":" + identifier;
    }

    private void logRedisReadFailure(String identifier, RuntimeException exception) {
        if (redisReadFailureLogged) {
            return;
        }

        redisReadFailureLogged = true;
        log.warn("Redis cache reads are unavailable. Falling back to MySQL for identifier {}.", identifier, exception);
    }

    private void logRedisWriteFailure(ProductPriceDto productPrice, RuntimeException exception) {
        if (redisWriteFailureLogged) {
            return;
        }

        redisWriteFailureLogged = true;
        String productNumber = productPrice == null ? null : productPrice.getProductNumber();
        log.warn("Redis cache writes are unavailable. MySQL remains the source of truth for product {}.", productNumber, exception);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasPrice(ProductPriceDto product) {
        return product != null && product.getPrice() != null;
    }

    private record WriteResult(int writtenKeys, boolean skippedWithoutPrice) {

        static WriteResult written(int writtenKeys) {
            return new WriteResult(writtenKeys, false);
        }

        static WriteResult skipped() {
            return new WriteResult(0, true);
        }

        static WriteResult writeFailed() {
            return new WriteResult(0, false);
        }
    }
}
