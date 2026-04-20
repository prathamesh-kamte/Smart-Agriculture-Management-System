package com.smartagri.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.enums.CropStatus;
import com.smartagri.domain.enums.Season;
import com.smartagri.service.CropService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc slice tests for {@link CropController}.
 */
@WebMvcTest(CropController.class)
@ActiveProfiles("dev")
class CropControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CropService cropService;

    private CropDto sampleCrop;

    @BeforeEach
    void setUp() {
        sampleCrop = new CropDto();
        sampleCrop.setId(1L);
        sampleCrop.setCropName("Wheat");
        sampleCrop.setCropType("Cereal");
        sampleCrop.setSeason(Season.RABI);
        sampleCrop.setStatus(CropStatus.PLANTED);
        sampleCrop.setPlantingDate(LocalDate.now().minusDays(10));
        sampleCrop.setAreaInAcres(5.0);
        sampleCrop.setFarmerId(2L);
        sampleCrop.setFarmerName("Ramesh Kumar");
    }

    // ─── GET /api/crops ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "farmer@smartagri.com", roles = "FARMER")
    @DisplayName("GET /api/crops — authenticated farmer receives their crop list")
    void getMyCrops_authenticated_returnsOk() throws Exception {
        when(cropService.getMyCrops("farmer@smartagri.com")).thenReturn(List.of(sampleCrop));

        mockMvc.perform(get("/api/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cropName").value("Wheat"))
                .andExpect(jsonPath("$[0].season").value("RABI"));
    }

    @Test
    @DisplayName("GET /api/crops — unauthenticated returns 401")
    void getMyCrops_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/crops"))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST /api/crops ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "farmer@smartagri.com", roles = "FARMER")
    @DisplayName("POST /api/crops — valid payload returns 201")
    void createCrop_validPayload_returns201() throws Exception {
        CropDto input = new CropDto();
        input.setCropName("Basmati Rice");
        input.setCropType("Cereal");
        input.setSeason(Season.KHARIF);
        input.setPlantingDate(LocalDate.now());
        input.setAreaInAcres(3.5);

        when(cropService.createCrop(any(CropDto.class), eq("farmer@smartagri.com")))
                .thenReturn(sampleCrop);

        mockMvc.perform(post("/api/crops")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "farmer@smartagri.com", roles = "FARMER")
    @DisplayName("POST /api/crops — missing required fields returns 400")
    void createCrop_missingFields_returns400() throws Exception {
        CropDto incomplete = new CropDto();
        // cropName, cropType, season, plantingDate all missing

        mockMvc.perform(post("/api/crops")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest());
    }

    // ─── DELETE /api/crops/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "farmer@smartagri.com", roles = "FARMER")
    @DisplayName("DELETE /api/crops/{id} — owner can delete their crop")
    void deleteCrop_owner_returns204() throws Exception {
        doNothing().when(cropService).deleteCrop(eq(1L), eq("farmer@smartagri.com"));

        mockMvc.perform(delete("/api/crops/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(cropService, times(1)).deleteCrop(1L, "farmer@smartagri.com");
    }

    // ─── GET /api/crops/all (admin) ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@smartagri.com", roles = "ADMIN")
    @DisplayName("GET /api/crops/all — admin receives all crops")
    void getAllCrops_admin_returnsOk() throws Exception {
        when(cropService.getAllCrops()).thenReturn(List.of(sampleCrop));

        mockMvc.perform(get("/api/crops/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "farmer@smartagri.com", roles = "FARMER")
    @DisplayName("GET /api/crops/all — non-admin receives 403")
    void getAllCrops_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/crops/all"))
                .andExpect(status().isForbidden());
    }
}
