package com.csbet.backend.wallet;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final WalletService service;
    public WalletController(WalletService service) { this.service = service; }
    @GetMapping
    public WalletResponse.Balance balance(@AuthenticationPrincipal Jwt jwt) {
        return service.balance(Long.valueOf(jwt.getSubject()));
    }
    @PostMapping("/credits")
    public WalletResponse.Balance add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCreditsRequest request) {
        return service.add(Long.valueOf(jwt.getSubject()), request);
    }
    @GetMapping("/movements")
    public WalletResponse.History history(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return service.history(Long.valueOf(jwt.getSubject()), page);
    }
}
