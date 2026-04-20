package com.smartagri.domain.entity;

import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Crop entity – captures the full lifecycle of a crop belonging to a farmer.
 */
@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String cropName;

    @Column(nullable = false, length = 100)
    private String cropType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CropStatus status;

    /** Planting date in the field. */
    @Column(nullable = false)
    private LocalDate plantingDate;

    /** Expected date of harvest. */
    private LocalDate expectedHarvestDate;

    /** Actual date of harvest – set when status moves to HARVESTED. */
    private LocalDate actualHarvestDate;

    /** Area of the plot in acres. */
    @Column(nullable = false)
    private Double areaInAcres;

    /** Free-form notes about the crop. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    // ─── Relationships ───────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    @OneToMany(mappedBy = "crop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();

    // ─── Lifecycle hooks ─────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = CropStatus.PLANTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
