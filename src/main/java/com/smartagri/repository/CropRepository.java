package com.smartagri.repository;

import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.enums.CropStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Crop} entities.
 */
@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {

    /** All crops belonging to a specific farmer. */
    List<Crop> findByFarmerId(Long farmerId);

    /** Crops belonging to a farmer filtered by status. */
    List<Crop> findByFarmerIdAndStatus(Long farmerId, CropStatus status);

    /** Active (non-harvested / non-failed) crops for a farmer. */
    @Query("SELECT c FROM Crop c WHERE c.farmer.id = :farmerId " +
           "AND c.status NOT IN (com.smartagri.domain.enums.CropStatus.HARVESTED, " +
           "                     com.smartagri.domain.enums.CropStatus.FAILED)")
    List<Crop> findActiveCropsByFarmerId(Long farmerId);

    /** All crops with a given status (admin / scheduler use). */
    List<Crop> findByStatus(CropStatus status);
}
