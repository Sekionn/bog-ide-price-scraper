package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.dto.ProductDiscoveryRunResponseDto;
import dk.sebastian.pricescraper.dto.ProductDiscoveryStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProductDiscoveryApiService {

    private final ProductDiscoveryJobService productDiscoveryJobService;

    public ProductDiscoveryApiService(ProductDiscoveryJobService productDiscoveryJobService) {
        this.productDiscoveryJobService = productDiscoveryJobService;
    }

    public ResponseEntity<ProductDiscoveryRunResponseDto> runNow(Integer limit) {
        if (limit != null && limit < 0) {
            return ResponseEntity.badRequest().body(new ProductDiscoveryRunResponseDto(
                    false,
                    "Discovery limit must be 0 or greater"
            ));
        }

        if (productDiscoveryJobService.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ProductDiscoveryRunResponseDto(
                    false,
                    "Product discovery is already running"
            ));
        }

        CompletableFuture.runAsync(() -> productDiscoveryJobService.runOnce(limit));

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ProductDiscoveryRunResponseDto(
                true,
                "Product discovery started. Check /api/discovery/status for progress."
        ));
    }

    public ProductDiscoveryStatusDto status() {
        return new ProductDiscoveryStatusDto(
                productDiscoveryJobService.isRunning(),
                productDiscoveryJobService.getLastSummary(),
                productDiscoveryJobService.getLastError(),
                productDiscoveryJobService.getLastFailedUrl()
        );
    }
}
