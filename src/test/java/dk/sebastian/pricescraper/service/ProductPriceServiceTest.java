package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.records.ProductPrice;
import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.repository.ProductLookupFailureRepository;
import dk.sebastian.pricescraper.repository.ProductPriceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ProductPriceServiceTest {

    @Test
    void savesBothPricesAndReturnsOfferPriceWithFlag() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceCacheService cache = new ProductPriceCacheService(null, new ScraperProperties()) {
            @Override
            public void write(ProductPriceDto productPrice) {
            }
        };
        when(repository.findById("123")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPriceService service = new ProductPriceService(
                repository,
                mock(ProductLookupFailureRepository.class),
                cache,
                new ScraperProperties()
        );

        ProductPriceDto result = service.save(new ProductPrice(
                "https://www.bog-ide.dk/products/example-123",
                "123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                new BigDecimal("129.95"),
                "DKK",
                "InStock",
                Instant.EPOCH
        ));

        ArgumentCaptor<ProductPriceEntity> entityCaptor = ArgumentCaptor.forClass(ProductPriceEntity.class);
        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getNormalPrice()).hasToString("199.95");
        assertThat(entityCaptor.getValue().getSpecialOfferPrice()).hasToString("129.95");
        assertThat(result.getPrice()).hasToString("129.95");
        assertThat(result.isSpecialOffer()).isTrue();
        assertThat(result.isStalePrice()).isTrue();
    }

    @Test
    void clearsStoredSpecialOfferWhenCurrentScrapeHasNoOffer() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity previouslyDiscounted = new ProductPriceEntity(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                new BigDecimal("129.95"),
                "DKK",
                "InStock",
                Instant.EPOCH,
                0,
                Instant.EPOCH,
                false
        );
        when(repository.findById("123")).thenReturn(Optional.of(previouslyDiscounted));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPriceCacheService cache = new ProductPriceCacheService(null, new ScraperProperties()) {
            @Override
            public void write(ProductPriceDto productPrice) {
            }
        };
        ProductPriceService service = new ProductPriceService(
                repository,
                mock(ProductLookupFailureRepository.class),
                cache,
                new ScraperProperties()
        );

        ProductPriceDto result = service.save(new ProductPrice(
                "https://www.bog-ide.dk/products/example-123",
                "123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                null,
                "DKK",
                "InStock",
                Instant.now()
        ));

        ArgumentCaptor<ProductPriceEntity> entityCaptor = ArgumentCaptor.forClass(ProductPriceEntity.class);
        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getSpecialOfferPrice()).isNull();
        assertThat(result.getPrice()).hasToString("199.95");
        assertThat(result.isSpecialOffer()).isFalse();
        assertThat(result.isStalePrice()).isFalse();
    }

    @Test
    void preservesStaleRequestCountWhenProductIsSuccessfullySaved() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        Instant lastRequestedAt = Instant.parse("2026-08-30T09:00:00Z");
        ProductPriceEntity previouslyRequestedWhileStale = new ProductPriceEntity(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                "DKK",
                "InStock",
                Instant.EPOCH,
                7,
                lastRequestedAt
        );
        ProductPriceCacheService cache = new ProductPriceCacheService(null, new ScraperProperties()) {
            @Override
            public void write(ProductPriceDto productPrice) {
            }
        };
        when(repository.findById("123")).thenReturn(Optional.of(previouslyRequestedWhileStale));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPriceService service = new ProductPriceService(
                repository,
                mock(ProductLookupFailureRepository.class),
                cache,
                new ScraperProperties()
        );

        service.save(new ProductPrice(
                "https://www.bog-ide.dk/products/example-123",
                "123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                "DKK",
                "InStock",
                Instant.now()
        ));

        ArgumentCaptor<ProductPriceEntity> entityCaptor = ArgumentCaptor.forClass(ProductPriceEntity.class);
        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStaleRequestCount()).isEqualTo(7);
        assertThat(entityCaptor.getValue().getLastRequestedAt()).isEqualTo(lastRequestedAt);
    }

    @Test
    void clearsLookupFailureWhenProductIsSuccessfullySaved() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductLookupFailureRepository lookupFailureRepository = mock(ProductLookupFailureRepository.class);
        ProductPriceCacheService cache = new ProductPriceCacheService(null, new ScraperProperties()) {
            @Override
            public void write(ProductPriceDto productPrice) {
            }
        };
        when(repository.findById("123")).thenReturn(Optional.empty());
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPriceService service = new ProductPriceService(repository, lookupFailureRepository, cache, new ScraperProperties());

        service.save(new ProductPrice(
                "https://www.bog-ide.dk/products/example-123",
                "123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                "DKK",
                "InStock",
                Instant.EPOCH
        ));

        verify(lookupFailureRepository).deleteById("123");
    }

    @Test
    void clearsStoredNormalAndSpecialOfferPrices() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity previouslyPriced = new ProductPriceEntity(
                "123",
                "https://www.bog-ide.dk/products/example-123",
                "9788711477960",
                "Example",
                "Author",
                new BigDecimal("199.95"),
                new BigDecimal("129.95"),
                "DKK",
                "InStock",
                Instant.EPOCH,
                0,
                Instant.EPOCH,
                false
        );
        when(repository.findById("123")).thenReturn(Optional.of(previouslyPriced));
        ProductPriceCacheService cache = new ProductPriceCacheService(null, new ScraperProperties()) {
            private ProductPriceDto evictedProduct;

            @Override
            public void evict(ProductPriceDto productPrice) {
                evictedProduct = productPrice;
            }
        };
        ProductPriceService service = new ProductPriceService(
                repository,
                mock(ProductLookupFailureRepository.class),
                cache,
                new ScraperProperties()
        );

        service.clearPrices("123");

        assertThat(previouslyPriced.getNormalPrice()).isNull();
        assertThat(previouslyPriced.getSpecialOfferPrice()).isNull();
    }

    @Test
    void discoveryAddsUrlToExistingPlaceholder() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity placeholder = new ProductPriceEntity("123456", null, null);
        when(repository.findById("123456")).thenReturn(Optional.of(placeholder));
        ProductPriceService service = new ProductPriceService(repository, null, null, new ScraperProperties());

        boolean inserted = service.trackProduct(
                "123456",
                "https://www.bog-ide.dk/products/discovered-product-123456",
                null
        );

        assertThat(inserted).isFalse();
        assertThat(placeholder.getUrl())
                .isEqualTo("https://www.bog-ide.dk/products/discovered-product-123456");
        verify(repository, never()).save(placeholder);
    }

    @Test
    void discoveryDoesNotOverwriteStoredUrl() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity product = new ProductPriceEntity(
                "123456",
                "https://www.bog-ide.dk/products/existing-product-123456",
                null
        );
        when(repository.findById("123456")).thenReturn(Optional.of(product));
        ProductPriceService service = new ProductPriceService(repository, null, null, new ScraperProperties());

        service.trackProduct(
                "123456",
                "https://www.bog-ide.dk/products/different-product-123456",
                null
        );

        assertThat(product.getUrl())
                .isEqualTo("https://www.bog-ide.dk/products/existing-product-123456");
    }

    @Test
    void manualDiscoveryCanOverwriteStoredUrl() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity product = new ProductPriceEntity(
                "123456",
                "https://www.bog-ide.dk/products/old-product-123456",
                null
        );
        when(repository.findById("123456")).thenReturn(Optional.of(product));
        ProductPriceService service = new ProductPriceService(repository, null, null, new ScraperProperties());

        service.trackProduct(
                "123456",
                "https://www.bog-ide.dk/products/current-product-123456",
                null,
                true
        );

        assertThat(product.getUrl())
                .isEqualTo("https://www.bog-ide.dk/products/current-product-123456");
    }
}
