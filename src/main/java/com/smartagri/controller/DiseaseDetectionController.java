package com.smartagri.controller;

import com.smartagri.domain.dto.DiseaseDetectionDto;
import com.smartagri.service.DiseaseDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for AI-powered crop disease detection.
 *
 * <p>All endpoints require a valid JWT Bearer token.
 *
 * <ul>
 *   <li>{@code POST /api/disease/analyze?cropId=} — submit photo for analysis</li>
 *   <li>{@code GET  /api/disease/history}          — farmer's detection history</li>
 *   <li>{@code GET  /api/disease/crop/{cropId}}    — detections for a crop</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/disease")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Disease Detection",
     description = "AI-powered crop disease detection via PlantNet API. "
                 + "Returns treatment and prevention advice.")
public class DiseaseDetectionController {

    private final DiseaseDetectionService diseaseDetectionService;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/disease/analyze?cropId={id}
     *
     * <p>Submits a crop photo to the AI backend and returns a disease analysis
     * result with treatments and prevention advice.
     *
     * <p>Request must be {@code multipart/form-data} with a {@code photo} part.
     *
     * @param photo       image file to analyse (JPG / PNG)
     * @param cropId      optional crop ID to associate the detection with
     * @param userDetails authenticated farmer
     * @return 201 Created with the populated {@link DiseaseDetectionDto}
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Analyze a crop photo for disease",
            description = "Submits the photo to the PlantNet AI API (or returns mock data "
                        + "when plantnet.enabled=false). "
                        + "Returns disease name, confidence, severity, treatments, and preventions. "
                        + "Result is persisted to the disease_detections table."
    )
    public ResponseEntity<DiseaseDetectionDto> analyzePhoto(
            @Parameter(description = "Crop photo to analyse (JPG / PNG)")
            @RequestPart("photo") MultipartFile photo,
            @Parameter(description = "Optional crop ID to link this detection to a specific crop")
            @RequestParam(required = false) Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {

        DiseaseDetectionDto result = diseaseDetectionService.analyzePhoto(
                photo, cropId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/disease/history
     *
     * <p>Returns all past disease detection results for the authenticated farmer,
     * ordered newest first.
     *
     * @param userDetails authenticated farmer
     * @return 200 OK with list of {@link DiseaseDetectionDto}
     */
    @GetMapping("/history")
    @Operation(
            summary = "Get disease detection history for the authenticated farmer",
            description = "Returns all stored detection results for the current farmer, newest first."
    )
    public ResponseEntity<List<DiseaseDetectionDto>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<DiseaseDetectionDto> history =
                diseaseDetectionService.getHistoryForFarmer(userDetails.getUsername());
        return ResponseEntity.ok(history);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/disease/crop/{cropId}
     *
     * <p>Returns all disease detections associated with the specified crop,
     * ordered newest first.
     *
     * @param cropId      crop primary key
     * @param userDetails authenticated farmer
     * @return 200 OK with list of {@link DiseaseDetectionDto}
     */
    @GetMapping("/crop/{cropId}")
    @Operation(
            summary = "Get disease detections for a specific crop",
            description = "Returns the full detection history for the given crop, newest first."
    )
    public ResponseEntity<List<DiseaseDetectionDto>> getForCrop(
            @Parameter(description = "Crop ID") @PathVariable Long cropId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<DiseaseDetectionDto> results =
                diseaseDetectionService.getHistoryForCrop(cropId, userDetails.getUsername());
        return ResponseEntity.ok(results);
    }
}
