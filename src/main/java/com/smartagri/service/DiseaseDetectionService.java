package com.smartagri.service;

import com.smartagri.domain.dto.DiseaseDetectionDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for AI-powered crop disease detection.
 *
 * <p>The primary implementation calls the PlantNet API when
 * {@code plantnet.enabled=true}.  When disabled (or when the API returns a
 * low-confidence result), a descriptive fallback DTO is returned so the rest
 * of the application can continue to function during local development.
 */
public interface DiseaseDetectionService {

    /**
     * Submits {@code photo} to the AI backend, interprets the result, persists
     * the detection record, and returns a populated {@link DiseaseDetectionDto}.
     *
     * <p>Rules:
     * <ul>
     *   <li>If {@code plantnet.enabled=false} → return mock data.</li>
     *   <li>If the API call fails → return an "unable to identify" fallback.</li>
     *   <li>If the top-result confidence score is &lt; 0.30 → return fallback.</li>
     * </ul>
     *
     * @param photo       image file to analyse
     * @param cropId      optional crop this photo is associated with; may be {@code null}
     * @param farmerEmail authenticated farmer's email
     * @return populated detection result DTO; never {@code null}
     */
    DiseaseDetectionDto analyzePhoto(MultipartFile photo, Long cropId, String farmerEmail);

    /**
     * Returns all past detection results for the given farmer, newest first.
     *
     * @param farmerEmail authenticated farmer's email
     * @return list of {@link DiseaseDetectionDto}; never {@code null}
     */
    java.util.List<DiseaseDetectionDto> getHistoryForFarmer(String farmerEmail);

    /**
     * Returns all past detections for a specific crop, newest first.
     *
     * @param cropId      crop primary key
     * @param farmerEmail authenticated farmer's email (ownership check)
     * @return list of {@link DiseaseDetectionDto}; never {@code null}
     */
    java.util.List<DiseaseDetectionDto> getHistoryForCrop(Long cropId, String farmerEmail);
}
