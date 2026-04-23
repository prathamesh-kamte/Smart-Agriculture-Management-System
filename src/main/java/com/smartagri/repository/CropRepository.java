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
}
