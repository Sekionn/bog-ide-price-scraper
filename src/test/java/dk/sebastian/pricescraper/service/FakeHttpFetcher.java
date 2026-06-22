package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.scraper.HttpFetchException;

import java.util.Map;

class FakeHttpFetcher extends HttpFetcherService {

    private final Map<String, String> responses;

    FakeHttpFetcher(ScraperProperties properties, Map<String, String> responses) {
        super(properties);
        this.responses = responses;
    }

    @Override
    public String fetch(String url) {
        String response = responses.get(url);
        if (response == null) {
            throw new HttpFetchException("No fake response for " + url, url, 404);
        }
        return response;
    }
}
