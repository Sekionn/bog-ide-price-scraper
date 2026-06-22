package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.records.ScrapeRunResponseDto;
import dk.sebastian.pricescraper.records.ScrapeStatusDto;
import dk.sebastian.pricescraper.service.ScrapeApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScrapeController {

    private final ScrapeApiService scrapeApiService;

    public ScrapeController(ScrapeApiService scrapeApiService) {
        this.scrapeApiService = scrapeApiService;
    }

    @PostMapping("/run")
    public ResponseEntity<ScrapeRunResponseDto> runNow() {
        return scrapeApiService.runNow();
    }

    @GetMapping("/status")
    public ScrapeStatusDto status() {
        return scrapeApiService.status();
    }
}
