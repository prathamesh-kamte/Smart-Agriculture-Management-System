package com.smartagri.util;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.enums.CropStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule engine that evaluates a crop and produces relevant advisory messages.
 *
 * <p>Each public {@code check*} method encapsulates a single agronomic rule.
 * New rules can be added without touching any other class.
 *
 * <p>In a production system, rules would be externalized to a database or
 * rules-engine (e.g. Drools) for dynamic configuration.
 */
@Component
public class AdvisoryRuleEngine {

    /**
     * Evaluate all rules against the given crop and return the resulting advisories.
     */
    public List<AdvisoryDto> evaluate(Crop crop) {
        List<AdvisoryDto> advisories = new ArrayList<>();

        checkHarvestApproaching(crop, advisories);
        checkOverdueHarvest(crop, advisories);
        checkIrrigationDue(crop, advisories);
        checkYoungCropFertilisation(crop, advisories);
        checkFailedCrop(crop, advisories);

        return advisories;
    }

    // ─── Rules ───────────────────────────────────────────────────────────────

    /**
     * Rule 1 – Harvest approaching within 14 days.
     */
    private void checkHarvestApproaching(Crop crop, List<AdvisoryDto> advisories) {
        if (crop.getExpectedHarvestDate() == null) return;
        long daysUntilHarvest = ChronoUnit.DAYS.between(LocalDate.now(), crop.getExpectedHarvestDate());
        if (daysUntilHarvest >= 0 && daysUntilHarvest <= 14
                && crop.getStatus() == CropStatus.GROWING) {
            advisories.add(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Harvest Approaching")
                    .message(String.format(
                            "Your %s crop is due for harvest in %d day(s). Prepare harvesting equipment and storage.",
                            crop.getCropName(), daysUntilHarvest))
                    .severity("WARNING")
                    .category("HARVEST")
                    .build());
        }
    }

    /**
     * Rule 2 – Expected harvest date is past without status change.
     */
    private void checkOverdueHarvest(Crop crop, List<AdvisoryDto> advisories) {
        if (crop.getExpectedHarvestDate() == null) return;
        if (LocalDate.now().isAfter(crop.getExpectedHarvestDate())
                && crop.getStatus() == CropStatus.GROWING) {
            advisories.add(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Overdue Harvest")
                    .message(String.format(
                            "The expected harvest date for %s has passed. Delayed harvest can lead to quality loss.",
                            crop.getCropName()))
                    .severity("CRITICAL")
                    .category("HARVEST")
                    .build());
        }
    }

    /**
     * Rule 3 – Irrigation reminder every 7 days after planting.
     */
    private void checkIrrigationDue(Crop crop, List<AdvisoryDto> advisories) {
        long daysSincePlanting = ChronoUnit.DAYS.between(crop.getPlantingDate(), LocalDate.now());
        if (daysSincePlanting > 0 && daysSincePlanting % 7 == 0
                && crop.getStatus() != CropStatus.HARVESTED
                && crop.getStatus() != CropStatus.FAILED) {
            advisories.add(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Irrigation Reminder")
                    .message(String.format(
                            "Weekly irrigation check: ensure adequate water supply for %s (Day %d).",
                            crop.getCropName(), daysSincePlanting))
                    .severity("INFO")
                    .category("IRRIGATION")
                    .build());
        }
    }

    /**
     * Rule 4 – Fertilisation recommended in first 30 days.
     */
    private void checkYoungCropFertilisation(Crop crop, List<AdvisoryDto> advisories) {
        long daysSincePlanting = ChronoUnit.DAYS.between(crop.getPlantingDate(), LocalDate.now());
        if (daysSincePlanting == 15 && crop.getStatus() == CropStatus.PLANTED) {
            advisories.add(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("First Fertilisation Due")
                    .message(String.format(
                            "%s is 15 days old. Apply the first dose of NPK fertiliser to boost early growth.",
                            crop.getCropName()))
                    .severity("INFO")
                    .category("FERTILISATION")
                    .build());
        }
    }

    /**
     * Rule 5 – Alert if crop is marked FAILED without action.
     */
    private void checkFailedCrop(Crop crop, List<AdvisoryDto> advisories) {
        if (crop.getStatus() == CropStatus.FAILED) {
            advisories.add(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Crop Failure Detected")
                    .message(String.format(
                            "Crop %s has been marked as FAILED. Review soil reports and consult an agronomist.",
                            crop.getCropName()))
                    .severity("CRITICAL")
                    .category("PEST_CONTROL")
                    .build());
        }
    }
}
