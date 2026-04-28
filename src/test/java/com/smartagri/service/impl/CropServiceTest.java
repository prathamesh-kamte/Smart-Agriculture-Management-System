package com.smartagri.service.impl;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.enums.Role;
import com.smartagri.entity.Crop;
import com.smartagri.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CropServiceTest {

    @Mock
    private CropRepository cropRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CropServiceImpl cropService;

    private User farmer;
    private User admin;
    private Crop crop;

    @BeforeEach
    void setUp() {
        farmer = User.builder()
                .id(1L)
                .email("farmer@smartagri.com")
                .role(Role.FARMER)
                .build();

        admin = User.builder()
                .id(2L)
                .email("admin@smartagri.com")
                .role(Role.ADMIN)
                .build();

        crop = Crop.builder()
                .id(10L)
                .cropName("Wheat")
                .farmer(farmer)
                .status(com.smartagri.entity.CropStatus.PLANTED)
                .build();
    }

    @Test
    void createCrop_validInput_returnsCropDto() {
        CropDto inputDto = new CropDto();
        inputDto.setCropName("Wheat");

        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.save(any(Crop.class))).thenReturn(crop);

        CropDto result = cropService.createCrop(inputDto, "farmer@smartagri.com");

        assertThat(result.getCropName()).isEqualTo("Wheat");
        assertThat(result.getFarmerId()).isEqualTo(1L);
    }

    @Test
    void createCrop_unknownFarmer_throwsNotFound() {
        CropDto inputDto = new CropDto();
        when(userRepository.findByEmail("unknown@smartagri.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cropService.createCrop(inputDto, "unknown@smartagri.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCropById_owner_returnsCropDto() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));

        CropDto result = cropService.getCropById(10L, "farmer@smartagri.com");

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getCropById_nonOwner_throwsUnauthorized() {
        User otherUser = User.builder().id(3L).email("other@smartagri.com").role(Role.FARMER).build();

        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("other@smartagri.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> cropService.getCropById(10L, "other@smartagri.com"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCropById_admin_returnsCropDto() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("admin@smartagri.com")).thenReturn(Optional.of(admin));

        CropDto result = cropService.getCropById(10L, "admin@smartagri.com");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void updateCropStatus_transitions_correctly() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        
        Crop updatedCrop = Crop.builder()
                .id(10L)
                .cropName("Wheat")
                .farmer(farmer)
                .status(com.smartagri.entity.CropStatus.GROWING)
                .build();
                
        when(cropRepository.save(any(Crop.class))).thenReturn(updatedCrop);

        CropDto result = cropService.updateCropStatus(10L, com.smartagri.domain.enums.CropStatus.GROWING, "farmer@smartagri.com");

        assertThat(result.getStatus()).isEqualTo(com.smartagri.domain.enums.CropStatus.GROWING);
    }

    @Test
    void getMyCrops_farmer_returnsList() {
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));
        when(cropRepository.findByFarmerId(1L)).thenReturn(List.of(crop));

        List<CropDto> result = cropService.getMyCrops("farmer@smartagri.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCropName()).isEqualTo("Wheat");
    }

    @Test
    void deleteCrop_owner_deletesSuccessfully() {
        when(cropRepository.findById(10L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("farmer@smartagri.com")).thenReturn(Optional.of(farmer));

        cropService.deleteCrop(10L, "farmer@smartagri.com");

        verify(cropRepository, times(1)).delete(crop);
    }
}
