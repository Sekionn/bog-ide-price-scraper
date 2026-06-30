package dk.sebastian.pricescraper.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class ProductPriceDto {

    private final String id;
    private final String url;
    private final String productNumber;
    private final String eanNumber;
    private final String title;
    private final String author;
    private final BigDecimal price;
    private final boolean specialOffer;

    @JsonCreator
    public ProductPriceDto(
            @JsonProperty("id") String id,
            @JsonProperty("url") String url,
            @JsonProperty("productNumber") String productNumber,
            @JsonProperty("eanNumber") String eanNumber,
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("specialOffer") boolean specialOffer
    ) {
        this.id = id;
        this.url = url;
        this.productNumber = productNumber;
        this.eanNumber = eanNumber;
        this.title = title;
        this.author = author;
        this.price = price;
        this.specialOffer = specialOffer;
    }

    public ProductPriceDto(
            String id,
            String url,
            String productNumber,
            String eanNumber,
            String title,
            String author,
            BigDecimal price
    ) {
        this(id, url, productNumber, eanNumber, title, author, price, false);
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getProductNumber() {
        return productNumber;
    }

    public String getEanNumber() {
        return eanNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isSpecialOffer() {
        return specialOffer;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductPriceDto that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
