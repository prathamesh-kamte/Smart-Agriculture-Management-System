package com.smartagri.service;

import com.smartagri.domain.dto.CropPhotoDto;

import java.util.List;

/**
 * Business logic for managing crop photo uploads, retrieval, and deletion.
 *
 * <p>Photo storage is delegated to {@link StorageService}; this interface
 * handles validation, persistence, and ownership checks.
 */
public interface CropPhotoService {

    /**
     * Uploads a photo for a crop and persists the metadata.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>The caller must own the crop.</li>
     *   <li>At most 3 photos are allowed per crop.</li>
     *   <li>File must be ≤ 5 MB and of type JPG or PNG.</li>
     * </ul>
     *
     * @param cropId      crop primary key
     * @param file        the uploaded image file
     * @param description optional caption
     * @param photoDate   optional date the photo was taken
     * @param email       authenticated farmer's email
     * @return the persisted {@link CropPhotoDto}
     */
    CropPhotoDto uploadPhoto(Long cropId,
                             org.springframework.web.multipart.MultipartFile file,
                             String description,
                             java.time.LocalDate photoDate,
                             String email);

    /**
     * Returns all photos for the given crop.
     *
     * @param cropId the crop primary key
     * @param email  authenticated farmer's email (used for ownership check)
     * @return list of {@link CropPhotoDto}; never {@code null}
     */
    List<CropPhotoDto> getPhotosForCrop(Long cropId, String email);

    /**
     * Deletes the specified photo.
     *
     * <p>Only the farmer who uploaded the photo may delete it.
     *
     * @param cropId  crop primary key
     * @param photoId photo primary key
     * @param email   authenticated farmer's email
     */
    void deletePhoto(Long cropId, Long photoId, String email);
}
