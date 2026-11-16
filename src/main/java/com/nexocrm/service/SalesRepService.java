package com.nexocrm.service;

import com.nexocrm.domain.SalesRep;
import com.nexocrm.repository.SalesRepRepository;
import com.nexocrm.web.dto.SalesRepDto;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesRepService {

    private final SalesRepRepository repository;

    public SalesRepService(SalesRepRepository repository) {
        this.repository = repository;
    }

    public List<SalesRepDto> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(SalesRepDto::from).toList();
    }

    SalesRep getOrNull(Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendedor", id));
    }
}
