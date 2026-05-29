package com.smartagri.repository;

import com.smartagri.domain.entity.CropPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link CropPhoto} entities.
 */
@Repository
public interface CropPhotoRepository extends JpaRepository<CropPhoto, Long> {

    /**
     * Returns all photos for a given crop, ordered newest-first.
     *
     * @param cropId the crop primary key
     * @return list of photos; never {@code null}
     */
    List<CropPhoto> findByCropIdOrderByUploadedAtDesc(Long cropId);

    /**
     * Counts how many photos a crop already has.
     * Used to enforce the 3-photo-per-crop limit.
     *
     * @param cropId the crop primary key
     * @return current photo count
     */
    long countByCropId(Long cropId);

    /**
     * Finds a specific photo belonging to a given farmer — used to verify
     * ownership before deletion.
     *
     * @param id       photo primary key
     * @param farmerId farmer primary key
     * @return the photo if found and owned by that farmer
     */
    Optional<CropPhoto> findByIdAndFarmerId(Long id, Long farmerId);
}
