package com.nexocrm.service;

import com.nexocrm.domain.Customer;
import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.domain.LeadSource;
import com.nexocrm.repository.ActivityRepository;
import com.nexocrm.repository.CustomerRepository;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.service.event.CustomerCreatedEvent;
import com.nexocrm.web.dto.CustomerDto;
import com.nexocrm.web.dto.CustomerRequest;
import com.nexocrm.web.dto.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final DealRepository deals;
    private final ActivityRepository activities;
    private final SalesRepService salesReps;
    private final ApplicationEventPublisher events;

    public CustomerService(CustomerRepository customers, DealRepository deals, ActivityRepository activities,
                           SalesRepService salesReps, ApplicationEventPublisher events) {
        this.customers = customers;
        this.deals = deals;
        this.activities = activities;
        this.salesReps = salesReps;
        this.events = events;
    }

    public PageResponse<CustomerDto> search(String q, CustomerStatus status, int page, int size, String sort) {
        Sort order = switch (sort == null ? "" : sort) {
            case "recent" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "city" -> Sort.by("city", "companyName");
            default -> Sort.by("companyName");
        };
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), order);
        String term = q == null ? "" : q.trim();
        return PageResponse.of(customers.search(term, status, pageable), CustomerDto::from);
    }

    public List<CustomerDto> findAll() {
        return customers.findAllByOrderByCompanyNameAsc().stream().map(CustomerDto::from).toList();
    }

    public CustomerDto findById(Long id) {
        return CustomerDto.from(get(id));
    }

    @Transactional
    public CustomerDto create(CustomerRequest request) {
        Customer customer = new Customer(request.companyName().trim(), request.contactName().trim(), request.email().trim());
        apply(customer, request);
        Customer saved = customers.save(customer);
        events.publishEvent(new CustomerCreatedEvent(saved.getId()));
        return CustomerDto.from(saved);
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        customer.setCompanyName(request.companyName().trim());
        customer.setContactName(request.contactName().trim());
        customer.setEmail(request.email().trim());
        apply(customer, request);
        return CustomerDto.from(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        if (!deals.findByCustomerId(id).isEmpty()) {
            throw new BusinessRuleException("No se puede eliminar un cliente con oportunidades asociadas");
        }
        activities.deleteAll(activities.findByCustomerId(id));
        customers.delete(customer);
    }

    Customer get(Long id) {
        return customers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setPhone(request.phone());
        customer.setIndustry(request.industry());
        customer.setCity(request.city());
        customer.setCountry(request.country());
        customer.setStatus(request.status());
        customer.setSource(request.source() != null ? request.source() : LeadSource.WEBSITE);
        customer.setOwner(salesReps.getOrNull(request.ownerId()));
        customer.setNotes(request.notes());
    }
}
