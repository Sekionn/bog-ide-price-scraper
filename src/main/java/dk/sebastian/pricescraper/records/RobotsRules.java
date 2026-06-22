package dk.sebastian.pricescraper.records;

import java.util.List;

public record RobotsRules(List<RobotsRule> rules) {

    public boolean isAllowed(String path) {
        RobotsRule bestMatch = null;
        for (RobotsRule rule : rules) {
            if (rule.path().isEmpty() || !path.startsWith(rule.path())) {
                continue;
            }

            if (bestMatch == null
                    || rule.path().length() > bestMatch.path().length()
                    || rule.path().length() == bestMatch.path().length() && rule.allow() && !bestMatch.allow()) {
                bestMatch = rule;
            }
        }

        return bestMatch == null || bestMatch.allow();
    }
}
