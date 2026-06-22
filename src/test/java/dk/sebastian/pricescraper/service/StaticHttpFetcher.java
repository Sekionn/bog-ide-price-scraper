package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;

class StaticHttpFetcher extends HttpFetcherService {

    private final String response;

    StaticHttpFetcher(ScraperProperties properties, String response) {
        super(properties);
        this.response = response;
    }

    @Override
    public String fetch(String url) {
        return response;
    }
}
