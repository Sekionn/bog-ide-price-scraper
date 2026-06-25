package dk.sebastian.pricescraper.controller;

import dk.sebastian.pricescraper.dto.ApiIndexDto;
import dk.sebastian.pricescraper.service.ApiIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "API index", description = "Manual API discovery endpoint.")
public class ApiIndexController {

    private final ApiIndexService apiIndexService;

    public ApiIndexController(ApiIndexService apiIndexService) {
        this.apiIndexService = apiIndexService;
    }

    @GetMapping("/api")
    @Operation(summary = "Get API index", description = "Returns links, examples, constraints, and endpoint descriptions.")
    public EntityModel<ApiIndexDto> index() {
        return apiIndexService.index();
    }
}
