package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SitemapService {

    private final HttpFetcherService httpFetcher;
    private final ScraperProperties properties;
    private final RobotsService robotsService;

    @Autowired
    public SitemapService(HttpFetcherService httpFetcher, ScraperProperties properties, RobotsService robotsService) {
        this.httpFetcher = httpFetcher;
        this.properties = properties;
        this.robotsService = robotsService;
    }

    SitemapService(HttpFetcherService httpFetcher, ScraperProperties properties) {
        this(httpFetcher, properties, null);
    }

    public List<String> findProductSitemapUrls() {
        verifyAllowed(properties.getSitemapIndexUrl());
        String sitemapIndexXml = httpFetcher.fetch(properties.getSitemapIndexUrl());

        return extractLocValues(sitemapIndexXml).stream()
                .filter(this::isProductSitemapUrl)
                .filter(this::isAllowed)
                .limit(limitOrMax(properties.getMaxSitemapsPerRun()))
                .toList();
    }

    public List<String> findProductUrls(String productSitemapUrl) {
        if (!isProductSitemapUrl(productSitemapUrl)) {
            throw new IllegalArgumentException("Refusing non-product sitemap URL " + productSitemapUrl);
        }

        verifyAllowed(productSitemapUrl);
        String productSitemapXml = httpFetcher.fetch(productSitemapUrl);

        Set<String> productUrls = new LinkedHashSet<>();
        for (String url : extractLocValues(productSitemapXml)) {
            if (isAllowedProductUrl(url) && isAllowed(url)) {
                productUrls.add(url);
            }
        }

        return new ArrayList<>(productUrls);
    }

    public Map<String, String> findProductUrlsByProductNumbers(Collection<String> productNumbers) {
        Set<String> remainingProductNumbers = new LinkedHashSet<>(productNumbers);
        Map<String, String> foundUrlsByProductNumber = new LinkedHashMap<>();

        for (String productSitemapUrl : findProductSitemapUrls()) {
            if (remainingProductNumbers.isEmpty()) {
                break;
            }

            for (String productUrl : findProductUrls(productSitemapUrl)) {
                String matchedProductNumber = findMatchingProductNumber(productUrl, remainingProductNumbers);
                if (matchedProductNumber != null) {
                    foundUrlsByProductNumber.put(matchedProductNumber, productUrl);
                    remainingProductNumbers.remove(matchedProductNumber);
                }
            }
        }

        return foundUrlsByProductNumber;
    }

    List<String> extractLocValues(String xml) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(trimBeforeXml(xml))));
            NodeList locNodes = document.getElementsByTagName("loc");

            List<String> locValues = new ArrayList<>();
            for (int i = 0; i < locNodes.getLength(); i++) {
                String value = locNodes.item(i).getTextContent();
                if (value != null && !value.isBlank()) {
                    locValues.add(value.trim());
                }
            }

            return locValues;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse sitemap XML", e);
        }
    }

    private static long limitOrMax(int configuredLimit) {
        return configuredLimit > 0 ? configuredLimit : Long.MAX_VALUE;
    }

    private boolean isProductSitemapUrl(String url) {
        return url.contains(properties.getProductSitemapUrlContains());
    }

    private boolean isAllowedProductUrl(String url) {
        return url.startsWith(properties.getAllowedProductUrlPrefix());
    }

    private void verifyAllowed(String url) {
        if (robotsService != null) {
            robotsService.verifyAllowed(url);
        }
    }

    private boolean isAllowed(String url) {
        return robotsService == null || robotsService.isAllowed(url);
    }

    private static String findMatchingProductNumber(String productUrl, Set<String> productNumbers) {
        for (String productNumber : productNumbers) {
            if (productUrl.endsWith("-" + productNumber)) {
                return productNumber;
            }
        }

        return null;
    }

    private static String trimBeforeXml(String xml) {
        int firstTag = xml.indexOf('<');
        if (firstTag < 0) {
            throw new IllegalArgumentException("Input does not contain XML");
        }
        return xml.substring(firstTag);
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
