package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.dto.ScrapeRunResponseDto;
import dk.sebastian.pricescraper.dto.ScrapeStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ScrapeApiService {

    private final WeeklyScrapeJobService weeklyScrapeJob;

    public ScrapeApiService(WeeklyScrapeJobService weeklyScrapeJob) {
        this.weeklyScrapeJob = weeklyScrapeJob;
    }

    public ResponseEntity<ScrapeRunResponseDto> runNow() {
        if (weeklyScrapeJob.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ScrapeRunResponseDto(
                    false,
                    "Scraper is already running"
            ));
        }

        CompletableFuture.runAsync(weeklyScrapeJob::runOnce);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ScrapeRunResponseDto(
                true,
                "Scrape started. Check /api/scrape/status for progress."
        ));
    }

    public ScrapeStatusDto status() {
        return new ScrapeStatusDto(
                weeklyScrapeJob.isRunning(),
                weeklyScrapeJob.getLastSummary(),
                weeklyScrapeJob.getLastError(),
                weeklyScrapeJob.getLastFailedUrl(),
                weeklyScrapeJob.getLastFailedStatusCode()
        );
    }
}
