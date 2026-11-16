package com.nexocrm.repository;

import com.nexocrm.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    @EntityGraph(attributePaths = "subscription")
    List<WebhookDelivery> findTop30ByOrderByDeliveredAtDesc();

    void deleteBySubscriptionId(Long subscriptionId);
}
