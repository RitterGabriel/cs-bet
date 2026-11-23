package com.csbet.backend.inventory;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/inventory/sell-all")
public class BulkInventorySaleController {
    private final BulkInventorySaleService service;
    public BulkInventorySaleController(BulkInventorySaleService service) { this.service=service; }
    @GetMapping("/quote") public BulkSaleDtos.Quote quote(@AuthenticationPrincipal Jwt jwt) { return service.quote(Long.valueOf(jwt.getSubject())); }
    @PostMapping public BulkSaleDtos.Receipt sell(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody BulkSaleDtos.Request request) {
        return service.sell(Long.valueOf(jwt.getSubject()),request);
    }
}
