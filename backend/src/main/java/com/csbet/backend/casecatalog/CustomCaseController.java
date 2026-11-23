package com.csbet.backend.casecatalog;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/custom-cases")
public class CustomCaseController {
    private final CustomCaseService service;
    public CustomCaseController(CustomCaseService service){this.service=service;}
    @GetMapping public CustomCaseDtos.Page list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="")String search,
            @RequestParam(defaultValue="featured")String sort){return service.list(page,search,sort);}
    @GetMapping("/{id}")public CustomCaseDtos.Detail detail(@PathVariable String id){return service.detail(id);}
}
