package dk.sebastian.pricescraper.scraper;

public class HttpFetchException extends IllegalStateException {

    private final String url;
    private final int statusCode;

    public HttpFetchException(String message, String url, int statusCode) {
        super(message);
        this.url = url;
        this.statusCode = statusCode;
    }

    public HttpFetchException(String message, String url, Throwable cause) {
        super(message, cause);
        this.url = url;
        this.statusCode = 0;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
