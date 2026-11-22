package com.csbet.backend.market;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import static com.csbet.backend.market.MarketDtos.*;

@RestController
public class MarketController {
    private final MarketQueryService service;
    public MarketController(MarketQueryService service) { this.service = service; }
    @GetMapping("/api/skins/{id}/market-prices")
    public List<QuoteView> skin(@PathVariable String id) { return service.forSkin(id); }
    @GetMapping("/api/cases/{id}/market-price")
    public QuoteView box(@PathVariable String id) { return service.forCase(id); }
    @GetMapping("/api/market/variants/{id}/history")
    public HistoryPage history(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "30") int size) { return service.history(id, page, size); }
}
