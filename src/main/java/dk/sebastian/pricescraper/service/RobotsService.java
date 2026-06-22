package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.config.ScraperProperties;
import dk.sebastian.pricescraper.records.RobotsRule;
import dk.sebastian.pricescraper.records.RobotsRules;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RobotsService {

    private final HttpFetcherService httpFetcher;
    private final ScraperProperties properties;
    private volatile RobotsRules cachedRules;

    public RobotsService(HttpFetcherService httpFetcher, ScraperProperties properties) {
        this.httpFetcher = httpFetcher;
        this.properties = properties;
    }

    public void verifyAllowed(String url) {
        if (!isAllowed(url)) {
            throw new IllegalStateException("robots.txt does not allow fetching " + url);
        }
    }

    public boolean isAllowed(String url) {
        RobotsRules rules = cachedRules;
        if (rules == null) {
            rules = parse(httpFetcher.fetch(properties.getRobotsTxtUrl()), userAgentToken(properties.getUserAgent()));
            cachedRules = rules;
        }

        return rules.isAllowed(pathWithQuery(url));
    }

    static RobotsRules parse(String robotsTxt, String userAgentToken) {
        List<RobotsRule> selectedRules = new ArrayList<>();
        List<String> currentAgents = new ArrayList<>();
        List<RobotsRule> currentRules = new ArrayList<>();

        for (String rawLine : robotsTxt.split("\\R")) {
            String line = stripComment(rawLine).trim();
            if (line.isBlank()) {
                if (matchesUserAgent(currentAgents, userAgentToken)) {
                    selectedRules.addAll(currentRules);
                }
                currentAgents.clear();
                currentRules.clear();
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                continue;
            }

            String key = line.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colonIndex + 1).trim();

            if ("user-agent".equals(key)) {
                if (!currentRules.isEmpty()) {
                    if (matchesUserAgent(currentAgents, userAgentToken)) {
                        selectedRules.addAll(currentRules);
                    }
                    currentAgents.clear();
                    currentRules.clear();
                }
                currentAgents.add(value.toLowerCase(Locale.ROOT));
            } else if ("allow".equals(key) || "disallow".equals(key)) {
                currentRules.add(new RobotsRule("allow".equals(key), value));
            }
        }

        if (matchesUserAgent(currentAgents, userAgentToken)) {
            selectedRules.addAll(currentRules);
        }

        return new RobotsRules(selectedRules);
    }

    private static boolean matchesUserAgent(List<String> agents, String userAgentToken) {
        if (agents.isEmpty()) {
            return false;
        }

        String normalizedToken = userAgentToken.toLowerCase(Locale.ROOT);
        return agents.stream().anyMatch(agent -> "*".equals(agent) || normalizedToken.contains(agent));
    }

    private static String userAgentToken(String userAgent) {
        int separator = userAgent.indexOf('/');
        String token = separator >= 0 ? userAgent.substring(0, separator) : userAgent;
        return token.trim();
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }

    private static String pathWithQuery(String url) {
        URI uri = URI.create(url);
        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
        return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
    }
}
