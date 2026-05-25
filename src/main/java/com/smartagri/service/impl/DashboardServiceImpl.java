package com.smartagri.service.impl;

import com.smartagri.domain.dto.DashboardStatsDto;
import com.smartagri.domain.dto.MonthlyExpenseTrendDto;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.AdvisoryRepository;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.ExpenseRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final CropRepository cropRepository;
    private final ExpenseRepository expenseRepository;
    private final AdvisoryRepository advisoryRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardStatsDto getFarmerDashboardStats(String email) {
        User farmer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Long farmerId = farmer.getId();

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate sixMonthsAgo = startOfMonth.minusMonths(5); // Including current month, so minus 5

        long totalCrops = cropRepository.countByFarmerId(farmerId);
        long activeCrops = cropRepository.countActiveCropsByFarmerId(farmerId);
        BigDecimal totalExpensesThisMonth = expenseRepository.sumExpensesForFarmerInDateRange(farmerId, startOfMonth, endOfMonth);
        long pendingAdvisories = advisoryRepository.countByFarmerIdAndAcknowledgedFalse(farmerId);

        List<Object[]> cropsByStatusRaw = cropRepository.countCropsByStatusForFarmer(farmerId);
        Map<String, Long> cropsByStatus = mapCropsByStatus(cropsByStatusRaw);

        List<Object[]> monthlyTrendRaw = expenseRepository.getMonthlyTrendForFarmer(farmerId, sixMonthsAgo);
        List<MonthlyExpenseTrendDto> monthlyTrend = mapMonthlyTrend(monthlyTrendRaw, sixMonthsAgo);

        return DashboardStatsDto.builder()
                .totalCrops(totalCrops)
                .activeCrops(activeCrops)
                .totalExpensesThisMonth(totalExpensesThisMonth)
                .pendingAdvisories(pendingAdvisories)
                .cropsByStatus(cropsByStatus)
                .monthlyExpenseTrend(monthlyTrend)
                .build();
    }

    @Override
    public DashboardStatsDto getAdminDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate sixMonthsAgo = startOfMonth.minusMonths(5);

        long totalCrops = cropRepository.count();
        long activeCrops = cropRepository.countAllActiveCrops();
        BigDecimal totalExpensesThisMonth = expenseRepository.sumAllExpensesInDateRange(startOfMonth, endOfMonth);
        long pendingAdvisories = advisoryRepository.countByAcknowledgedFalse();

        List<Object[]> cropsByStatusRaw = cropRepository.countAllCropsByStatus();
        Map<String, Long> cropsByStatus = mapCropsByStatus(cropsByStatusRaw);

        List<Object[]> monthlyTrendRaw = expenseRepository.getMonthlyTrendForAdmin(sixMonthsAgo);
        List<MonthlyExpenseTrendDto> monthlyTrend = mapMonthlyTrend(monthlyTrendRaw, sixMonthsAgo);

        return DashboardStatsDto.builder()
                .totalCrops(totalCrops)
                .activeCrops(activeCrops)
                .totalExpensesThisMonth(totalExpensesThisMonth)
                .pendingAdvisories(pendingAdvisories)
                .cropsByStatus(cropsByStatus)
                .monthlyExpenseTrend(monthlyTrend)
                .build();
    }

    private Map<String, Long> mapCropsByStatus(List<Object[]> rawList) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rawList) {
            CropStatus status = (CropStatus) row[0];
            Long count = (Long) row[1];
            map.put(status.name(), count);
        }
        return map;
    }

    private List<MonthlyExpenseTrendDto> mapMonthlyTrend(List<Object[]> rawList, LocalDate startDate) {
        // Initialize the last 6 months with 0
        Map<String, BigDecimal> trendMap = new java.util.LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        for (int i = 0; i < 6; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            trendMap.put(monthDate.format(formatter), BigDecimal.ZERO);
        }

        // Fill with actual data
        for (Object[] row : rawList) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            BigDecimal amount = (BigDecimal) row[2];

            LocalDate date = LocalDate.of(year, month, 1);
            String formattedMonth = date.format(formatter);

            if (trendMap.containsKey(formattedMonth)) {
                trendMap.put(formattedMonth, amount);
            }
        }

        List<MonthlyExpenseTrendDto> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : trendMap.entrySet()) {
            result.add(new MonthlyExpenseTrendDto(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
