package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.records.ApiIndexDto;
import dk.sebastian.pricescraper.service.ApiIndexService;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiIndexController {

    private final ApiIndexService apiIndexService;

    public ApiIndexController(ApiIndexService apiIndexService) {
        this.apiIndexService = apiIndexService;
    }

    @GetMapping("/api")
    public EntityModel<ApiIndexDto> index() {
        return apiIndexService.index();
    }
}
