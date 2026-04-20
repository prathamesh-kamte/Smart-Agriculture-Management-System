package com.smartagri.service;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Role;
import com.smartagri.domain.enums.Season;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.impl.AdvisoryServiceImpl;
import com.smartagri.util.AdvisoryRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AdvisoryServiceImpl}.
 *
 * <p>Uses a {@code @Spy} on {@link AdvisoryRuleEngine} so real rule logic
 * is exercised while still allowing verification.
 */
@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

    @Mock private CropRepository cropRepository;
    @Mock private UserRepository userRepository;

    @Spy  private AdvisoryRuleEngine ruleEngine;

    @InjectMocks private AdvisoryServiceImpl advisoryService;

    private User farmer;
    private Crop activeCrop;

    @BeforeEach
    void setUp() {
        farmer = User.builder()
                .id(1L).fullName("Ramesh Kumar")
                .email("farmer@smartagri.com")
                .password("hashed").role(Role.FARMER).enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        activeCrop = Crop.builder()
                .id(5L).cropName("Tomato").cropType("Vegetable")
                .season(Season.ZAID).status(CropStatus.GROWING)
                .plantingDate(LocalDate.now().minusDays(30))
                .expectedHarvestDate(LocalDate.now().plusDays(10)) // approaching harvest
                .areaInAcres(1.2)
                .farmer(farmer)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── generateAdvisories ───────────────────────────────────────────────────

    @Test
    @DisplayName("generateAdvisories — harvest-approaching rule fires for crop due in 10 days")
    void generateAdvisories_harvestApproaching_producesAdvisory() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of(activeCrop));

        List<AdvisoryDto> advisories = advisoryService.generateAdvisories("farmer@smartagri.com");

        assertThat(advisories).isNotEmpty();
        boolean hasHarvestAdvisory = advisories.stream()
                .anyMatch(a -> "HARVEST".equals(a.getCategory()));
        assertThat(hasHarvestAdvisory).isTrue();
    }

    @Test
    @DisplayName("generateAdvisories — no active crops produces empty list")
    void generateAdvisories_noActiveCrops_returnsEmpty() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of());

        List<AdvisoryDto> advisories = advisoryService.generateAdvisories("farmer@smartagri.com");

        assertThat(advisories).isEmpty();
    }

    // ─── getActiveAdvisories ──────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveAdvisories — returns only unacknowledged advisories")
    void getActiveAdvisories_afterGeneration_returnsUnacknowledged() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of(activeCrop));

        // Generate first
        advisoryService.generateAdvisories("farmer@smartagri.com");

        List<AdvisoryDto> active = advisoryService.getActiveAdvisories("farmer@smartagri.com");

        assertThat(active).allSatisfy(a -> assertThat(a.isAcknowledged()).isFalse());
    }

    @Test
    @DisplayName("getActiveAdvisories — no prior advisories returns empty list")
    void getActiveAdvisories_noPrior_returnsEmpty() {
        List<AdvisoryDto> result = advisoryService.getActiveAdvisories("nobody@x.com");
        assertThat(result).isEmpty();
    }

    // ─── acknowledgeAdvisory ──────────────────────────────────────────────────

    @Test
    @DisplayName("acknowledgeAdvisory — marks advisory as acknowledged")
    void acknowledgeAdvisory_validId_marksAcknowledged() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of(activeCrop));

        List<AdvisoryDto> generated = advisoryService.generateAdvisories("farmer@smartagri.com");
        assertThat(generated).isNotEmpty();

        Long firstId = generated.get(0).getId();
        advisoryService.acknowledgeAdvisory(firstId, "farmer@smartagri.com");

        List<AdvisoryDto> active = advisoryService.getActiveAdvisories("farmer@smartagri.com");
        boolean stillActive = active.stream().anyMatch(a -> firstId.equals(a.getId()));
        assertThat(stillActive).isFalse();
    }

    // ─── runScheduledAdvisoryGeneration ──────────────────────────────────────

    @Test
    @DisplayName("runScheduledAdvisoryGeneration — iterates all users without throwing")
    void runScheduledAdvisoryGeneration_allUsers_doesNotThrow() {
        when(userRepository.findAll()).thenReturn(List.of(farmer));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of());

        assertThatCode(() -> advisoryService.runScheduledAdvisoryGeneration())
                .doesNotThrowAnyException();

        verify(userRepository, times(1)).findAll();
    }
}
