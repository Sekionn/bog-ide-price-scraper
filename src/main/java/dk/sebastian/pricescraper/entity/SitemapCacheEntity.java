package dk.sebastian.pricescraper.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "sitemap_cache",
        indexes = {
                @Index(name = "idx_sitemap_cache_fetched_at", columnList = "fetched_at")
        }
)
public class SitemapCacheEntity {

    @Id
    @Column(length = 768, nullable = false)
    private String url;

    @Lob
    @Column(name = "xml_content", nullable = false, columnDefinition = "LONGTEXT")
    private String xmlContent;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected SitemapCacheEntity() {
    }

    public SitemapCacheEntity(String url, String xmlContent, Instant fetchedAt) {
        this.url = url;
        this.xmlContent = xmlContent;
        this.fetchedAt = fetchedAt;
    }

    public void refresh(String xmlContent, Instant fetchedAt) {
        this.xmlContent = xmlContent;
        this.fetchedAt = fetchedAt;
    }

    public String getUrl() {
        return url;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
