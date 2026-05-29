package com.smartagri.service.impl;

import com.smartagri.domain.dto.CropPhotoDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.CropPhoto;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropPhotoRepository;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.CropPhotoService;
import com.smartagri.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link CropPhotoService} implementation.
 *
 * <p>Delegates actual file I/O to {@link StorageService} and enforces
 * all business rules (ownership, 3-photo cap, validation pass-through).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CropPhotoServiceImpl implements CropPhotoService {

    /** Maximum number of photos allowed per crop. */
    private static final int MAX_PHOTOS_PER_CROP = 3;

    /** Logical storage folder name passed to {@link StorageService}. */
    private static final String STORAGE_FOLDER = "crop-photos";

    private final CropPhotoRepository cropPhotoRepository;
    private final CropRepository      cropRepository;
    private final UserRepository      userRepository;
    private final StorageService      storageService;

    // ═════════════════════════════════════════════════════════════════════════
    // CropPhotoService implementation
    // ═════════════════════════════════════════════════════════════════════════

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CropPhotoDto uploadPhoto(Long cropId,
                                    MultipartFile file,
                                    String description,
                                    LocalDate photoDate,
                                    String email) {

        User farmer = findUserByEmail(email);
        Crop crop   = findCropOwnedByFarmer(cropId, farmer.getId());

        long existing = cropPhotoRepository.countByCropId(cropId);
        if (existing >= MAX_PHOTOS_PER_CROP) {
            throw new IllegalStateException(
                    "Maximum of " + MAX_PHOTOS_PER_CROP + " photos per crop reached. "
                    + "Delete an existing photo before uploading a new one.");
        }

        // StorageService performs type and size validation
        String photoUrl = storageService.uploadFile(file, STORAGE_FOLDER);

        CropPhoto photo = CropPhoto.builder()
                .crop(crop)
                .farmer(farmer)
                .photoUrl(photoUrl)
                .fileName(sanitiseFilename(file.getOriginalFilename()))
                .description(description)
                .photoDate(photoDate)
                .build();

        CropPhoto saved = cropPhotoRepository.save(photo);
        log.info("Uploaded photo id={} for crop id={} by farmer={}",
                saved.getId(), cropId, email);

        return toDto(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<CropPhotoDto> getPhotosForCrop(Long cropId, String email) {
        User farmer = findUserByEmail(email);
        // Verify the crop belongs to this farmer before listing
        findCropOwnedByFarmer(cropId, farmer.getId());

        return cropPhotoRepository
                .findByCropIdOrderByUploadedAtDesc(cropId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deletePhoto(Long cropId, Long photoId, String email) {
        User farmer = findUserByEmail(email);

        CropPhoto photo = cropPhotoRepository
                .findByIdAndFarmerId(photoId, farmer.getId())
                .orElseThrow(() -> new UnauthorizedException(
                        "Photo not found or you do not have permission to delete it"));

        // Ensure the photo belongs to the requested crop
        if (!photo.getCrop().getId().equals(cropId)) {
            throw new ResourceNotFoundException("Photo not found with id: " + photoId + " for crop: " + cropId);
        }

        storageService.deleteFile(photo.getPhotoUrl());
        cropPhotoRepository.delete(photo);
        log.info("Deleted photo id={} from crop id={} by farmer={}", photoId, cropId, email);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═════════════════════════════════════════════════════════════════════════

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Crop findCropOwnedByFarmer(Long cropId, Long farmerId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + cropId));
        if (!crop.getFarmer().getId().equals(farmerId)) {
            throw new UnauthorizedException("You do not own this crop");
        }
        return crop;
    }

    /** Strips path separators to prevent directory traversal via filename. */
    private String sanitiseFilename(String original) {
        if (original == null) return null;
        return Paths.get(original).getFileName().toString();
    }

    private CropPhotoDto toDto(CropPhoto photo) {
        return CropPhotoDto.builder()
                .id(photo.getId())
                .cropId(photo.getCrop().getId())
                .cropName(photo.getCrop().getCropName())
                .farmerId(photo.getFarmer().getId())
                .photoUrl(photo.getPhotoUrl())
                .fileName(photo.getFileName())
                .description(photo.getDescription())
                .photoDate(photo.getPhotoDate())
                .uploadedAt(photo.getUploadedAt())
                .build();
    }
}
