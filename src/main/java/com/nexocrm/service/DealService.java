package com.nexocrm.service;

import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.domain.Deal;
import com.nexocrm.domain.DealStage;
import com.nexocrm.repository.ActivityRepository;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.service.event.DealStageChangedEvent;
import com.nexocrm.web.dto.DealDto;
import com.nexocrm.web.dto.DealRequest;
import com.nexocrm.web.dto.StageChangeRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DealService {

    private final DealRepository deals;
    private final ActivityRepository activities;
    private final CustomerService customers;
    private final SalesRepService salesReps;
    private final ApplicationEventPublisher events;

    public DealService(DealRepository deals, ActivityRepository activities, CustomerService customers,
                       SalesRepService salesReps, ApplicationEventPublisher events) {
        this.deals = deals;
        this.activities = activities;
        this.customers = customers;
        this.salesReps = salesReps;
        this.events = events;
    }

    public List<DealDto> search(String q, DealStage stage, Long ownerId, Long customerId) {
        String term = q == null ? "" : q.trim();
        return deals.search(term, stage, ownerId, customerId).stream().map(DealDto::from).toList();
    }

    public DealDto findById(Long id) {
        return DealDto.from(get(id));
    }

    @Transactional
    public DealDto create(DealRequest request) {
        var customer = customers.get(request.customerId());
        Deal deal = new Deal(request.title().trim(), customer, salesReps.getOrNull(request.ownerId()), request.amount());
        deal.setExpectedCloseDate(request.expectedCloseDate());
        DealStage stage = request.stage() != null ? request.stage() : DealStage.LEAD;
        if (stage != DealStage.LEAD) {
            deal.moveTo(stage, null, LocalDateTime.now());
        }
        if (request.probability() != null && stage.isOpen()) {
            deal.setProbability(request.probability());
        }
        Deal saved = deals.save(deal);
        promoteCustomerIfWon(saved);
        return DealDto.from(saved);
    }

    @Transactional
    public DealDto update(Long id, DealRequest request) {
        Deal deal = get(id);
        DealStage previous = deal.getStage();
        deal.setTitle(request.title().trim());
        deal.setCustomer(customers.get(request.customerId()));
        deal.setOwner(salesReps.getOrNull(request.ownerId()));
        deal.setAmount(request.amount());
        deal.setExpectedCloseDate(request.expectedCloseDate());
        if (request.stage() != null && request.stage() != previous) {
            changeStage(deal, request.stage(), null);
        }
        if (request.probability() != null && deal.getStage().isOpen()) {
            deal.setProbability(request.probability());
        }
        return DealDto.from(deal);
    }

    /** Usado por el tablero Kanban al arrastrar una tarjeta entre columnas. */
    @Transactional
    public DealDto moveStage(Long id, StageChangeRequest request) {
        Deal deal = get(id);
        if (request.stage() == DealStage.LOST && (request.lostReason() == null || request.lostReason().isBlank())) {
            throw new BusinessRuleException("Indica el motivo por el que se perdió la oportunidad");
        }
        changeStage(deal, request.stage(), request.lostReason());
        return DealDto.from(deal);
    }

    @Transactional
    public void delete(Long id) {
        Deal deal = get(id);
        activities.findByDealId(id).forEach(a -> a.setDeal(null));
        deals.delete(deal);
    }

    Deal get(Long id) {
        return deals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Oportunidad", id));
    }

    private void changeStage(Deal deal, DealStage target, String reason) {
        DealStage from = deal.getStage();
        if (from == target) {
            return;
        }
        deal.moveTo(target, reason, LocalDateTime.now());
        promoteCustomerIfWon(deal);
        events.publishEvent(new DealStageChangedEvent(deal.getId(), from, target));
    }

    /** Un cliente que nos compra deja de ser lead y pasa a activo. */
    private void promoteCustomerIfWon(Deal deal) {
        if (deal.getStage() == DealStage.WON && deal.getCustomer().getStatus() != CustomerStatus.ACTIVE) {
            deal.getCustomer().setStatus(CustomerStatus.ACTIVE);
        }
    }
}
