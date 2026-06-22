package dk.sebastian.pricescraper.records;

public record ApiErrorDto(
        String error,
        String message
) {
}
