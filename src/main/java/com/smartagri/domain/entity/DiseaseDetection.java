package com.smartagri.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persisted record of a single AI disease-detection analysis run.
 *
 * <p>Treatments and preventions are stored as pipe-delimited strings
 * ({@code "step1|step2|step3"}) to avoid a separate join table.
 * They are split back to lists in the service layer.
 */
@Entity
@Table(name = "disease_detections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseaseDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Crop this detection is associated with.
     * Optional — nullable because the FK uses ON DELETE SET NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    /** Farmer who submitted the photo for analysis. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    /** URL of the submitted photo (may be null for inline uploads). */
    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    /** Name of the detected disease or plant condition. */
    @Column(name = "disease_name", length = 200)
    private String diseaseName;

    /** Confidence percentage string (e.g. {@code "87.3%"}). */
    @Column(name = "confidence", length = 20)
    private String confidence;

    /** Severity level: LOW, MEDIUM, or HIGH. */
    @Column(name = "severity", length = 20)
    private String severity;

    /** Plain-language description of the disease. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Pipe-delimited treatment steps.
     * Split on {@code "|"} to reconstruct the list.
     */
    @Column(name = "treatments", columnDefinition = "TEXT")
    private String treatments;

    /**
     * Pipe-delimited prevention steps.
     * Split on {@code "|"} to reconstruct the list.
     */
    @Column(name = "preventions", columnDefinition = "TEXT")
    private String preventions;

    /** Timestamp when the detection was performed — set in {@link #prePersist()}. */
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void prePersist() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
