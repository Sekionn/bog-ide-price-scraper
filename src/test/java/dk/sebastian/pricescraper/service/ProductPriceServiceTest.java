package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.entity.ProductPriceEntity;
import dk.sebastian.pricescraper.repository.ProductPriceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductPriceServiceTest {

    @Test
    void discoveryAddsUrlToExistingPlaceholder() {
        ProductPriceRepository repository = mock(ProductPriceRepository.class);
        ProductPriceEntity placeholder = new ProductPriceEntity("123456", null, null);
        when(repository.findById("123456")).thenReturn(Optional.of(placeholder));
        ProductPriceService service = new ProductPriceService(repository, null);

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
        ProductPriceService service = new ProductPriceService(repository, null);

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
        ProductPriceService service = new ProductPriceService(repository, null);

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
