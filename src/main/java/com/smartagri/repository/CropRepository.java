package com.smartagri.repository;

import com.smartagri.entity.Crop;
import com.smartagri.entity.CropStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {

    List<Crop> findByFarmerId(Long farmerId);

    List<Crop> findByFarmerIdAndStatus(Long farmerId, CropStatus status);

    @Query("SELECT c FROM Crop c WHERE c.farmer.id = :farmerId AND c.status NOT IN (com.smartagri.entity.CropStatus.HARVESTED, com.smartagri.entity.CropStatus.FAILED)")
    List<Crop> findActiveCropsByFarmerId(@Param("farmerId") Long farmerId);

    List<Crop> findByStatus(CropStatus status);

    long countByFarmerId(Long farmerId);

    @Query("SELECT COUNT(c) FROM Crop c WHERE c.farmer.id = :farmerId AND c.status NOT IN (com.smartagri.entity.CropStatus.HARVESTED, com.smartagri.entity.CropStatus.FAILED)")
    long countActiveCropsByFarmerId(@Param("farmerId") Long farmerId);

    @Query("SELECT COUNT(c) FROM Crop c WHERE c.status NOT IN (com.smartagri.entity.CropStatus.HARVESTED, com.smartagri.entity.CropStatus.FAILED)")
    long countAllActiveCrops();

    @Query("SELECT c.status, COUNT(c) FROM Crop c WHERE c.farmer.id = :farmerId GROUP BY c.status")
    List<Object[]> countCropsByStatusForFarmer(@Param("farmerId") Long farmerId);

    @Query("SELECT c.status, COUNT(c) FROM Crop c GROUP BY c.status")
    List<Object[]> countAllCropsByStatus();

    @Query("SELECT c FROM Crop c WHERE c.farmer.email = :email " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:season IS NULL OR c.season = :season)")
    org.springframework.data.domain.Page<Crop> findByFarmerEmailAndFilters(
            @Param("email") String email,
            @Param("status") CropStatus status,
            @Param("season") com.smartagri.entity.Season season,
            org.springframework.data.domain.Pageable pageable);
}
