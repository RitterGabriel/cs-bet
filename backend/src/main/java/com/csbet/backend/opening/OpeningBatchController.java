package com.csbet.backend.opening;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/openings/batch")
public class OpeningBatchController {
    private final OpeningBatchService service;
    public OpeningBatchController(OpeningBatchService service) { this.service = service; }
    @PostMapping public OpeningBatchResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpeningBatchRequest request) {
        return service.open(Long.valueOf(jwt.getSubject()), request);
    }
}
