package com.smartagri.repository;

import com.smartagri.domain.entity.DiseaseDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link DiseaseDetection} entities.
 */
@Repository
public interface DiseaseDetectionRepository extends JpaRepository<DiseaseDetection, Long> {

    /**
     * All detections for a given farmer, newest first.
     *
     * @param farmerId the farmer's primary key
     * @return list of detections; never {@code null}
     */
    List<DiseaseDetection> findByFarmerIdOrderByDetectedAtDesc(Long farmerId);

    /**
     * All detections associated with a specific crop, newest first.
     *
     * @param cropId the crop's primary key
     * @return list of detections; never {@code null}
     */
    List<DiseaseDetection> findByCropIdOrderByDetectedAtDesc(Long cropId);
}
