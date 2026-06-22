package dk.sebastian.pricescraper.service;

import dk.sebastian.pricescraper.records.ApiErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ApiErrorService {

    public ResponseEntity<ApiErrorDto> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiErrorDto(
                "Bad Request",
                exception.getMessage()
        ));
    }
}
