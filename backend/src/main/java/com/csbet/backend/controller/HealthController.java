package com.csbet.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/api/health")
	public HealthResponse health() {
		return new HealthResponse("UP", "CS Bet");
	}

	private record HealthResponse(String status, String application) {
	}
}
