package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the result of an AI-powered crop disease detection analysis.
 *
 * <p>Fields are populated either from the PlantNet API response or from the
 * fallback mock/error path.  Confidence is expressed as a percentage string
 * (e.g. {@code "78.4%"}) for easy display in the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseDetectionDto {

    /** Persistence ID — null on first return, populated after save. */
    private Long id;

    /** Human-readable name of the detected disease or plant condition. */
    private String diseaseName;

    /**
     * Confidence score from the AI model, formatted as a percentage string
     * (e.g. {@code "87.3%"}).
     */
    private String confidence;

    /**
     * Severity classification: {@code "LOW"}, {@code "MEDIUM"}, or
     * {@code "HIGH"}.  Derived from the disease lookup table.
     */
    private String severity;

    /** Plain-language description of the disease and its effects. */
    private String description;

    /** Ordered list of recommended treatment steps. */
    private List<String> treatments;

    /** Ordered list of preventive measures to avoid recurrence. */
    private List<String> preventions;

    /**
     * URL of the photo that was submitted for analysis.
     * May be {@code null} when the photo was analysed without being stored.
     */
    private String photoUrl;

    /** Timestamp when the detection was performed. */
    private LocalDateTime detectedAt;

    /** ID of the crop this detection is associated with; may be {@code null}. */
    private Long cropId;

    /** ID of the farmer who submitted the photo. */
    private Long farmerId;
}
