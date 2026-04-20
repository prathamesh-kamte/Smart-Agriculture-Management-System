package com.smartagri.service.impl;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.AdvisoryService;
import com.smartagri.util.AdvisoryRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link AdvisoryService}.
 *
 * <p>Advisories are generated in-memory via the {@link AdvisoryRuleEngine}
 * and stored in a thread-safe map keyed by farmer email.
 * In a production setup, advisories would be persisted in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvisoryServiceImpl implements AdvisoryService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final AdvisoryRuleEngine ruleEngine;

    /**
     * In-memory advisory store: farmerEmail → list of advisory DTOs.
     * Replace with a proper Advisory JPA entity + repository for full persistence.
     */
    private final ConcurrentHashMap<String, List<AdvisoryDto>> advisoryStore = new ConcurrentHashMap<>();

    /** Running advisory ID counter (replace with DB sequence in production). */
    private long idCounter = 1L;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<AdvisoryDto> generateAdvisories(String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);
        List<Crop> activeCrops = cropRepository.findActiveCropsByFarmerId(farmer.getId());

        List<AdvisoryDto> generated = new ArrayList<>();
        for (Crop crop : activeCrops) {
            List<AdvisoryDto> cropAdvisories = ruleEngine.evaluate(crop);
            cropAdvisories.forEach(a -> {
                a.setId(idCounter++);
                a.setGeneratedAt(LocalDateTime.now());
                a.setAcknowledged(false);
            });
            generated.addAll(cropAdvisories);
        }

        advisoryStore.merge(farmerEmail, generated, (existing, newOnes) -> {
            existing.addAll(newOnes);
            return existing;
        });

        log.info("Generated {} advisories for farmer={}", generated.size(), farmerEmail);
        return generated;
    }

    @Override
    public List<AdvisoryDto> getActiveAdvisories(String farmerEmail) {
        return advisoryStore.getOrDefault(farmerEmail, List.of()).stream()
                .filter(a -> !a.isAcknowledged())
                .collect(Collectors.toList());
    }

    @Override
    public void acknowledgeAdvisory(Long advisoryId, String farmerEmail) {
        List<AdvisoryDto> advisories = advisoryStore.get(farmerEmail);
        if (advisories == null) {
            throw new ResourceNotFoundException("No advisories found for farmer: " + farmerEmail);
        }
        advisories.stream()
                .filter(a -> a.getId() != null && a.getId().equals(advisoryId))
                .findFirst()
                .ifPresentOrElse(
                        a -> {
                            a.setAcknowledged(true);
                            log.info("Advisory id={} acknowledged by {}", advisoryId, farmerEmail);
                        },
                        () -> { throw new ResourceNotFoundException("Advisory not found: " + advisoryId); }
                );
    }

    @Override
    public void runScheduledAdvisoryGeneration() {
        log.info("Running scheduled advisory generation for all farmers…");
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            try {
                generateAdvisories(user.getEmail());
            } catch (Exception ex) {
                log.warn("Failed to generate advisory for user={}: {}", user.getEmail(), ex.getMessage());
            }
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
