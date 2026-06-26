package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.entity.SitemapCacheEntity;
import dk.sebastian.pricescraper.repository.SitemapCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

@Service
public class SitemapCacheService {

    private final SitemapCacheRepository sitemapCacheRepository;
    private final ScraperProperties properties;

    public SitemapCacheService(SitemapCacheRepository sitemapCacheRepository, ScraperProperties properties) {
        this.sitemapCacheRepository = sitemapCacheRepository;
        this.properties = properties;
    }

    @Transactional
    public String getOrFetch(String url, Supplier<String> fetcher) {
        Instant now = Instant.now();
        SitemapCacheEntity cachedSitemap = sitemapCacheRepository.findById(url).orElse(null);
        if (cachedSitemap != null && isFresh(cachedSitemap, now)) {
            return cachedSitemap.getXmlContent();
        }

        String xmlContent = fetcher.get();
        if (cachedSitemap == null) {
            sitemapCacheRepository.save(new SitemapCacheEntity(url, xmlContent, now));
        } else {
            cachedSitemap.refresh(xmlContent, now);
            sitemapCacheRepository.save(cachedSitemap);
        }

        return xmlContent;
    }

    private boolean isFresh(SitemapCacheEntity sitemap, Instant now) {
        Duration cacheTtl = properties.getSitemapCacheTtl();
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            return false;
        }

        return sitemap.getFetchedAt() != null && sitemap.getFetchedAt().plus(cacheTtl).isAfter(now);
    }
}
