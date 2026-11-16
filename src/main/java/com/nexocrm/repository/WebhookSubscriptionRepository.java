package com.nexocrm.repository;

import com.nexocrm.domain.WebhookEvent;
import com.nexocrm.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByEventAndActiveTrue(WebhookEvent event);
}
