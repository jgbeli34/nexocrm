package com.nexocrm.web;

import com.nexocrm.service.ActivityService;
import com.nexocrm.web.dto.ActivityDto;
import com.nexocrm.web.dto.ActivityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Actividades")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityDto> search(@RequestParam(required = false) Boolean completed,
                                    @RequestParam(required = false) Long customerId) {
        return service.search(completed, customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityDto create(@Valid @RequestBody ActivityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ActivityDto update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Marca la actividad como hecha o la reabre")
    public ActivityDto toggle(@PathVariable Long id) {
        return service.toggleComplete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
