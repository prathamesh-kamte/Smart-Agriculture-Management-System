package com.smartagri.engine;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Evaluates a set of agronomic rules against a {@link Crop} and produces
 * a list of {@link AdvisoryDto} recommendations.
 *
 * <h3>Internationalisation</h3>
 * All user-visible message strings are resolved through a {@link MessageSource}
 * so that the same rule logic can produce advisories in any supported locale
 * (English, Hindi, Marathi).  The caller supplies the locale via
 * {@link #evaluate(Crop, Locale)}.
 *
 * <h3>Rules evaluated</h3>
 * <ol>
 *   <li>Harvest approaching (≤ 14 days away)</li>
 *   <li>Overdue harvest</li>
 *   <li>Weekly irrigation check</li>
 *   <li>First NPK dose at 15 days</li>
 *   <li>Failed crop alert</li>
 *   <li>Soil moisture check (every 3 days)</li>
 *   <li>Kharif pest season warning (day 30-60)</li>
 *   <li>Ready-for-harvest but unharvested reminder</li>
 *   <li>30-day growth milestone — top-dress fertiliser</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class AdvisoryRuleEngine {

    private final MessageSource messageSource;

    // ─── Message keys ─────────────────────────────────────────────────────────

    private static final String KEY_HARVEST_APPROACHING     = "advisory.harvest.approaching";
    private static final String KEY_HARVEST_OVERDUE         = "advisory.harvest.overdue";
    private static final String KEY_HARVEST_READY           = "advisory.harvest.ready";
    private static final String KEY_IRRIGATION_DUE          = "advisory.irrigation.due";
    private static final String KEY_IRRIGATION_SOIL         = "advisory.irrigation.soil.moisture";
    private static final String KEY_FERTILISATION_FIRST     = "advisory.fertilisation.first";
    private static final String KEY_FERTILISATION_TOPDRESS  = "advisory.fertilisation.topdress";
    private static final String KEY_CROP_FAILED             = "advisory.crop.failed";
    private static final String KEY_PEST_SEASON             = "advisory.pest.season";

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Evaluates all rules for {@code crop} and returns the resulting advisories
     * in English (for backward-compatible callers that do not specify a locale).
     *
     * @param crop the crop to evaluate
     * @return list of generated advisories; never {@code null}
     */
    public List<AdvisoryDto> evaluate(Crop crop) {
        return evaluate(crop, Locale.ENGLISH);
    }

    /**
     * Evaluates all rules for {@code crop} and returns advisories localised to
     * the given {@code locale}.
     *
     * @param crop   the crop to evaluate
     * @param locale the target locale for advisory messages
     * @return list of generated advisories; never {@code null}
     */
    public List<AdvisoryDto> evaluate(Crop crop, Locale locale) {
        List<AdvisoryDto> advisories = new ArrayList<>();
        LocalDate now = LocalDate.now();

        long daysSincePlanting = crop.getPlantingDate() != null
                ? ChronoUnit.DAYS.between(crop.getPlantingDate(), now)
                : 0;

        checkHarvestApproaching(crop, now, locale).ifPresent(advisories::add);
        checkOverdueHarvest(crop, now, locale).ifPresent(advisories::add);
        checkIrrigationDue(crop, daysSincePlanting, locale).ifPresent(advisories::add);
        checkYoungCropFertilisation(crop, daysSincePlanting, locale).ifPresent(advisories::add);
        checkFailedCrop(crop, locale).ifPresent(advisories::add);
        checkSoilMoistureAlert(crop, daysSincePlanting, locale).ifPresent(advisories::add);
        checkPestSeasonAlert(crop, daysSincePlanting, locale).ifPresent(advisories::add);
        checkReadyForHarvestReminder(crop, locale).ifPresent(advisories::add);
        checkGrowthMilestone(crop, daysSincePlanting, locale).ifPresent(advisories::add);

        return advisories;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private rule methods
    // ═════════════════════════════════════════════════════════════════════════

    private Optional<AdvisoryDto> checkHarvestApproaching(Crop crop, LocalDate now, Locale locale) {
        if (crop.getExpectedHarvestDate() != null && crop.getStatus() == CropStatus.GROWING) {
            long daysToHarvest = ChronoUnit.DAYS.between(now, crop.getExpectedHarvestDate());
            if (daysToHarvest >= 0 && daysToHarvest <= 14) {
                return Optional.of(AdvisoryDto.builder()
                        .cropId(crop.getId())
                        .cropName(crop.getCropName())
                        .title("Harvest Approaching")
                        .message(msg(KEY_HARVEST_APPROACHING, locale,
                                crop.getCropName(), daysToHarvest))
                        .severity("WARNING")
                        .category("HARVEST")
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkOverdueHarvest(Crop crop, LocalDate now, Locale locale) {
        if (crop.getExpectedHarvestDate() != null && crop.getStatus() == CropStatus.GROWING) {
            if (now.isAfter(crop.getExpectedHarvestDate())) {
                return Optional.of(AdvisoryDto.builder()
                        .cropId(crop.getId())
                        .cropName(crop.getCropName())
                        .title("Overdue Harvest")
                        .message(msg(KEY_HARVEST_OVERDUE, locale, crop.getCropName()))
                        .severity("CRITICAL")
                        .category("HARVEST")
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkIrrigationDue(Crop crop, long daysSincePlanting,
                                                       Locale locale) {
        if (daysSincePlanting > 0 && daysSincePlanting % 7 == 0
                && crop.getStatus() != CropStatus.HARVESTED
                && crop.getStatus() != CropStatus.FAILED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Irrigation Due")
                    .message(msg(KEY_IRRIGATION_DUE, locale,
                            crop.getCropName(), daysSincePlanting))
                    .severity("INFO")
                    .category("IRRIGATION")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkYoungCropFertilisation(Crop crop, long daysSincePlanting,
                                                                Locale locale) {
        if (daysSincePlanting == 15 && crop.getStatus() == CropStatus.PLANTED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("First NPK Dose")
                    .message(msg(KEY_FERTILISATION_FIRST, locale, crop.getCropName()))
                    .severity("INFO")
                    .category("FERTILISATION")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkFailedCrop(Crop crop, Locale locale) {
        if (crop.getStatus() == CropStatus.FAILED) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Failed Crop Analysis")
                    .message(msg(KEY_CROP_FAILED, locale, crop.getCropName()))
                    .severity("CRITICAL")
                    .category("PEST_CONTROL")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkSoilMoistureAlert(Crop crop, long daysSincePlanting,
                                                           Locale locale) {
        if (daysSincePlanting % 3 == 0
                && (crop.getStatus() == CropStatus.PLANTED
                    || crop.getStatus() == CropStatus.GROWING)) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Soil Moisture Check")
                    .message(msg(KEY_IRRIGATION_SOIL, locale, crop.getCropName()))
                    .severity("INFO")
                    .category("IRRIGATION")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkPestSeasonAlert(Crop crop, long daysSincePlanting,
                                                         Locale locale) {
        if (crop.getSeason() == Season.KHARIF
                && crop.getStatus() == CropStatus.GROWING
                && daysSincePlanting >= 30
                && daysSincePlanting <= 60) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Pest Season Warning")
                    .message(msg(KEY_PEST_SEASON, locale, crop.getCropName()))
                    .severity("WARNING")
                    .category("PEST_CONTROL")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkReadyForHarvestReminder(Crop crop, Locale locale) {
        if (crop.getStatus() == CropStatus.READY_FOR_HARVEST
                && crop.getActualHarvestDate() == null) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Harvest Reminder")
                    .message(msg(KEY_HARVEST_READY, locale, crop.getCropName()))
                    .severity("CRITICAL")
                    .category("HARVEST")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<AdvisoryDto> checkGrowthMilestone(Crop crop, long daysSincePlanting,
                                                         Locale locale) {
        if (daysSincePlanting == 30 && crop.getStatus() == CropStatus.GROWING) {
            return Optional.of(AdvisoryDto.builder()
                    .cropId(crop.getId())
                    .cropName(crop.getCropName())
                    .title("Growth Milestone")
                    .message(msg(KEY_FERTILISATION_TOPDRESS, locale, crop.getCropName()))
                    .severity("INFO")
                    .category("FERTILISATION")
                    .build());
        }
        return Optional.empty();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helper – MessageSource lookup
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Resolves a message from the bundle and formats it with {@code args}.
     *
     * <p>Falls back to an empty string (rather than throwing) if the key is
     * missing in the bundle — the advisory will still be generated but with
     * no text, which is preferable to an application crash.
     *
     * @param key    message bundle key
     * @param locale target locale
     * @param args   positional substitution arguments ({0}, {1}, …)
     * @return resolved and formatted message string
     */
    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
