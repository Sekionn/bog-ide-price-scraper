package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.records.ProductDiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ProductDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProductDiscoveryService.class);

    private final SitemapService sitemapService;
    private final ProductPriceService productPriceService;

    public ProductDiscoveryService(SitemapService sitemapService, ProductPriceService productPriceService) {
        this.sitemapService = sitemapService;
        this.productPriceService = productPriceService;
    }

    public ProductDiscoveryResult discoverProductsUntil(Instant deadline, int productLimit) {
        int discovered = 0;
        int alreadyKnown = 0;
        int invalidUrl = 0;
        int failed = 0;

        for (String productSitemapUrl : sitemapService.findProductSitemapUrls()) {
            if (shouldStop(deadline, productLimit, discovered)) {
                break;
            }

            for (String productUrl : sitemapService.findProductUrls(productSitemapUrl)) {
                if (shouldStop(deadline, productLimit, discovered)) {
                    break;
                }

                Optional<String> productNumber = sitemapService.extractProductNumber(productUrl);
                if (productNumber.isEmpty()) {
                    invalidUrl++;
                    continue;
                }

                try {
                    if (productPriceService.trackProduct(productNumber.get(), productUrl, null)) {
                        discovered++;
                    } else {
                        alreadyKnown++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("Could not track discovered product {} from {}", productNumber.get(), productUrl, e);
                }
            }
        }

        return new ProductDiscoveryResult(discovered, alreadyKnown, invalidUrl, failed);
    }

    private static boolean shouldStop(Instant deadline, int productLimit, int discovered) {
        if (Instant.now().isAfter(deadline)) {
            return true;
        }

        return productLimit > 0 && discovered >= productLimit;
    }
}
