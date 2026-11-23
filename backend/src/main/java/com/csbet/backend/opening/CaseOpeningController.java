package com.csbet.backend.opening;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openings")
public class CaseOpeningController {
    private final CaseOpeningService service;
    public CaseOpeningController(CaseOpeningService service) { this.service = service; }
    @PostMapping
    public OpeningResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpeningRequest request) {
        return service.open(Long.valueOf(jwt.getSubject()), request);
    }
}
