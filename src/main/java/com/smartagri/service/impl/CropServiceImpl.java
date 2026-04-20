package com.smartagri.service.impl;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.CropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link CropService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CropServiceImpl implements CropService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CropDto createCrop(CropDto dto, String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);

        Crop crop = Crop.builder()
                .cropName(dto.getCropName())
                .cropType(dto.getCropType())
                .season(dto.getSeason())
                .status(dto.getStatus() != null ? dto.getStatus() : CropStatus.PLANTED)
                .plantingDate(dto.getPlantingDate())
                .expectedHarvestDate(dto.getExpectedHarvestDate())
                .areaInAcres(dto.getAreaInAcres())
                .notes(dto.getNotes())
                .farmer(farmer)
                .build();

        Crop saved = cropRepository.save(crop);
        log.info("Crop id={} created by farmer={}", saved.getId(), farmerEmail);
        return toDto(saved);
    }

    @Override
    public CropDto getCropById(Long id, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        return toDto(crop);
    }

    @Override
    public List<CropDto> getMyCrops(String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);
        return cropRepository.findByFarmerId(farmer.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CropDto> getAllCrops() {
        return cropRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CropDto updateCrop(Long id, CropDto dto, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);

        crop.setCropName(dto.getCropName());
        crop.setCropType(dto.getCropType());
        crop.setSeason(dto.getSeason());
        crop.setPlantingDate(dto.getPlantingDate());
        crop.setExpectedHarvestDate(dto.getExpectedHarvestDate());
        crop.setActualHarvestDate(dto.getActualHarvestDate());
        crop.setAreaInAcres(dto.getAreaInAcres());
        crop.setNotes(dto.getNotes());

        return toDto(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public CropDto updateCropStatus(Long id, CropStatus newStatus, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        crop.setStatus(newStatus);
        log.info("Crop id={} status updated to {} by {}", id, newStatus, requesterEmail);
        return toDto(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public void deleteCrop(Long id, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        cropRepository.delete(crop);
        log.info("Crop id={} deleted by {}", id, requesterEmail);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Crop findCropOrThrow(Long id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + id));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertOwnerOrAdmin(Crop crop, String requesterEmail) {
        boolean isAdmin = userRepository.findByEmail(requesterEmail)
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);
        if (!isAdmin && !crop.getFarmer().getEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("Access denied to crop id: " + crop.getId());
        }
    }

    CropDto toDto(Crop crop) {
        CropDto dto = new CropDto();
        dto.setId(crop.getId());
        dto.setCropName(crop.getCropName());
        dto.setCropType(crop.getCropType());
        dto.setSeason(crop.getSeason());
        dto.setStatus(crop.getStatus());
        dto.setPlantingDate(crop.getPlantingDate());
        dto.setExpectedHarvestDate(crop.getExpectedHarvestDate());
        dto.setActualHarvestDate(crop.getActualHarvestDate());
        dto.setAreaInAcres(crop.getAreaInAcres());
        dto.setNotes(crop.getNotes());
        dto.setFarmerId(crop.getFarmer().getId());
        dto.setFarmerName(crop.getFarmer().getFullName());
        return dto;
    }
}
