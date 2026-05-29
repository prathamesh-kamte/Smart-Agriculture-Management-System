package com.smartagri.controller;

import com.smartagri.domain.dto.CropPhotoDto;
import com.smartagri.service.CropPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for crop photo management.
 *
 * <p>All endpoints require a valid JWT Bearer token (enforced globally by
 * the {@code SecurityFilterChain}).
 *
 * <ul>
 *   <li>{@code POST   /api/crops/{cropId}/photos}          — upload a photo</li>
 *   <li>{@code GET    /api/crops/{cropId}/photos}          — list all photos</li>
 *   <li>{@code DELETE /api/crops/{cropId}/photos/{photoId}} — delete a photo</li>
 * </ul>
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>Only the crop owner may upload or delete photos.</li>
 *   <li>Maximum 3 photos per crop.</li>
 *   <li>Files must be ≤ 5 MB and of type JPG or PNG.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/crops/{cropId}/photos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Crop Photos", description = "Upload and manage crop photos (max 3 per crop, JPG/PNG ≤ 5 MB)")
public class CropPhotoController {

    private final CropPhotoService cropPhotoService;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/crops/{cropId}/photos
     *
     * <p>Uploads a photo for the specified crop. The request must be sent
     * as {@code multipart/form-data} with a {@code file} part containing
     * the image and optional {@code description} / {@code photoDate} fields.
     *
     * @param cropId      crop primary key
     * @param file        image file (JPG or PNG, max 5 MB)
     * @param description optional caption
     * @param photoDate   optional date the photo was taken (ISO format)
     * @param userDetails authenticated farmer
     * @return 201 Created with the persisted {@link CropPhotoDto}
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a crop photo",
            description = "Uploads a JPG or PNG image for the specified crop. " +
                          "Maximum 3 photos per crop. File must be ≤ 5 MB. " +
                          "Only the crop owner may upload."
    )
    public ResponseEntity<CropPhotoDto> uploadPhoto(
            @Parameter(description = "Crop ID") @PathVariable Long cropId,
            @Parameter(description = "Image file (JPG / PNG, max 5 MB)")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional photo caption")
            @RequestParam(required = false) String description,
            @Parameter(description = "Date photo was taken (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate photoDate,
            @AuthenticationPrincipal UserDetails userDetails) {

        CropPhotoDto saved = cropPhotoService.uploadPhoto(
                cropId, file, description, photoDate, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/crops/{cropId}/photos
     *
     * <p>Returns all photos for the specified crop, ordered newest-first.
     *
     * @param cropId      crop primary key
     * @param userDetails authenticated farmer (must own the crop)
     * @return 200 OK with list of {@link CropPhotoDto}
     */
    @GetMapping
    @Operation(
            summary = "List photos for a crop",
            description = "Returns all uploaded photos for the given crop, newest first. " +
                          "Only the crop owner may list photos."
    )
    public ResponseEntity<List<CropPhotoDto>> getPhotos(
            @Parameter(description = "Crop ID") @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<CropPhotoDto> photos =
                cropPhotoService.getPhotosForCrop(cropId, userDetails.getUsername());
        return ResponseEntity.ok(photos);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DELETE /api/crops/{cropId}/photos/{photoId}
     *
     * <p>Deletes the specified photo. Only the farmer who uploaded it may
     * delete it. The underlying file is also removed from storage.
     *
     * @param cropId      crop primary key
     * @param photoId     photo primary key
     * @param userDetails authenticated farmer
     * @return 204 No Content on success
     */
    @DeleteMapping("/{photoId}")
    @Operation(
            summary = "Delete a crop photo",
            description = "Permanently deletes the photo and its stored file. " +
                          "Only the uploader (crop owner) may delete their own photos."
    )
    public ResponseEntity<Void> deletePhoto(
            @Parameter(description = "Crop ID")  @PathVariable Long cropId,
            @Parameter(description = "Photo ID") @PathVariable Long photoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        cropPhotoService.deletePhoto(cropId, photoId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
