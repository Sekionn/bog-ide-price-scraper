package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.dto.ProductDiscoveryRunResponseDto;
import dk.sebastian.pricescraper.dto.ProductDiscoveryStatusDto;
import dk.sebastian.pricescraper.service.ProductDiscoveryApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
@Tag(name = "Product discovery", description = "Discover new product numbers from product sitemaps.")
public class ProductDiscoveryController {

    private final ProductDiscoveryApiService productDiscoveryApiService;

    public ProductDiscoveryController(ProductDiscoveryApiService productDiscoveryApiService) {
        this.productDiscoveryApiService = productDiscoveryApiService;
    }

    @PostMapping("/run")
    @Operation(
            summary = "Start product discovery",
            description = "Scans product sitemaps and stores missing product numbers and URLs. "
                    + "Existing URLs are only replaced when overwriteExistingUrls is true."
    )
    public ResponseEntity<ProductDiscoveryRunResponseDto> runNow(
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean overwriteExistingUrls
    ) {
        return productDiscoveryApiService.runNow(limit, overwriteExistingUrls);
    }

    @GetMapping("/status")
    @Operation(summary = "Get product discovery status", description = "Returns current discovery status and the last discovery summary.")
    public ProductDiscoveryStatusDto status() {
        return productDiscoveryApiService.status();
    }
}
