package com.csbet.backend.statistics;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics/me")
public class PersonalStatisticsController {
    private final PersonalStatisticsService service;
    public PersonalStatisticsController(PersonalStatisticsService service) { this.service = service; }
    @GetMapping
    public PersonalStatisticsResponse me(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") ActivityType type) {
        return service.read(Long.valueOf(jwt.getSubject()), page, size, type);
    }
}
