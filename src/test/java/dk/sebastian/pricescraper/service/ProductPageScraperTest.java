package dk.sebastian.pricescraper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.records.ProductPrice;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPageScraperTest {

    @Test
    void scrapesProductNumberAndEanAsStrings() {
        String html = """
                <html>
                  <head>
                    <meta property="og:title" content="Egholms Gud">
                    <script type="application/ld+json">
                      {
                        "@context": "https://schema.org",
                        "@type": ["Product","Book"],
                        "name": "Egholms Gud",
                        "author": {
                          "@type": "Person",
                          "name": "Johannes Buchholtz"
                        },
                        "sku": "2287895",
                        "gtin13": "9788711477960",
                        "offers": {
                          "@type": "Offer",
                          "price": "69.95",
                          "priceCurrency": "DKK",
                          "availability": "https://schema.org/InStock"
                        }
                      }
                    </script>
                  </head>
                  <body></body>
                </html>
                """;
        ProductPageScraperService scraper = new ProductPageScraperService(
                new StaticHttpFetcher(new ScraperProperties(), html),
                new ObjectMapper()
        );

        ProductPrice productPrice = scraper.scrape("https://www.bog-ide.dk/products/example");

        assertThat(productPrice.productNumber()).isEqualTo("2287895");
        assertThat(productPrice.eanNumber()).isEqualTo("9788711477960");
        assertThat(productPrice.author()).isEqualTo("Johannes Buchholtz");
        assertThat(productPrice.price()).hasToString("69.95");
    }

    @Test
    void scrapesMultipleAuthorsFromJsonLdArray() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                      {
                        "@context": "https://schema.org",
                        "@type": "Book",
                        "name": "Example Book",
                        "author": [
                          {"@type": "Person", "name": "First Author"},
                          {"@type": "Person", "name": "Second Author"}
                        ],
                        "sku": "12345",
                        "gtin13": "9788711477960",
                        "offers": {
                          "@type": "Offer",
                          "price": "129.95",
                          "priceCurrency": "DKK"
                        }
                      }
                    </script>
                  </head>
                </html>
                """;
        ProductPageScraperService scraper = new ProductPageScraperService(
                new StaticHttpFetcher(new ScraperProperties(), html),
                new ObjectMapper()
        );

        ProductPrice productPrice = scraper.scrape("https://www.bog-ide.dk/products/example");

        assertThat(productPrice.author()).isEqualTo("First Author, Second Author");
    }

    @Test
    void separatesSpecialOfferFromNormalPrice() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                      {
                        "@type": "Product",
                        "sku": "12345",
                        "gtin13": "9788711477960",
                        "normalPrice": "199.95",
                        "offers": {
                          "@type": "Offer",
                          "price": "129.95",
                          "priceCurrency": "DKK"
                        }
                      }
                    </script>
                  </head>
                </html>
                """;
        ProductPageScraperService scraper = new ProductPageScraperService(
                new StaticHttpFetcher(new ScraperProperties(), html),
                new ObjectMapper()
        );

        ProductPrice productPrice = scraper.scrape("https://www.bog-ide.dk/products/example");

        assertThat(productPrice.normalPrice()).hasToString("199.95");
        assertThat(productPrice.specialOfferPrice()).hasToString("129.95");
        assertThat(productPrice.price()).hasToString("129.95");
    }

    @Test
    void detectsNormalPriceFromPageWhenJsonLdOnlyContainsOfferPrice() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                      {
                        "@type": "Product",
                        "sku": "12345",
                        "gtin13": "9788711477960",
                        "offers": {"price": "129.95", "priceCurrency": "DKK"}
                      }
                    </script>
                  </head>
                  <body><span class="price--old">Før 199,95 kr.</span></body>
                </html>
                """;
        ProductPageScraperService scraper = new ProductPageScraperService(
                new StaticHttpFetcher(new ScraperProperties(), html),
                new ObjectMapper()
        );

        ProductPrice productPrice = scraper.scrape("https://www.bog-ide.dk/products/example");

        assertThat(productPrice.normalPrice()).hasToString("199.95");
        assertThat(productPrice.specialOfferPrice()).hasToString("129.95");
    }

    @Test
    void doesNotTreatAggregateHighPriceAsARegularPrice() {
        String html = """
                <html>
                  <head>
                    <script type="application/ld+json">
                      {
                        "@type": "Product",
                        "sku": "12345",
                        "gtin13": "9788711477960",
                        "offers": {
                          "@type": "AggregateOffer",
                          "price": "129.95",
                          "highPrice": "199.95",
                          "priceCurrency": "DKK"
                        }
                      }
                    </script>
                  </head>
                </html>
                """;
        ProductPageScraperService scraper = new ProductPageScraperService(
                new StaticHttpFetcher(new ScraperProperties(), html),
                new ObjectMapper()
        );

        ProductPrice productPrice = scraper.scrape("https://www.bog-ide.dk/products/example");

        assertThat(productPrice.normalPrice()).hasToString("129.95");
        assertThat(productPrice.specialOfferPrice()).isNull();
    }
}
