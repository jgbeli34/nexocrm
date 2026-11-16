package com.nexocrm.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Oportunidad de venta. Los montos se almacenan en USD. */
@Entity
@Table(name = "deals", indexes = {
        @Index(name = "idx_deal_stage", columnList = "stage"),
        @Index(name = "idx_deal_closed_at", columnList = "closedAt")
})
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private SalesRep owner;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DealStage stage = DealStage.LEAD;

    @Column(nullable = false)
    private int probability = DealStage.LEAD.getDefaultProbability();

    private LocalDate expectedCloseDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Fecha del último cambio de etapa; sirve para medir el envejecimiento. */
    private LocalDateTime stageChangedAt;

    private LocalDateTime closedAt;

    @Column(length = 300)
    private String lostReason;

    protected Deal() {
    }

    public Deal(String title, Customer customer, SalesRep owner, BigDecimal amount) {
        this.title = title;
        this.customer = customer;
        this.owner = owner;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (stageChangedAt == null) {
            stageChangedAt = createdAt;
        }
    }

    /**
     * Mueve la oportunidad a una nueva etapa aplicando las reglas de negocio:
     * probabilidad por defecto, fecha de cierre y motivo de pérdida.
     */
    public void moveTo(DealStage newStage, String reason, LocalDateTime when) {
        if (newStage == this.stage) {
            return;
        }
        this.stage = newStage;
        this.probability = newStage.getDefaultProbability();
        this.stageChangedAt = when;
        if (newStage.isClosed()) {
            this.closedAt = when;
            this.lostReason = newStage == DealStage.LOST ? reason : null;
        } else {
            this.closedAt = null;
            this.lostReason = null;
        }
    }

    /** Valor esperado = monto × probabilidad. */
    public BigDecimal weightedAmount() {
        return amount.multiply(BigDecimal.valueOf(probability)).divide(BigDecimal.valueOf(100));
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public SalesRep getOwner() { return owner; }
    public void setOwner(SalesRep owner) { this.owner = owner; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public DealStage getStage() { return stage; }
    public int getProbability() { return probability; }
    public void setProbability(int probability) { this.probability = probability; }
    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate expectedCloseDate) { this.expectedCloseDate = expectedCloseDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getStageChangedAt() { return stageChangedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getLostReason() { return lostReason; }
}
