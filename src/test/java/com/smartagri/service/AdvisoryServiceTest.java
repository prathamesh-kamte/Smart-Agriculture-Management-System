package com.smartagri.service;

import com.smartagri.domain.dto.AdvisoryDto;
import com.smartagri.entity.Advisory;
import com.smartagri.entity.Crop;
import com.smartagri.entity.User;
import com.smartagri.entity.CropStatus;
import com.smartagri.entity.Role;
import com.smartagri.entity.Season;
import com.smartagri.repository.AdvisoryRepository;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.impl.AdvisoryServiceImpl;
import com.smartagri.engine.AdvisoryRuleEngine;
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

@ExtendWith(MockitoExtension.class)
class AdvisoryServiceTest {

    @Mock private CropRepository cropRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdvisoryRepository advisoryRepository;

    @Spy  private AdvisoryRuleEngine ruleEngine;

    @InjectMocks private AdvisoryServiceImpl advisoryService;

    private User farmer;
    private Crop activeCrop;
    private Advisory advisoryEntity;

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
                .expectedHarvestDate(LocalDate.now().plusDays(10))
                .areaInAcres(1.2)
                .farmer(farmer)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
                
        advisoryEntity = Advisory.builder()
                .id(100L)
                .title("Harvest Approaching")
                .message("Your crop is nearing harvest.")
                .severity("INFO")
                .category("HARVEST")
                .generatedAt(LocalDateTime.now())
                .acknowledged(false)
                .crop(activeCrop)
                .farmer(farmer)
                .build();
    }

    @Test
    @DisplayName("generateAdvisories — harvest-approaching rule fires for crop due in 10 days")
    void generateAdvisories_harvestApproaching_producesAdvisory() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findActiveCropsByFarmerId(1L)).thenReturn(List.of(activeCrop));
        when(advisoryRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

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
        verify(advisoryRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("getActiveAdvisories — returns only unacknowledged advisories")
    void getActiveAdvisories_afterGeneration_returnsUnacknowledged() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(advisoryRepository.findByFarmerIdAndAcknowledgedFalse(1L)).thenReturn(List.of(advisoryEntity));

        List<AdvisoryDto> active = advisoryService.getActiveAdvisories("farmer@smartagri.com");

        assertThat(active).hasSize(1);
        assertThat(active.get(0).isAcknowledged()).isFalse();
    }

    @Test
    @DisplayName("acknowledgeAdvisory — marks advisory as acknowledged")
    void acknowledgeAdvisory_validId_marksAcknowledged() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(advisoryRepository.findById(100L)).thenReturn(Optional.of(advisoryEntity));

        advisoryService.acknowledgeAdvisory(100L, "farmer@smartagri.com");

        assertThat(advisoryEntity.isAcknowledged()).isTrue();
        verify(advisoryRepository).save(advisoryEntity);
    }
}
