package com.smartagri.service.impl;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.engine.AdvisoryRuleEngine;
import com.smartagri.entity.Advisory;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.AdvisoryRepository;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.AdvisoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisoryServiceImpl implements AdvisoryService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final AdvisoryRepository advisoryRepository;
    private final AdvisoryRuleEngine ruleEngine;

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
