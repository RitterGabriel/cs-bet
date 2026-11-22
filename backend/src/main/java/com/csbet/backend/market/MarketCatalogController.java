package com.csbet.backend.market;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market/skins")
public class MarketCatalogController {
    private final MarketCatalogService service;
    public MarketCatalogController(MarketCatalogService service) { this.service=service; }
    @GetMapping
    public MarketCatalogDtos.SkinPage list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="24") int size,
            @RequestParam(defaultValue="") String search, @RequestParam(defaultValue="") String weapon,
            @RequestParam(defaultValue="") String rarity, @RequestParam(defaultValue="") String wear,
            @RequestParam(defaultValue="name") String sort) {
        return service.list(page,size,search,weapon,rarity,wear,sort);
    }
}
