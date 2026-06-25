package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
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

    private static final String PRODUCT_NUMBER_KEY_PART = "product-number";
    private static final String EAN_NUMBER_KEY_PART = "ean";

    private final RedisTemplate<String, ProductPriceDto> redisTemplate;
    private final ScraperProperties properties;

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
            if (product != null) {
                return Optional.of(product);
            }

            return Optional.ofNullable(redisTemplate.opsForValue().get(eanNumberKey(identifier)));
        } catch (RedisConnectionFailureException e) {
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
        Duration ttl = properties.getRefreshAfter();
        try {
            if (hasText(productPrice.getProductNumber())) {
                redisTemplate.opsForValue().set(productNumberKey(productPrice.getProductNumber()), productPrice, ttl);
            }
            if (hasText(productPrice.getEanNumber())) {
                redisTemplate.opsForValue().set(eanNumberKey(productPrice.getEanNumber()), productPrice, ttl);
            }
        } catch (RedisConnectionFailureException ignored) {
            // MySQL remains the source of truth if Redis is temporarily unavailable.
        }
    }

    public void writeAll(List<ProductPriceDto> productPrices) {
        for (ProductPriceDto productPrice : productPrices) {
            write(productPrice);
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
                .filter(product -> product != null)
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
        return properties.getCachePrefix() + ":" + keyPart + ":" + identifier;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
