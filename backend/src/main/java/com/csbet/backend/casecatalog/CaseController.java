package com.csbet.backend.casecatalog;

import org.springframework.web.bind.annotation.*;
import static com.csbet.backend.casecatalog.CatalogDtos.*;

@RestController
@RequestMapping("/api/cases")
public class CaseController {
    private final CaseCatalogService service;
    public CaseController(CaseCatalogService service) { this.service = service; }
    @GetMapping
    public CasePage list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
                         @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "official") String category,
                         @RequestParam(defaultValue = "featured") String sort) {
        return service.list(page, size, search, category, sort);
    }
    @GetMapping("/{id}")
    public CaseDetail detail(@PathVariable String id) { return service.detail(id); }
}
