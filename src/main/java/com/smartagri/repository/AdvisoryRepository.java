package com.smartagri.repository;

import com.smartagri.entity.Advisory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryRepository extends JpaRepository<Advisory, Long> {
    
    List<Advisory> findByFarmerIdAndAcknowledgedFalse(Long farmerId);
    
    List<Advisory> findByCropId(Long cropId);

    long countByFarmerIdAndAcknowledgedFalse(Long farmerId);

    long countByAcknowledgedFalse();
}
