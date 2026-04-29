package com.smartagri.controller;

import com.smartagri.domain.dto.DashboardStatsDto;
import com.smartagri.domain.dto.MonthlyExpenseTrendDto;
import com.smartagri.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @WithMockUser(username = "farmer@example.com", roles = "FARMER")
    void getDashboardStats_asFarmer_returnsStats() throws Exception {
        DashboardStatsDto dto = DashboardStatsDto.builder()
                .totalCrops(10)
                .activeCrops(5)
                .totalExpensesThisMonth(new BigDecimal("1000.50"))
                .pendingAdvisories(2)
                .cropsByStatus(Map.of("GROWING", 3L, "PLANTED", 2L))
                .monthlyExpenseTrend(List.of(new MonthlyExpenseTrendDto("Oct 2023", new BigDecimal("500.00"))))
                .build();

        when(dashboardService.getFarmerDashboardStats("farmer@example.com")).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCrops").value(10))
                .andExpect(jsonPath("$.activeCrops").value(5))
                .andExpect(jsonPath("$.totalExpensesThisMonth").value(1000.50))
                .andExpect(jsonPath("$.pendingAdvisories").value(2))
                .andExpect(jsonPath("$.cropsByStatus.GROWING").value(3))
                .andExpect(jsonPath("$.monthlyExpenseTrend[0].month").value("Oct 2023"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAdminDashboardStats_asAdmin_returnsSystemStats() throws Exception {
        DashboardStatsDto dto = DashboardStatsDto.builder()
                .totalCrops(100)
                .activeCrops(50)
                .totalExpensesThisMonth(new BigDecimal("25000.00"))
                .pendingAdvisories(12)
                .cropsByStatus(Map.of("GROWING", 30L, "PLANTED", 20L))
                .monthlyExpenseTrend(List.of(new MonthlyExpenseTrendDto("Oct 2023", new BigDecimal("15000.00"))))
                .build();

        when(dashboardService.getAdminDashboardStats()).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCrops").value(100))
                .andExpect(jsonPath("$.activeCrops").value(50));
    }

    @Test
    @WithMockUser(username = "farmer@example.com", roles = "FARMER")
    void getAdminDashboardStats_asFarmer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardStats_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
