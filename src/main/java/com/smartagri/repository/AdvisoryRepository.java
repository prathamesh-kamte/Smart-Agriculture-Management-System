package com.smartagri.repository;

import com.smartagri.entity.Advisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {
    
    List<Advisory> findByFarmerIdAndAcknowledgedFalse(Long farmerId);
    
    List<Advisory> findByCropId(Long cropId);

    long countByFarmerIdAndAcknowledgedFalse(Long farmerId);

    long countByAcknowledgedFalse();

    @Query("SELECT a FROM Advisory a WHERE a.farmer.email = :email AND a.acknowledged = false " +
           "AND (:severity IS NULL OR a.severity = :severity)")
    org.springframework.data.domain.Page<Advisory> findActiveByFarmerEmailAndFilters(
            @Param("email") String email,
            @Param("severity") String severity,
            org.springframework.data.domain.Pageable pageable);
}
