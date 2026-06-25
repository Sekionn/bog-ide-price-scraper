package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.dto.ScrapeRunResponseDto;
import dk.sebastian.pricescraper.dto.ScrapeStatusDto;
import dk.sebastian.pricescraper.service.ScrapeApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
@Tag(name = "Scrape runs", description = "Start and inspect scraper refresh jobs.")
public class ScrapeController {

    private final ScrapeApiService scrapeApiService;

    public ScrapeController(ScrapeApiService scrapeApiService) {
        this.scrapeApiService = scrapeApiService;
    }

    @PostMapping("/run")
    @Operation(summary = "Start scraper refresh", description = "Starts a background refresh job for known database products.")
    public ResponseEntity<ScrapeRunResponseDto> runNow() {
        return scrapeApiService.runNow();
    }

    @GetMapping("/status")
    @Operation(summary = "Get scraper status", description = "Returns current refresh status, last summary, and last failure details.")
    public ScrapeStatusDto status() {
        return scrapeApiService.status();
    }
}
