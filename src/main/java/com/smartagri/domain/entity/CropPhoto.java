package com.smartagri.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persisted record of a single photo attached to a {@link Crop}.
 *
 * <p>The actual file is stored either in Amazon S3 or the local filesystem
 * (controlled by {@code aws.enabled}).  This entity stores only the
 * resolvable URL and descriptive metadata.
 */
@Entity
@Table(name = "crop_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Crop this photo belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /** Farmer who uploaded the photo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private User farmer;

    /**
     * Fully-qualified URL where the image can be retrieved.
     * For S3 this is the public object URL; for local storage it is
     * the relative servlet path (e.g. {@code /uploads/abc123.jpg}).
     */
    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    /** Original (sanitised) filename as uploaded by the client. */
    @Column(name = "file_name", length = 200)
    private String fileName;

    /** Optional free-form caption provided by the farmer. */
    @Column(name = "description", length = 300)
    private String description;

    /** Date the photo was taken (may differ from upload date). */
    @Column(name = "photo_date")
    private LocalDate photoDate;

    /** Timestamp when the record was inserted — set in {@link #prePersist()}. */
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void prePersist() {
        uploadedAt = LocalDateTime.now();
    }
}
