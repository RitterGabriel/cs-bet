package com.csbet.backend.inventory;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import static com.csbet.backend.inventory.InventoryResponse.InventoryPage;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/me")
    public InventoryPage me(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "12") int size,
                            @RequestParam(required = false) InventoryStatus status) {
        return service.findOwned(Long.valueOf(jwt.getSubject()), page, size, status);
    }
}
