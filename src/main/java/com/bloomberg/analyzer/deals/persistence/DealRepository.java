package com.bloomberg.analyzer.deals.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DealRepository extends JpaRepository<DealEntity, Long> {
    boolean existsByDealId(String dealId);
}


