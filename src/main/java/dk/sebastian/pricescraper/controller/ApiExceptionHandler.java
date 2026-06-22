package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.records.ApiErrorDto;
import dk.sebastian.pricescraper.service.ApiErrorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final ApiErrorService apiErrorService;

    public ApiExceptionHandler(ApiErrorService apiErrorService) {
        this.apiErrorService = apiErrorService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorDto> handleIllegalArgument(IllegalArgumentException exception) {
        return apiErrorService.badRequest(exception);
    }
}
