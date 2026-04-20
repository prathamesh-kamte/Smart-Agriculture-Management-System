package com.smartagri.controller;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.service.CropService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for crop lifecycle management.
 */
@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Crops", description = "Crop lifecycle management")
public class CropController {

    private final CropService cropService;

    /**
     * POST /api/crops — Create a new crop for the authenticated farmer.
     */
    @PostMapping
    @Operation(summary = "Create crop")
    public ResponseEntity<CropDto> createCrop(
            @Valid @RequestBody CropDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cropService.createCrop(dto, userDetails.getUsername()));
    }

    /**
     * GET /api/crops — Get all crops owned by the authenticated farmer.
     */
    @GetMapping
    @Operation(summary = "Get my crops")
    public ResponseEntity<List<CropDto>> getMyCrops(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cropService.getMyCrops(userDetails.getUsername()));
    }

    /**
     * GET /api/crops/all — Get all crops in system (ADMIN only).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all crops (Admin)")
    public ResponseEntity<List<CropDto>> getAllCrops() {
        return ResponseEntity.ok(cropService.getAllCrops());
    }

    /**
     * GET /api/crops/{id} — Get a specific crop by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get crop by ID")
    public ResponseEntity<CropDto> getCrop(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cropService.getCropById(id, userDetails.getUsername()));
    }

    /**
     * PUT /api/crops/{id} — Update crop details.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update crop")
    public ResponseEntity<CropDto> updateCrop(
            @PathVariable Long id,
            @Valid @RequestBody CropDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cropService.updateCrop(id, dto, userDetails.getUsername()));
    }

    /**
     * PATCH /api/crops/{id}/status — Transition crop to a new lifecycle status.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update crop status")
    public ResponseEntity<CropDto> updateStatus(
            @PathVariable Long id,
            @RequestParam CropStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cropService.updateCropStatus(id, status, userDetails.getUsername()));
    }

    /**
     * DELETE /api/crops/{id} — Delete a crop and all associated expenses.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete crop")
    public ResponseEntity<Void> deleteCrop(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        cropService.deleteCrop(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
