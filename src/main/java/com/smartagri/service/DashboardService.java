package com.smartagri.service;

import com.smartagri.domain.dto.DashboardStatsDto;

public interface DashboardService {
    
    DashboardStatsDto getFarmerDashboardStats(String email);
    
    DashboardStatsDto getAdminDashboardStats();
}
