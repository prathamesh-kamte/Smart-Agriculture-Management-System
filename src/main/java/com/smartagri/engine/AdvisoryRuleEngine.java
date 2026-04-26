package com.smartagri.engine;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.entity.Crop;
import com.smartagri.entity.CropStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AdvisoryRuleEngine {

    public List<AdvisoryDto> evaluate(Crop crop) {
        List<AdvisoryDto> advisories = new ArrayList<>();
        LocalDate now = LocalDate.now();

        long daysSincePlanting = crop.getPlantingDate() != null
                ? ChronoUnit.DAYS.between(crop.getPlantingDate(), now)
                : 0;

        checkHarvestApproaching(crop, now).ifPresent(advisories::add);
        checkOverdueHarvest(crop, now).ifPresent(advisories::add);
        checkIrrigationDue(crop, daysSincePlanting).ifPresent(advisories::add);
        checkYoungCropFertilisation(crop, daysSincePlanting).ifPresent(advisories::add);
        checkFailedCrop(crop).ifPresent(advisories::add);

        return advisories;
    }

    private Optional<AdvisoryDto> checkHarvestApproaching(Crop crop, LocalDate now) {
        if (crop.getExpectedHarvestDate() != null && crop.getStatus() == CropStatus.GROWING) {
            long daysToHarvest = ChronoUnit.DAYS.between(now, crop.getExpectedHarvestDate());
            if (daysToHarvest >= 0 && daysToHarvest <= 14) {
                return Optional.of(AdvisoryDto.builder()
                        .cropId(crop.getId())
                        .cropName(crop.getCropName())
                        .title("Harvest Approaching")
                        .message(String.format("%s is approaching harvest in %d days.", crop.getCropName(), daysToHarvest))
                        .severity("WARNING")
                        .category("HARVEST")
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkOverdueHarvest(Crop crop, LocalDate now) {
        if (crop.getExpectedHarvestDate() != null && crop.getStatus() == CropStatus.GROWING) {
            if (now.isAfter(crop.getExpectedHarvestDate())) {
                return Optional.of(AdvisoryDto.builder()
                        .cropId(crop.getId())
                        .cropName(crop.getCropName())
                        .title("Overdue Harvest")
                        .message(String.format("%s is overdue for harvest.", crop.getCropName()))
                        .severity("CRITICAL")
                        .category("HARVEST")
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkIrrigationDue(Crop crop, long daysSincePlanting) {
        if (daysSincePlanting > 0 && daysSincePlanting % 7 == 0
                && crop.getStatus() != CropStatus.HARVESTED
                && crop.getStatus() != CropStatus.FAILED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Irrigation Due")
                    .message(String.format("Irrigation is due for %s (Day %d).", crop.getCropName(), daysSincePlanting))
                    .severity("INFO")
                    .category("IRRIGATION")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkYoungCropFertilisation(Crop crop, long daysSincePlanting) {
        if (daysSincePlanting == 15 && crop.getStatus() == CropStatus.PLANTED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("First NPK Dose")
                    .message(String.format("Time for the first NPK dose for %s.", crop.getCropName()))
                    .severity("INFO")
                    .category("FERTILISATION")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkFailedCrop(Crop crop) {
        if (crop.getStatus() == CropStatus.FAILED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Failed Crop Analysis")
                    .message(String.format("%s has failed. Please conduct a post-mortem and review pest control strategies.", crop.getCropName()))
                    .severity("CRITICAL")
                    .category("PEST_CONTROL")
                    .build());
        }
        return Optional.empty();
    }
}
