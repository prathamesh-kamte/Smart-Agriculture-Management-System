package com.smartagri.service;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Role;
import com.smartagri.domain.enums.Season;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.impl.CropServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CropServiceImpl}.
 * All dependencies are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CropServiceTest {

    @Mock private CropRepository cropRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CropServiceImpl cropService;

    private User farmer;
    private User admin;
    private Crop crop;

    @BeforeEach
    void setUp() {
        farmer = User.builder()
                .id(1L).fullName("Ramesh Kumar")
                .email("farmer@smartagri.com")
                .password("hashed").role(Role.FARMER)
                .enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        admin = User.builder()
                .id(2L).fullName("System Admin")
                .email("admin@smartagri.com")
                .password("hashed").role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        crop = Crop.builder()
                .id(10L).cropName("Wheat").cropType("Cereal")
                .season(Season.RABI).status(CropStatus.PLANTED)
                .plantingDate(LocalDate.now().minusDays(5))
                .areaInAcres(5.0)
                .farmer(farmer)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createCrop ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCrop — saves and returns CropDto")
    void createCrop_validInput_returnsCropDto() {
        CropDto dto = new CropDto();
        dto.setCropName("Wheat"); dto.setCropType("Cereal");
        dto.setSeason(Season.RABI); dto.setPlantingDate(LocalDate.now());
        dto.setAreaInAcres(5.0);

        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.save(any(Crop.class))).thenReturn(crop);

        CropDto result = cropService.createCrop(dto, "farmer@smartagri.com");

        assertThat(result).isNotNull();
        assertThat(result.getCropName()).isEqualTo("Wheat");
        assertThat(result.getFarmerId()).isEqualTo(1L);
        verify(cropRepository, times(1)).save(any(Crop.class));
    }

    @Test
    @DisplayName("createCrop — unknown farmer throws ResourceNotFoundException")
    void createCrop_unknownFarmer_throwsNotFound() {
        when(userRepository.findByEmail("unknown@x.com")).thenReturn(Optional.empty());

        CropDto dto = new CropDto();
        dto.setCropName("Wheat"); dto.setCropType("Cereal");
        dto.setSeason(Season.RABI); dto.setPlantingDate(LocalDate.now());
        dto.setAreaInAcres(5.0);

        assertThatThrownBy(() -> cropService.createCrop(dto, "unknown@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getCropById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCropById — owner can access their crop")
    void getCropById_owner_returnsCropDto() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));

        CropDto result = cropService.getCropById(10L, "farmer@smartagri.com");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCropName()).isEqualTo("Wheat");
    }

    @Test
    @DisplayName("getCropById — non-owner non-admin throws UnauthorizedException")
    void getCropById_nonOwner_throwsUnauthorized() {
        User otherFarmer = User.builder()
                .id(99L).email("other@x.com").role(Role.FARMER)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("other@x.com")).thenReturn(Optional.of(otherFarmer));

        assertThatThrownBy(() -> cropService.getCropById(10L, "other@x.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getCropById — admin can access any crop")
    void getCropById_admin_returnsCropDto() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("admin@smartagri.com")).thenReturn(Optional.of(admin));

        CropDto result = cropService.getCropById(10L, "admin@smartagri.com");

        assertThat(result.getId()).isEqualTo(10L);
    }

    // ─── updateCropStatus ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCropStatus — transitions status correctly")
    void updateCropStatus_owner_updatesStatus() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));

        Crop updated = Crop.builder()
                .id(10L).cropName("Wheat").cropType("Cereal")
                .season(Season.RABI).status(CropStatus.GROWING)
                .plantingDate(crop.getPlantingDate()).areaInAcres(5.0)
                .farmer(farmer)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(cropRepository.save(any(Crop.class))).thenReturn(updated);

        CropDto result = cropService.updateCropStatus(10L, CropStatus.GROWING, "farmer@smartagri.com");

        assertThat(result.getStatus()).isEqualTo(CropStatus.GROWING);
    }

    // ─── getMyCrops ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyCrops — returns all crops for the authenticated farmer")
    void getMyCrops_farmer_returnsCropList() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findByFarmerId(1L)).thenReturn(List.of(crop));

        List<CropDto> result = cropService.getMyCrops("farmer@smartagri.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCropName()).isEqualTo("Wheat");
    }

    // ─── deleteCrop ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCrop — owner deletes crop successfully")
    void deleteCrop_owner_deletesSuccessfully() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));

        cropService.deleteCrop(10L, "farmer@smartagri.com");

        verify(cropRepository, times(1)).delete(crop);
    }
}
