package dk.sebastian.pricescraper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "scraper")
public class ScraperProperties {

    private final ScraperScheduleProperties schedule = new ScraperScheduleProperties();
    private String robotsTxtUrl = "https://www.bog-ide.dk/robots.txt";
    private String sitemapIndexUrl = "https://www.bog-ide.dk/sitemap.xml";
    private String allowedProductUrlPrefix = "https://www.bog-ide.dk/products/";
    private String productSitemapUrlContains = "sitemap_products_";
    private String userAgent = "BogIdePriceScraper/1.0";
    private Duration requestDelay = Duration.ofSeconds(4);
    private Duration minimumRequestDelay = Duration.ofSeconds(1);
    private Duration requestTimeout = Duration.ofSeconds(20);
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration refreshAfter = Duration.ofDays(7);
    private Duration sitemapCacheTtl = Duration.ofDays(7);
    private Duration maxRunTime = Duration.ofHours(5);
    private String discoveryCron = "0 0 3 ? * MON";
    private String discoveryZone = "Europe/Copenhagen";
    private Duration discoveryMaxRunTime = Duration.ofHours(5);
    private String cachePrefix = "product-price";
    private int maxSitemapsPerRun = 0;
    private int maxProductsPerRun = 0;
    private int maxDiscoveredProductsPerRun = 0;

    public ScraperScheduleProperties getSchedule() {
        return schedule;
    }

    public String getRobotsTxtUrl() {
        return robotsTxtUrl;
    }

    public void setRobotsTxtUrl(String robotsTxtUrl) {
        this.robotsTxtUrl = robotsTxtUrl;
    }

    public String getSitemapIndexUrl() {
        return sitemapIndexUrl;
    }

    public void setSitemapIndexUrl(String sitemapIndexUrl) {
        this.sitemapIndexUrl = sitemapIndexUrl;
    }

    public String getAllowedProductUrlPrefix() {
        return allowedProductUrlPrefix;
    }

    public void setAllowedProductUrlPrefix(String allowedProductUrlPrefix) {
        this.allowedProductUrlPrefix = allowedProductUrlPrefix;
    }

    public String getProductSitemapUrlContains() {
        return productSitemapUrlContains;
    }

    public void setProductSitemapUrlContains(String productSitemapUrlContains) {
        this.productSitemapUrlContains = productSitemapUrlContains;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Duration getRequestDelay() {
        return requestDelay;
    }

    public void setRequestDelay(Duration requestDelay) {
        this.requestDelay = requestDelay;
    }

    public Duration getMinimumRequestDelay() {
        return minimumRequestDelay;
    }

    public void setMinimumRequestDelay(Duration minimumRequestDelay) {
        this.minimumRequestDelay = minimumRequestDelay;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getRefreshAfter() {
        return refreshAfter;
    }

    public void setRefreshAfter(Duration refreshAfter) {
        this.refreshAfter = refreshAfter;
    }

    public Duration getSitemapCacheTtl() {
        return sitemapCacheTtl;
    }

    public void setSitemapCacheTtl(Duration sitemapCacheTtl) {
        this.sitemapCacheTtl = sitemapCacheTtl;
    }

    public Duration getMaxRunTime() {
        return maxRunTime;
    }

    public void setMaxRunTime(Duration maxRunTime) {
        this.maxRunTime = maxRunTime;
    }

    public String getDiscoveryCron() {
        return discoveryCron;
    }

    public void setDiscoveryCron(String discoveryCron) {
        this.discoveryCron = discoveryCron;
    }

    public String getDiscoveryZone() {
        return discoveryZone;
    }

    public void setDiscoveryZone(String discoveryZone) {
        this.discoveryZone = discoveryZone;
    }

    public Duration getDiscoveryMaxRunTime() {
        return discoveryMaxRunTime;
    }

    public void setDiscoveryMaxRunTime(Duration discoveryMaxRunTime) {
        this.discoveryMaxRunTime = discoveryMaxRunTime;
    }

    public String getCachePrefix() {
        return cachePrefix;
    }

    public void setCachePrefix(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    public int getMaxSitemapsPerRun() {
        return maxSitemapsPerRun;
    }

    public void setMaxSitemapsPerRun(int maxSitemapsPerRun) {
        this.maxSitemapsPerRun = maxSitemapsPerRun;
    }

    public int getMaxProductsPerRun() {
        return maxProductsPerRun;
    }

    public void setMaxProductsPerRun(int maxProductsPerRun) {
        this.maxProductsPerRun = maxProductsPerRun;
    }

    public int getMaxDiscoveredProductsPerRun() {
        return maxDiscoveredProductsPerRun;
    }

    public void setMaxDiscoveredProductsPerRun(int maxDiscoveredProductsPerRun) {
        this.maxDiscoveredProductsPerRun = maxDiscoveredProductsPerRun;
    }

}
