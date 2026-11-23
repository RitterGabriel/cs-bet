package com.csbet.backend.inventory;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/inventory")
public class InventorySaleController {
    private final InventorySaleService service;
    public InventorySaleController(InventorySaleService service) { this.service = service; }
    @PostMapping("/{itemId}/sell") public InventorySaleResponse sell(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId, @Valid @RequestBody InventorySaleRequest request) {
        return service.sell(Long.valueOf(jwt.getSubject()), itemId, request);
    }
}
