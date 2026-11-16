package com.nexocrm.service;

import com.nexocrm.domain.Activity;
import com.nexocrm.repository.ActivityRepository;
import com.nexocrm.web.dto.ActivityDto;
import com.nexocrm.web.dto.ActivityRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activities;
    private final CustomerService customers;
    private final DealService deals;
    private final SalesRepService salesReps;

    public ActivityService(ActivityRepository activities, CustomerService customers, DealService deals,
                           SalesRepService salesReps) {
        this.activities = activities;
        this.customers = customers;
        this.deals = deals;
        this.salesReps = salesReps;
    }

    public List<ActivityDto> search(Boolean completed, Long customerId) {
        return activities.search(completed, customerId).stream().map(ActivityDto::from).toList();
    }

    @Transactional
    public ActivityDto create(ActivityRequest request) {
        Activity activity = new Activity(request.type(), request.subject().trim(), request.dueAt());
        apply(activity, request);
        return ActivityDto.from(activities.save(activity));
    }

    @Transactional
    public ActivityDto update(Long id, ActivityRequest request) {
        Activity activity = get(id);
        activity.setType(request.type());
        activity.setSubject(request.subject().trim());
        activity.setDueAt(request.dueAt());
        apply(activity, request);
        return ActivityDto.from(activity);
    }

    @Transactional
    public ActivityDto toggleComplete(Long id) {
        Activity activity = get(id);
        if (activity.isCompleted()) {
            activity.reopen();
        } else {
            activity.complete(LocalDateTime.now());
        }
        return ActivityDto.from(activity);
    }

    @Transactional
    public void delete(Long id) {
        activities.delete(get(id));
    }

    private Activity get(Long id) {
        return activities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Actividad", id));
    }

    private void apply(Activity activity, ActivityRequest request) {
        activity.setDescription(request.description());
        var deal = request.dealId() != null ? deals.get(request.dealId()) : null;
        activity.setDeal(deal);
        if (request.customerId() != null) {
            activity.setCustomer(customers.get(request.customerId()));
        } else {
            activity.setCustomer(deal != null ? deal.getCustomer() : null);
        }
        activity.setOwner(salesReps.getOrNull(request.ownerId()));
    }
}
