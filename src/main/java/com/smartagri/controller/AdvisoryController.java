package com.smartagri.controller;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.service.AdvisoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for retrieving and managing AI/rule-based agricultural advisories.
 */
@RestController
@RequestMapping("/api/advisories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Advisories", description = "Rule-based agricultural advisory management")
public class AdvisoryController {

    private final AdvisoryService advisoryService;

    /**
     * GET /api/advisories/active
     * Retrieve unacknowledged advisories for the authenticated farmer.
     */
    @GetMapping("/active")
    @Operation(summary = "Get active advisories")
    public ResponseEntity<List<AdvisoryDto>> getActiveAdvisories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(advisoryService.getActiveAdvisories(userDetails.getUsername()));
    }

    /**
     * POST /api/advisories/generate
     * Trigger advisory generation for the authenticated farmer's active crops.
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate advisories")
    public ResponseEntity<List<AdvisoryDto>> generateAdvisories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(advisoryService.generateAdvisories(userDetails.getUsername()));
    }

    /**
     * PATCH /api/advisories/{id}/acknowledge
     * Mark an advisory as acknowledged / dismissed.
     */
    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge advisory")
    public ResponseEntity<Void> acknowledgeAdvisory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        advisoryService.acknowledgeAdvisory(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
