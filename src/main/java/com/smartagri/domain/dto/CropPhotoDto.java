package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read/write DTO for {@code CropPhoto} records.
 *
 * <p>On <em>upload</em> the client sends {@code description} and
 * {@code photoDate} as multipart form fields alongside the file.
 * All other fields are populated by the server on the response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropPhotoDto {

    /** Null on create; populated in responses. */
    private Long id;

    /** ID of the crop this photo belongs to. */
    private Long cropId;

    /** Name of the crop — populated in responses for convenience. */
    private String cropName;

    /** ID of the farmer who uploaded the photo — populated in responses. */
    private Long farmerId;

    /**
     * Fully-qualified URL where the image can be retrieved.
     * Populated in responses only.
     */
    private String photoUrl;

    /** Original (sanitised) filename — populated in responses. */
    private String fileName;

    /** Optional caption provided by the farmer. */
    private String description;

    /** Date the photo was taken. */
    private LocalDate photoDate;

    /** Server-side timestamp of the upload — populated in responses. */
    private LocalDateTime uploadedAt;
}
