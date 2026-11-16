package com.nexocrm.web;

import com.nexocrm.domain.DealStage;
import com.nexocrm.service.DealService;
import com.nexocrm.web.dto.DealDto;
import com.nexocrm.web.dto.DealRequest;
import com.nexocrm.web.dto.StageChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deals")
@Tag(name = "Oportunidades")
public class DealController {

    private final DealService service;

    public DealController(DealService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista oportunidades filtrando por texto, etapa, vendedor o cliente")
    public List<DealDto> search(@RequestParam(required = false) String q,
                                @RequestParam(required = false) DealStage stage,
                                @RequestParam(required = false) Long ownerId,
                                @RequestParam(required = false) Long customerId) {
        return service.search(q, stage, ownerId, customerId);
    }

    @GetMapping("/{id}")
    public DealDto get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealDto create(@Valid @RequestBody DealRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DealDto update(@PathVariable Long id, @Valid @RequestBody DealRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Mueve la oportunidad de etapa (dispara webhooks)")
    public DealDto moveStage(@PathVariable Long id, @Valid @RequestBody StageChangeRequest request) {
        return service.moveStage(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
