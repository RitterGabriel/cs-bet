package com.csbet.backend.pricing;
import org.springframework.web.bind.annotation.*;
import static com.csbet.backend.pricing.PriceDtos.*;

@RestController
@RequestMapping("/api/skins/{id}/price")
public class PriceController {
    private final PriceQueryService service;
    public PriceController(PriceQueryService service) { this.service = service; }
    @GetMapping
    public CurrentPrice current(@PathVariable String id) { return service.current(id); }
    @GetMapping("/history")
    public HistoryPage history(@PathVariable String id, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "30") int size) { return service.history(id, page, size); }
}
