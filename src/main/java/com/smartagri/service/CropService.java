package com.smartagri.service;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.enums.CropStatus;

import java.util.List;

/**
 * Contract for crop management operations.
 */
public interface CropService {

    /** Create a new crop record for the authenticated farmer. */
    CropDto createCrop(CropDto cropDto, String farmerEmail);

    /** Retrieve a crop by its ID (validates ownership/admin). */
    CropDto getCropById(Long id, String requesterEmail);

    /** All crops owned by the authenticated farmer. */
    List<CropDto> getMyCrops(String farmerEmail);

    /** All crops in the system (admin only). */
    List<CropDto> getAllCrops();

    /** Update mutable crop fields. */
    CropDto updateCrop(Long id, CropDto cropDto, String requesterEmail);

    /** Transition the crop to a new lifecycle status. */
    CropDto updateCropStatus(Long id, CropStatus newStatus, String requesterEmail);

    /** Delete a crop and cascade-remove its expenses. */
    void deleteCrop(Long id, String requesterEmail);
}
