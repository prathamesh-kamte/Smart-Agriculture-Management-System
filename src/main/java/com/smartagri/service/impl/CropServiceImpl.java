package com.smartagri.service.impl;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.dto.YieldAnalyticsDto;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.exception.UnauthorizedException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.ExpenseRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.CropService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CropServiceImpl implements CropService {

    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    @Transactional
    public CropDto createCrop(CropDto dto, String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);

        Crop crop = Crop.builder()
                .cropName(dto.getCropName())
                .cropType(dto.getCropType())
                .season(dto.getSeason())
                .status(dto.getStatus() != null ? dto.getStatus() : CropStatus.PLANTED)
                .plantingDate(dto.getPlantingDate())
                .expectedHarvestDate(dto.getExpectedHarvestDate())
                .areaInAcres(dto.getAreaInAcres())
                .notes(dto.getNotes())
                .expectedYieldKg(dto.getExpectedYieldKg())
                .actualYieldKg(dto.getActualYieldKg())
                .sellingPricePerKg(dto.getSellingPricePerKg())
                .farmer(farmer)
                .build();

        Crop saved = cropRepository.save(crop);
        log.info("Crop id={} created by farmer={}", saved.getId(), farmerEmail);
        return toDto(saved);
    }

    @Override
    public CropDto getCropById(Long id, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        return toDto(crop);
    }

    @Override
    public com.smartagri.domain.dto.PageResponse<CropDto> getMyCrops(String farmerEmail, CropStatus status, com.smartagri.domain.enums.Season season, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Crop> page = cropRepository.findByFarmerEmailAndFilters(farmerEmail, status, season, pageable);
        return com.smartagri.domain.dto.PageResponse.of(page, this::toDto);
    }

    @Override
    public List<CropDto> getAllCrops() {
        return cropRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CropDto updateCrop(Long id, CropDto dto, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);

        crop.setCropName(dto.getCropName());
        crop.setCropType(dto.getCropType());
        if (dto.getSeason() != null) {
            crop.setSeason(dto.getSeason());
        }
        crop.setPlantingDate(dto.getPlantingDate());
        crop.setExpectedHarvestDate(dto.getExpectedHarvestDate());
        crop.setActualHarvestDate(dto.getActualHarvestDate());
        crop.setAreaInAcres(dto.getAreaInAcres());
        crop.setNotes(dto.getNotes());
        // Yield tracking fields
        crop.setExpectedYieldKg(dto.getExpectedYieldKg());
        crop.setActualYieldKg(dto.getActualYieldKg());
        crop.setSellingPricePerKg(dto.getSellingPricePerKg());

        return toDto(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public CropDto updateCropStatus(Long id, CropStatus newStatus, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        crop.setStatus(newStatus);
        log.info("Crop id={} status updated to {} by {}", id, newStatus, requesterEmail);
        return toDto(cropRepository.save(crop));
    }

    @Override
    @Transactional
    public void deleteCrop(Long id, String requesterEmail) {
        Crop crop = findCropOrThrow(id);
        assertOwnerOrAdmin(crop, requesterEmail);
        cropRepository.delete(crop);
        log.info("Crop id={} deleted by {}", id, requesterEmail);
    }

    @Override
    public List<YieldAnalyticsDto> getYieldAnalytics(String farmerEmail) {
        User farmer = findUserOrThrow(farmerEmail);
        List<Crop> harvestedCrops = cropRepository.findByFarmerIdAndStatus(
                farmer.getId(), CropStatus.HARVESTED);

        return harvestedCrops.stream()
                .map(this::toYieldAnalyticsDto)
                .collect(Collectors.toList());
    }

    private Crop findCropOrThrow(Long id) {
        return cropRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + id));
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void assertOwnerOrAdmin(Crop crop, String requesterEmail) {
        boolean isAdmin = userRepository.findByEmail(requesterEmail)
                .map(u -> u.getRole().name().equals("ADMIN"))
                .orElse(false);
        if (!isAdmin && !crop.getFarmer().getEmail().equals(requesterEmail)) {
            throw new UnauthorizedException("Access denied to crop id: " + crop.getId());
        }
    }

    CropDto toDto(Crop crop) {
        CropDto dto = new CropDto();
        dto.setId(crop.getId());
        dto.setCropName(crop.getCropName());
        dto.setCropType(crop.getCropType());
        dto.setSeason(crop.getSeason());
        if (crop.getStatus() != null) {
            dto.setStatus(crop.getStatus());
        }
        dto.setPlantingDate(crop.getPlantingDate());
        dto.setExpectedHarvestDate(crop.getExpectedHarvestDate());
        dto.setActualHarvestDate(crop.getActualHarvestDate());
        dto.setAreaInAcres(crop.getAreaInAcres());
        dto.setNotes(crop.getNotes());

        // ── Yield fields ─────────────────────────────────────────────
        dto.setExpectedYieldKg(crop.getExpectedYieldKg());
        dto.setActualYieldKg(crop.getActualYieldKg());
        dto.setSellingPricePerKg(crop.getSellingPricePerKg());

        // ── Profit / loss calculation ─────────────────────────────────
        // Requires all three fields; null-safe — returns null when any is absent.
        if (crop.getActualYieldKg() != null
                && crop.getSellingPricePerKg() != null
                && crop.getId() != null) {
            BigDecimal revenue = BigDecimal.valueOf(crop.getActualYieldKg())
                    .multiply(crop.getSellingPricePerKg());
            BigDecimal totalExpenses = expenseRepository.sumByCropId(crop.getId());
            dto.setProfitLoss(revenue.subtract(totalExpenses));
        } else {
            dto.setProfitLoss(null);
        }

        if (crop.getFarmer() != null) {
            dto.setFarmerId(crop.getFarmer().getId());
            dto.setFarmerName(crop.getFarmer().getFullName());
        }
        return dto;
    }

    /**
     * Converts a HARVESTED {@link Crop} into a {@link YieldAnalyticsDto}.
     * All financial and efficiency calculations are performed null-safely;
     * any field that cannot be computed is left as {@code null}.
     */
    private YieldAnalyticsDto toYieldAnalyticsDto(Crop crop) {
        // ── Yield efficiency ──────────────────────────────────────────────────
        Double yieldEfficiency = null;
        if (crop.getActualYieldKg() != null
                && crop.getExpectedYieldKg() != null
                && crop.getExpectedYieldKg() != 0.0) {
            yieldEfficiency = (crop.getActualYieldKg() / crop.getExpectedYieldKg()) * 100.0;
        }

        // ── Revenue ───────────────────────────────────────────────────────────
        BigDecimal revenue = null;
        if (crop.getActualYieldKg() != null && crop.getSellingPricePerKg() != null) {
            revenue = BigDecimal.valueOf(crop.getActualYieldKg())
                    .multiply(crop.getSellingPricePerKg());
        }

        // ── Total expenses (COALESCE in query guarantees non-null) ─────────────
        BigDecimal totalExpenses = expenseRepository.sumByCropId(crop.getId());

        // ── Profit / loss ─────────────────────────────────────────────────────
        BigDecimal profitLoss = null;
        String profitStatus = null;
        if (revenue != null) {
            profitLoss = revenue.subtract(totalExpenses);
            int cmp = profitLoss.compareTo(BigDecimal.ZERO);
            profitStatus = cmp > 0 ? "PROFIT" : cmp < 0 ? "LOSS" : "BREAK_EVEN";
        }

        return YieldAnalyticsDto.builder()
                .cropId(crop.getId())
                .cropName(crop.getCropName())
                .season(crop.getSeason() != null ? crop.getSeason().name() : null)
                .expectedYieldKg(crop.getExpectedYieldKg())
                .actualYieldKg(crop.getActualYieldKg())
                .yieldEfficiencyPercent(yieldEfficiency)
                .totalExpenses(totalExpenses)
                .revenue(revenue)
                .profitLoss(profitLoss)
                .profitStatus(profitStatus)
                .build();
    }
}
