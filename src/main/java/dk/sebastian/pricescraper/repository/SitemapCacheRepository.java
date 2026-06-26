package dk.sebastian.pricescraper.repository;

import dk.sebastian.pricescraper.entity.SitemapCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitemapCacheRepository extends JpaRepository<SitemapCacheEntity, String> {
}
