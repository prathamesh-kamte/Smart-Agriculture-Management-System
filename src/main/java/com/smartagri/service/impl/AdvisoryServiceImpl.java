package com.smartagri.service.impl;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.domain.dto.WeatherDto;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.engine.AdvisoryRuleEngine;
import com.smartagri.entity.Advisory;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.AdvisoryRepository;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.AdvisoryService;
import com.smartagri.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisoryServiceImpl implements AdvisoryService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final AdvisoryRepository advisoryRepository;
    private final AdvisoryRuleEngine ruleEngine;
    private final WeatherService weatherService;

    @Override
    @Transactional
    public List<AdvisoryDto> generateAdvisories(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        List<Crop> activeCrops = cropRepository.findActiveCropsByFarmerId(farmer.getId());
        List<AdvisoryDto> generatedAdvisories = new ArrayList<>();
        List<Advisory> advisoriesToSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Crop crop : activeCrops) {
            List<AdvisoryDto> generated = ruleEngine.evaluate(crop);
            for (AdvisoryDto dto : generated) {
                Advisory advisory = Advisory.builder()
                        .title(dto.getTitle())
                        .message(dto.getMessage())
                        .severity(dto.getSeverity())
                        .category(dto.getCategory())
                        .generatedAt(now)
                        .acknowledged(false)
                        .crop(crop)
                        .farmer(farmer)
                        .build();
                advisoriesToSave.add(advisory);
            }
        }

        List<Advisory> savedAdvisories = advisoryRepository.saveAll(advisoriesToSave);
        
        for (Advisory saved : savedAdvisories) {
            generatedAdvisories.add(mapToDto(saved));
        }

        log.info("Generated {} advisories for farmer: {}", savedAdvisories.size(), farmerEmail);
        return generatedAdvisories;
    }

    @Override
    public com.smartagri.domain.dto.PageResponse<AdvisoryDto> getActiveAdvisories(String farmerEmail, String severity, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Advisory> page = advisoryRepository.findActiveByFarmerEmailAndFilters(farmerEmail, severity, pageable);
        return com.smartagri.domain.dto.PageResponse.of(page, this::mapToDto);
    }

    @Override
    @Transactional
    public void acknowledgeAdvisory(Long advisoryId, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        Advisory advisory = advisoryRepository.findById(advisoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisory not found with id: " + advisoryId));

        if (!advisory.getFarmer().getId().equals(farmer.getId())) {
            throw new ResourceNotFoundException("Advisory not found with id: " + advisoryId + " for user: " + farmerEmail);
        }

        advisory.setAcknowledged(true);
        advisoryRepository.save(advisory);
        log.info("Advisory id={} acknowledged by farmer: {}", advisoryId, farmerEmail);
    }

    @Override
    @Transactional
    public void runScheduledAdvisoryGeneration() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                generateAdvisories(user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to generate advisories for user: {}", user.getEmail(), e);
            }
            try {
                generateWeatherAdvisories(user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to generate weather advisories for user: {}", user.getEmail(), e);
            }
        }
    }

    @Override
    @Transactional
    public void runScheduledIrrigationAdvisories() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                generateAdvisories(user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to generate irrigation advisories for user: {}", user.getEmail(), e);
            }
        }
    }

    /**
     * Fetches current weather for Pune and generates condition-specific advisories
     * for the given farmer's active crops:
     * <ul>
     *   <li>Rainy  → INFO  for GROWING crops   — skip irrigation today</li>
     *   <li>Frosty → CRITICAL for PLANTED/GROWING — frost protection required</li>
     *   <li>Temp &gt; 40 °C → WARNING for all active crops — extreme heat</li>
     * </ul>
     *
     * <p>The weather call is wrapped in a try-catch; any exception is logged at
     * WARN level and the method returns without generating any advisories.
     */
    @Override
    @Transactional
    public void generateWeatherAdvisories(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        // ── Fetch current weather — graceful degradation on failure ───────────
        WeatherDto weather;
        try {
            weather = weatherService.getCurrentWeather("Pune");
        } catch (Exception ex) {
            log.warn("Weather service unavailable — skipping weather advisories for farmer: {}. Reason: {}",
                    farmerEmail, ex.getMessage());
            return;
        }

        List<Crop> activeCrops = cropRepository.findActiveCropsByFarmerId(farmer.getId());
        List<Advisory> advisoriesToSave = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Crop crop : activeCrops) {
            String cropName = crop.getCropName();
            CropStatus status = crop.getStatus();

            // ── 1. Rainy: INFO advisory for GROWING crops ─────────────────────
            if (weather.isRainy() && status == CropStatus.GROWING) {
                advisoriesToSave.add(Advisory.builder()
                        .title("Rain expected — irrigation not required today")
                        .message("Rain expected - skip irrigation today for " + cropName)
                        .severity("INFO")
                        .category("WEATHER")
                        .generatedAt(now)
                        .acknowledged(false)
                        .crop(crop)
                        .farmer(farmer)
                        .build());
            }

            // ── 2. Frosty: CRITICAL advisory for PLANTED and GROWING crops ────
            if (weather.isFrosty()
                    && (status == CropStatus.PLANTED || status == CropStatus.GROWING)) {
                advisoriesToSave.add(Advisory.builder()
                        .title("Frost alert — crop protection required tonight")
                        .message("Frost alert - protect " + cropName + " with covering tonight")
                        .severity("CRITICAL")
                        .category("WEATHER")
                        .generatedAt(now)
                        .acknowledged(false)
                        .crop(crop)
                        .farmer(farmer)
                        .build());
            }

            // ── 3. Extreme heat: WARNING for all active crops ─────────────────
            if (weather.getTemperature() > 40.0) {
                advisoriesToSave.add(Advisory.builder()
                        .title("Extreme heat alert — increase irrigation frequency")
                        .message("Extreme heat alert - increase irrigation frequency for " + cropName)
                        .severity("WARNING")
                        .category("WEATHER")
                        .generatedAt(now)
                        .acknowledged(false)
                        .crop(crop)
                        .farmer(farmer)
                        .build());
            }
        }

        if (!advisoriesToSave.isEmpty()) {
            advisoryRepository.saveAll(advisoriesToSave);
            log.info("Saved {} weather advisory/advisories for farmer: {} (rainy={}, frosty={}, temp={}°C)",
                    advisoriesToSave.size(), farmerEmail,
                    weather.isRainy(), weather.isFrosty(), weather.getTemperature());
        } else {
            log.debug("No weather conditions triggered advisories for farmer: {}", farmerEmail);
        }
    }

    private AdvisoryDto mapToDto(Advisory advisory) {
        return AdvisoryDto.builder()
                .id(advisory.getId())
                .cropId(advisory.getCrop() != null ? advisory.getCrop().getId() : null)
                .cropName(advisory.getCrop() != null ? advisory.getCrop().getCropName() : null)
                .title(advisory.getTitle())
                .message(advisory.getMessage())
                .severity(advisory.getSeverity())
                .category(advisory.getCategory())
                .generatedAt(advisory.getGeneratedAt())
                .acknowledged(advisory.isAcknowledged())
                .build();
    }
}
