package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.dto.ProductLookupFailureDto;
import dk.sebastian.pricescraper.dto.ProductPriceDto;
import dk.sebastian.pricescraper.dto.ProductPriceLookupRequestDto;
import dk.sebastian.pricescraper.service.ProductLookupFailureService;
import dk.sebastian.pricescraper.service.ProductRefreshService;
import dk.sebastian.pricescraper.service.ProductPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "Prices", description = "Read and refresh product price data.")
public class ProductPriceController {

    private final ProductPriceService productPriceService;
    private final ProductRefreshService productRefreshService;
    private final ProductLookupFailureService productLookupFailureService;

    public ProductPriceController(
            ProductPriceService productPriceService,
            ProductRefreshService productRefreshService,
            ProductLookupFailureService productLookupFailureService
    ) {
        this.productPriceService = productPriceService;
        this.productRefreshService = productRefreshService;
        this.productLookupFailureService = productLookupFailureService;
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest prices", description = "Returns up to 100 latest stored product price rows.")
    public List<ProductPriceDto> latest() {
        return productPriceService.findLatest();
    }

    @PostMapping("/batch")
    @Operation(summary = "Find prices by identifier", description = "Looks up products by Varenr. or EAN and refreshes stale known products later.")
    public List<ProductPriceDto> batch(@RequestBody List<ProductPriceLookupRequestDto> requests) {
        return productRefreshService.findFreshByProductNumberOrEanNumberRequests(requests);
    }

    @GetMapping("/lookup-failures")
    @Operation(summary = "Get failed product lookups", description = "Returns tracked products that could not be found after all lookup strategies.")
    public List<ProductLookupFailureDto> lookupFailures() {
        return productLookupFailureService.findAll();
    }
}
