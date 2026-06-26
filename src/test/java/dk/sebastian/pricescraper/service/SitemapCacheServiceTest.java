package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.entity.SitemapCacheEntity;
import dk.sebastian.pricescraper.repository.SitemapCacheRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SitemapCacheServiceTest {

    @Test
    void returnsFreshCachedSitemapWithoutFetching() {
        ScraperProperties properties = new ScraperProperties();
        SitemapCacheRepository repository = mock(SitemapCacheRepository.class);
        SitemapCacheService sitemapCacheService = new SitemapCacheService(repository, properties);
        String sitemapUrl = "https://www.bog-ide.dk/sitemap.xml";
        when(repository.findById(sitemapUrl)).thenReturn(Optional.of(new SitemapCacheEntity(
                sitemapUrl,
                "<sitemapindex />",
                Instant.now().minus(Duration.ofDays(1))
        )));

        AtomicInteger fetchCount = new AtomicInteger();
        String xml = sitemapCacheService.getOrFetch(sitemapUrl, () -> {
            fetchCount.incrementAndGet();
            return "<fresh />";
        });

        assertThat(xml).isEqualTo("<sitemapindex />");
        assertThat(fetchCount).hasValue(0);
        verify(repository, never()).save(any());
    }

    @Test
    void refreshesCachedSitemapWhenItIsOutdated() {
        ScraperProperties properties = new ScraperProperties();
        SitemapCacheRepository repository = mock(SitemapCacheRepository.class);
        SitemapCacheService sitemapCacheService = new SitemapCacheService(repository, properties);
        String sitemapUrl = "https://www.bog-ide.dk/sitemap.xml";
        SitemapCacheEntity cachedSitemap = new SitemapCacheEntity(
                sitemapUrl,
                "<old />",
                Instant.now().minus(Duration.ofDays(8))
        );
        when(repository.findById(sitemapUrl)).thenReturn(Optional.of(cachedSitemap));

        String xml = sitemapCacheService.getOrFetch(sitemapUrl, () -> "<new />");

        assertThat(xml).isEqualTo("<new />");
        assertThat(cachedSitemap.getXmlContent()).isEqualTo("<new />");
        verify(repository).save(cachedSitemap);
    }
}
