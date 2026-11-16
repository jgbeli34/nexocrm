package com.nexocrm.web;

import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.service.CustomerService;
import com.nexocrm.web.dto.CustomerDto;
import com.nexocrm.web.dto.CustomerRequest;
import com.nexocrm.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Clientes")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Busca clientes con filtros y paginación")
    public PageResponse<CustomerDto> search(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) CustomerStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(defaultValue = "name") String sort) {
        return service.search(q, status, page, size, sort);
    }

    @GetMapping("/all")
    @Operation(summary = "Lista completa (para selectores)")
    public List<CustomerDto> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@Valid @RequestBody CustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
