package com.nexocrm.integration.webhook;

import com.nexocrm.repository.CustomerRepository;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.web.dto.CustomerDto;
import com.nexocrm.web.dto.DealDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Carga los datos de negocio en su propia transacción (los listeners corren tras el commit, en otro hilo). */
@Component
@Transactional(readOnly = true)
public class WebhookPayloadFactory {

    private final DealRepository deals;
    private final CustomerRepository customers;

    public WebhookPayloadFactory(DealRepository deals, CustomerRepository customers) {
        this.deals = deals;
        this.customers = customers;
    }

    public Optional<DealDto> deal(Long id) {
        return deals.findById(id).map(DealDto::from);
    }

    public Optional<CustomerDto> customer(Long id) {
        return customers.findById(id).map(CustomerDto::from);
    }
}
