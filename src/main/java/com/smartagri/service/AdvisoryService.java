package com.smartagri.service;

import com.smartagri.domain.dto.AdvisoryDto;

import java.util.List;

/**
 * Contract for generating and retrieving agricultural advisories.
 */
public interface AdvisoryService {

    /**
     * Generate rule-based advisories for all active crops of the
     * authenticated farmer.
     *
     * @param farmerEmail authenticated farmer's email
     * @return list of advisory messages
     */
    List<AdvisoryDto> generateAdvisories(String farmerEmail);

    /**
     * Retrieve the most recent active (unacknowledged) advisories for
     * the authenticated farmer.
     */
    List<AdvisoryDto> getActiveAdvisories(String farmerEmail);

    /**
     * Acknowledge / dismiss an advisory by its ID.
     *
     * @param advisoryId  ID of the advisory to acknowledge
     * @param farmerEmail authenticated farmer's email
     */
    void acknowledgeAdvisory(Long advisoryId, String farmerEmail);

    /**
     * Admin-only: generate and persist advisories for all active crops
     * across every farmer in the system (invoked by the scheduler).
     */
    void runScheduledAdvisoryGeneration();
}
