package com.smartagri.scheduler;

import com.smartagri.service.AdvisoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically evaluates agronomic rules for all active
 * crops and persists the resulting advisories.
 *
 * <p>Scheduling expressions are in standard cron format:
 * {@code second minute hour day-of-month month day-of-week}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CropAdvisoryScheduler {

    private final AdvisoryService advisoryService;

    /**
     * Daily advisory generation at 07:00 every morning.
     * Evaluates every active crop across all farmers.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void generateDailyAdvisories() {
        log.info("=== [Scheduler] Daily advisory generation started ===");
        try {
            advisoryService.runScheduledAdvisoryGeneration();
            log.info("=== [Scheduler] Daily advisory generation completed ===");
        } catch (Exception ex) {
            log.error("=== [Scheduler] Advisory generation failed: {} ===", ex.getMessage(), ex);
        }
    }

    /**
     * Irrigation check every Monday and Thursday at 06:00.
     * Dedicated irrigation-focused advisory pass.
     */
    @Scheduled(cron = "0 0 6 * * MON,THU")
    public void generateIrrigationAdvisories() {
        log.info("=== [Scheduler] Irrigation advisory check triggered ===");
        try {
            advisoryService.runScheduledAdvisoryGeneration();
            log.info("=== [Scheduler] Irrigation advisory check completed ===");
        } catch (Exception ex) {
            log.error("=== [Scheduler] Irrigation advisory check failed: {} ===", ex.getMessage(), ex);
        }
    }
}
