package dk.sebastian.pricescraper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sebastian.pricescraper.records.PriceDetails;
import dk.sebastian.pricescraper.records.ProductIdentifiers;
import dk.sebastian.pricescraper.records.ProductPrice;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductPageScraperService {

    private static final Pattern JSON_PRICE_PATTERN = Pattern.compile("\"price\"\\s*:\\s*\"?([0-9.,]+)\"?");
    private static final Pattern SKU_PATTERN = Pattern.compile("\"sku\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BARCODE_PATTERN = Pattern.compile("\"barcode\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GTIN13_PATTERN = Pattern.compile("\"gtin13\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ISBN_PATTERN = Pattern.compile("\"isbn\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("\"author\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpFetcherService httpFetcher;
    private final ObjectMapper objectMapper;

    public ProductPageScraperService(HttpFetcherService httpFetcher, ObjectMapper objectMapper) {
        this.httpFetcher = httpFetcher;
        this.objectMapper = objectMapper;
    }

    public ProductPrice scrape(String productUrl) {
        String html = httpFetcher.fetch(productUrl);
        Document document = Jsoup.parse(html, productUrl);

        PriceDetails priceDetails = findPriceInJsonLd(document)
                .or(() -> findPriceInMetaTags(document))
                .or(() -> findPriceWithRegex(html))
                .orElseThrow(() -> new IllegalStateException("Could not find price on " + productUrl));
        ProductIdentifiers identifiers = findIdentifiersInJsonLd(document)
                .or(() -> findIdentifiersWithRegex(html))
                .orElseThrow(() -> new IllegalStateException("Could not find product number and EAN on " + productUrl));

        return new ProductPrice(
                productUrl,
                identifiers.productNumber(),
                identifiers.eanNumber(),
                findTitle(document),
                findAuthor(document, html).orElse(null),
                priceDetails.price(),
                priceDetails.currency(),
                priceDetails.availability(),
                Instant.now()
        );
    }

    private Optional<PriceDetails> findPriceInJsonLd(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            String json = script.data().isBlank() ? script.html() : script.data();
            if (json.isBlank()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(json);
                Optional<PriceDetails> price = findPriceInJson(root);
                if (price.isPresent()) {
                    return price;
                }
            } catch (Exception ignored) {
                // Some shops include multiple JSON objects or non-strict JSON. Fallbacks handle those pages.
            }
        }

        return Optional.empty();
    }

    private Optional<ProductIdentifiers> findIdentifiersInJsonLd(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            String json = script.data().isBlank() ? script.html() : script.data();
            if (json.isBlank()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(json);
                Optional<ProductIdentifiers> identifiers = findIdentifiersInJson(root);
                if (identifiers.isPresent()) {
                    return identifiers;
                }
            } catch (Exception ignored) {
                // Non-strict JSON-LD is handled by the regex fallback.
            }
        }

        return Optional.empty();
    }

    private Optional<String> findAuthor(Document document, String html) {
        return findAuthorInJsonLd(document)
                .or(() -> findAuthorInMetaTags(document))
                .or(() -> findAuthorWithRegex(html));
    }

    private Optional<String> findAuthorInJsonLd(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            String json = script.data().isBlank() ? script.html() : script.data();
            if (json.isBlank()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(json);
                Optional<String> author = findAuthorInJson(root);
                if (author.isPresent()) {
                    return author;
                }
            } catch (Exception ignored) {
                // Non-strict JSON-LD is handled by the regex fallback.
            }
        }

        return Optional.empty();
    }

    private Optional<PriceDetails> findPriceInJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<PriceDetails> price = findPriceInJson(child);
                if (price.isPresent()) {
                    return price;
                }
            }
            return Optional.empty();
        }

        if (!node.isObject()) {
            return Optional.empty();
        }

        if (node.has("offers")) {
            Optional<PriceDetails> offerPrice = findPriceInJson(node.get("offers"));
            if (offerPrice.isPresent()) {
                return offerPrice;
            }
        }

        if (node.hasNonNull("price")) {
            return parsePrice(node.get("price").asText()).map(price -> new PriceDetails(
                    price,
                    textOrDefault(node.get("priceCurrency"), "DKK"),
                    cleanAvailability(textOrDefault(node.get("availability"), ""))
            ));
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            Optional<PriceDetails> price = findPriceInJson(children.next());
            if (price.isPresent()) {
                return price;
            }
        }

        return Optional.empty();
    }

    private Optional<ProductIdentifiers> findIdentifiersInJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<ProductIdentifiers> identifiers = findIdentifiersInJson(child);
                if (identifiers.isPresent()) {
                    return identifiers;
                }
            }
            return Optional.empty();
        }

        if (!node.isObject()) {
            return Optional.empty();
        }

        String productNumber = firstNonBlank(textOrDefault(node.get("sku"), ""));
        String eanNumber = firstNonBlank(
                textOrDefault(node.get("gtin13"), ""),
                textOrDefault(node.get("gtin"), ""),
                textOrDefault(node.get("isbn"), ""),
                textOrDefault(node.get("barcode"), "")
        );

        if (productNumber != null && eanNumber != null) {
            return Optional.of(new ProductIdentifiers(productNumber, eanNumber));
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            Optional<ProductIdentifiers> identifiers = findIdentifiersInJson(children.next());
            if (identifiers.isPresent()) {
                return identifiers;
            }
        }

        return Optional.empty();
    }

    private Optional<String> findAuthorInJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<String> author = findAuthorInJson(child);
                if (author.isPresent()) {
                    return author;
                }
            }
            return Optional.empty();
        }

        if (!node.isObject()) {
            return Optional.empty();
        }

        if (node.has("author")) {
            Optional<String> author = authorValue(node.get("author"));
            if (author.isPresent()) {
                return author;
            }
        }

        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            Optional<String> author = findAuthorInJson(children.next());
            if (author.isPresent()) {
                return author;
            }
        }

        return Optional.empty();
    }

    private Optional<PriceDetails> findPriceInMetaTags(Document document) {
        String priceValue = firstNonBlank(
                attr(document, "meta[property=product:price:amount]", "content"),
                attr(document, "meta[property=og:price:amount]", "content"),
                attr(document, "meta[itemprop=price]", "content"),
                attr(document, "[itemprop=price]", "content"),
                text(document, "[itemprop=price]")
        );

        if (priceValue == null) {
            return Optional.empty();
        }

        String currency = firstNonBlank(
                attr(document, "meta[property=product:price:currency]", "content"),
                attr(document, "meta[property=og:price:currency]", "content"),
                attr(document, "meta[itemprop=priceCurrency]", "content"),
                attr(document, "[itemprop=priceCurrency]", "content"),
                "DKK"
        );

        String availability = cleanAvailability(firstNonBlank(
                attr(document, "link[itemprop=availability]", "href"),
                attr(document, "meta[property=product:availability]", "content"),
                ""
        ));

        return parsePrice(priceValue).map(price -> new PriceDetails(price, currency, availability));
    }

    private Optional<PriceDetails> findPriceWithRegex(String html) {
        Matcher matcher = JSON_PRICE_PATTERN.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }

        return parsePrice(matcher.group(1)).map(price -> new PriceDetails(price, "DKK", ""));
    }

    private Optional<String> findAuthorInMetaTags(Document document) {
        return Optional.ofNullable(firstNonBlank(
                attr(document, "meta[name=author]", "content"),
                attr(document, "meta[property=book:author]", "content"),
                attr(document, "[itemprop=author]", "content"),
                text(document, "[itemprop=author]")
        ));
    }

    private Optional<String> findAuthorWithRegex(String html) {
        return Optional.ofNullable(firstRegexGroup(html, AUTHOR_PATTERN));
    }

    private Optional<ProductIdentifiers> findIdentifiersWithRegex(String html) {
        String productNumber = firstRegexGroup(html, SKU_PATTERN);
        String eanNumber = firstNonBlank(
                firstRegexGroup(html, GTIN13_PATTERN),
                firstRegexGroup(html, ISBN_PATTERN),
                firstRegexGroup(html, BARCODE_PATTERN)
        );

        if (productNumber == null || eanNumber == null) {
            return Optional.empty();
        }

        return Optional.of(new ProductIdentifiers(productNumber, eanNumber));
    }

    private static String findTitle(Document document) {
        return firstNonBlank(
                attr(document, "meta[property=og:title]", "content"),
                text(document, "h1"),
                document.title()
        );
    }

    private static Optional<String> authorValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }

        if (node.isTextual()) {
            return Optional.ofNullable(firstNonBlank(node.asText()));
        }

        if (node.isArray()) {
            List<String> authors = new ArrayList<>();
            for (JsonNode child : node) {
                authorValue(child).ifPresent(authors::add);
            }
            return authors.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", authors));
        }

        if (node.isObject()) {
            return Optional.ofNullable(firstNonBlank(
                    textOrDefault(node.get("name"), ""),
                    textOrDefault(node.get("@id"), "")
            ));
        }

        return Optional.empty();
    }

    private static String attr(Document document, String selector, String attribute) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.attr(attribute);
    }

    private static String text(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.text();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String firstRegexGroup(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String textOrDefault(JsonNode node, String defaultValue) {
        return node == null || node.isNull() || node.asText().isBlank() ? defaultValue : node.asText();
    }

    private static String cleanAvailability(String availability) {
        if (availability == null || availability.isBlank()) {
            return "";
        }

        int slashIndex = availability.lastIndexOf('/');
        return slashIndex >= 0 ? availability.substring(slashIndex + 1) : availability;
    }

    private static Optional<BigDecimal> parsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return Optional.empty();
        }

        String value = rawPrice.replaceAll("[^0-9,.]", "");
        if (value.isBlank()) {
            return Optional.empty();
        }

        if (value.contains(",") && value.contains(".")) {
            value = value.lastIndexOf(',') > value.lastIndexOf('.')
                    ? value.replace(".", "").replace(",", ".")
                    : value.replace(",", "");
        } else if (value.contains(",")) {
            value = value.replace(",", ".");
        }

        int firstDot = value.indexOf('.');
        int lastDot = value.lastIndexOf('.');
        if (firstDot != lastDot) {
            value = value.substring(0, lastDot).replace(".", "") + value.substring(lastDot);
        }

        try {
            return Optional.of(new BigDecimal(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
