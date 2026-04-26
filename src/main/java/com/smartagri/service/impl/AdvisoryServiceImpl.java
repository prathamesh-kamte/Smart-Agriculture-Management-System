package com.smartagri.service.impl;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.engine.AdvisoryRuleEngine;
import com.smartagri.entity.Crop;
import com.smartagri.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisoryServiceImpl implements AdvisoryService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final AdvisoryRuleEngine ruleEngine;

    private final ConcurrentHashMap<Long, List<AdvisoryDto>> advisoryStore = new ConcurrentHashMap<>();
    private long idCounter = 1L;

    @Override
    public List<AdvisoryDto> generateAdvisories(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        List<Crop> activeCrops = cropRepository.findActiveCropsByFarmerId(farmer.getId());
        List<AdvisoryDto> newAdvisories = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Crop crop : activeCrops) {
            List<AdvisoryDto> generated = ruleEngine.evaluate(crop);
            for (AdvisoryDto dto : generated) {
                dto.setId(getNextId());
                dto.setGeneratedAt(now);
                dto.setAcknowledged(false);
                newAdvisories.add(dto);
            }
        }

        advisoryStore.merge(farmer.getId(), new CopyOnWriteArrayList<>(newAdvisories), (existing, newAdv) -> {
            existing.addAll(newAdv);
            return existing;
        });

        log.info("Generated {} advisories for farmer: {}", newAdvisories.size(), farmerEmail);
        return newAdvisories;
    }

    @Override
    public List<AdvisoryDto> getActiveAdvisories(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        List<AdvisoryDto> advisories = advisoryStore.getOrDefault(farmer.getId(), new CopyOnWriteArrayList<>());
        return advisories.stream()
                .filter(a -> !a.isAcknowledged())
                .collect(Collectors.toList());
    }

    @Override
    public void acknowledgeAdvisory(Long advisoryId, String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + farmerEmail));

        List<AdvisoryDto> advisories = advisoryStore.getOrDefault(farmer.getId(), new CopyOnWriteArrayList<>());
        AdvisoryDto advisory = advisories.stream()
                .filter(a -> a.getId().equals(advisoryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Advisory not found with id: " + advisoryId));

        advisory.setAcknowledged(true);
        log.info("Advisory id={} acknowledged by farmer: {}", advisoryId, farmerEmail);
    }

    @Override
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

    private synchronized long getNextId() {
        return idCounter++;
    }
}
