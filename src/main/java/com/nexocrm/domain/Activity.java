package com.nexocrm.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities", indexes = @Index(name = "idx_activity_due", columnList = "dueAt"))
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    private boolean completed;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private SalesRep owner;

    protected Activity() {
    }

    public Activity(ActivityType type, String subject, LocalDateTime dueAt) {
        this.type = type;
        this.subject = subject;
        this.dueAt = dueAt;
    }

    public void complete(LocalDateTime when) {
        this.completed = true;
        this.completedAt = when;
    }

    public void reopen() {
        this.completed = false;
        this.completedAt = null;
    }

    public Long getId() { return id; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Deal getDeal() { return deal; }
    public void setDeal(Deal deal) { this.deal = deal; }
    public SalesRep getOwner() { return owner; }
    public void setOwner(SalesRep owner) { this.owner = owner; }
}
