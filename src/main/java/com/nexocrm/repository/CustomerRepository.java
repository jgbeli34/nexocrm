package com.nexocrm.repository;

import com.nexocrm.domain.Customer;
import com.nexocrm.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select c from Customer c
            where (:status is null or c.status = :status)
              and (lower(c.companyName) like lower(concat('%', :q, '%'))
                   or lower(c.contactName) like lower(concat('%', :q, '%'))
                   or lower(c.email) like lower(concat('%', :q, '%'))
                   or lower(c.city) like lower(concat('%', :q, '%')))
            """)
    Page<Customer> search(@Param("q") String q, @Param("status") CustomerStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<Customer> findAllByOrderByCompanyNameAsc();

    boolean existsByEmailIgnoreCase(String email);

    long countByStatus(CustomerStatus status);
}
