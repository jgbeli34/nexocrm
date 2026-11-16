package com.nexocrm.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_reps")
public class SalesRep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** Meta mensual de ventas en USD. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyQuota = BigDecimal.ZERO;

    protected SalesRep() {
    }

    public SalesRep(String name, String email, BigDecimal monthlyQuota) {
        this.name = name;
        this.email = email;
        this.monthlyQuota = monthlyQuota;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getMonthlyQuota() { return monthlyQuota; }
    public void setMonthlyQuota(BigDecimal monthlyQuota) { this.monthlyQuota = monthlyQuota; }
}
