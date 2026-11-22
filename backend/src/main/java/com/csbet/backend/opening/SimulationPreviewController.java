package com.csbet.backend.opening;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
public class SimulationPreviewController {
    private final SimulationPreviewService service;
    public SimulationPreviewController(SimulationPreviewService service) { this.service = service; }
    @PostMapping("/preview")
    public SimulationPreviewResponse preview(@Valid @RequestBody SimulationPreviewRequest request) {
        return service.preview(request);
    }
}
