package com.nexocrm.repository;

import com.nexocrm.domain.Activity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @EntityGraph(attributePaths = {"customer", "deal", "owner"})
    @Query("""
            select a from Activity a
            left join a.customer c
            where (:completed is null or a.completed = :completed)
              and (:customerId is null or c.id = :customerId)
            order by a.completed asc, a.dueAt asc
            """)
    List<Activity> search(@Param("completed") Boolean completed, @Param("customerId") Long customerId);

    List<Activity> findByDealId(Long dealId);

    List<Activity> findByCustomerId(Long customerId);
}
