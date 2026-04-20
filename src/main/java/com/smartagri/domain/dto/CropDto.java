package com.smartagri.domain.dto;

import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import jakarta.validation.constraints.*;
import lombok.Data;

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

    /** Populated on responses – ID of the owning farmer. */
    private Long farmerId;

    /** Populated on responses – display name of the owning farmer. */
    private String farmerName;
}
