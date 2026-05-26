package com.smartagri.domain.dto;

import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO used for both creating and updating a Crop resource.
 */
@Data
public class CropDto {

    /** Null on create; populated on responses. */
    private Long id;

    @NotBlank(message = "Crop name is required")
    @Size(max = 100)
    private String cropName;

    @NotBlank(message = "Crop type is required")
    @Size(max = 100)
    private String cropType;

    @NotNull(message = "Season is required")
    private Season season;

    private CropStatus status;

    @NotNull(message = "Planting date is required")
    private LocalDate plantingDate;

    private LocalDate expectedHarvestDate;
    private LocalDate actualHarvestDate;

    @NotNull(message = "Area in acres is required")
    @Positive(message = "Area must be positive")
    private Double areaInAcres;

    private String notes;

    // ─── Yield tracking ──────────────────────────────────────────────────────

    /** Expected yield in kg — optional, supplied by the farmer. */
    @Positive(message = "Expected yield must be positive")
    private Double expectedYieldKg;

    /** Actual yield in kg — recorded at harvest time. */
    @Positive(message = "Actual yield must be positive")
    private Double actualYieldKg;

    /** Selling price per kg. */
    @Positive(message = "Selling price must be positive")
    private BigDecimal sellingPricePerKg;

    /**
     * Calculated on responses only (not persisted).
     * Formula: (actualYieldKg * sellingPricePerKg) - totalExpenses.
     * Null when any of the three yield fields is null.
     */
    private BigDecimal profitLoss;

    /** Populated on responses – ID of the owning farmer. */
    private Long farmerId;

    /** Populated on responses – display name of the owning farmer. */
    private String farmerName;
}
