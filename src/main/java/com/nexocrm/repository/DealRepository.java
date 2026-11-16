package com.nexocrm.repository;

import com.nexocrm.domain.Deal;
import com.nexocrm.domain.DealStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long> {

    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("""
            select d from Deal d
            join d.customer c
            left join d.owner o
            where (:stage is null or d.stage = :stage)
              and (:ownerId is null or o.id = :ownerId)
              and (:customerId is null or c.id = :customerId)
              and (lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(c.companyName) like lower(concat('%', :q, '%')))
            order by d.stageChangedAt desc
            """)
    List<Deal> search(@Param("q") String q,
                      @Param("stage") DealStage stage,
                      @Param("ownerId") Long ownerId,
                      @Param("customerId") Long customerId);

    /** Todas las oportunidades con sus relaciones cargadas: base de la analítica. */
    @EntityGraph(attributePaths = {"customer", "owner"})
    @Query("select d from Deal d")
    List<Deal> findAllWithRelations();

    List<Deal> findByCustomerId(Long customerId);
}
