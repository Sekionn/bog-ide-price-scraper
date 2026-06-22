package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.records.ProductPriceDto;
import dk.sebastian.pricescraper.service.ProductRefreshService;
import dk.sebastian.pricescraper.service.ProductPriceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class ProductPriceController {

    private final ProductPriceService productPriceService;
    private final ProductRefreshService productRefreshService;

    public ProductPriceController(ProductPriceService productPriceService, ProductRefreshService productRefreshService) {
        this.productPriceService = productPriceService;
        this.productRefreshService = productRefreshService;
    }

    @GetMapping("/latest")
    public List<ProductPriceDto> latest() {
        return productPriceService.findLatest();
    }

    @PostMapping("/batch")
    public List<ProductPriceDto> batch(@RequestBody List<String> identifiers) {
        return productRefreshService.findFreshByProductNumberOrEanNumber(identifiers);
    }
}
