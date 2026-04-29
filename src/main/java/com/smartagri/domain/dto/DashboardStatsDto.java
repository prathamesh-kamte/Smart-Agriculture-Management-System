package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalCrops;
    private long activeCrops;
    private BigDecimal totalExpensesThisMonth;
    private long pendingAdvisories;
    private Map<String, Long> cropsByStatus;
    private List<MonthlyExpenseTrendDto> monthlyExpenseTrend;
}
