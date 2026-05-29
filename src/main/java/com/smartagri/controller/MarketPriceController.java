package com.smartagri.controller;

import com.smartagri.domain.dto.MarketPriceDto;
import com.smartagri.service.MarketPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for commodity market price data from the
 * data.gov.in Agmarknet API.
 *
 * <p>All endpoints require a valid JWT Bearer token (enforced by the global
 * {@code SecurityFilterChain}).
 *
 * <ul>
 *   <li>{@code GET /api/market/prices?crop=} — prices for a specific commodity</li>
 *   <li>{@code GET /api/market/prices/all}   — latest prices for all commodities</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Market Prices", description = "Commodity market price data from data.gov.in Agmarknet API")
public class MarketPriceController {

    private final MarketPriceService marketPriceService;

    /**
     * GET /api/market/prices?crop={cropName}
     *
     * <p>Returns the latest APMC market prices for the specified commodity.
     * Results are cached for 6 hours; the fallback list is returned when the
     * upstream API is unavailable.
     *
     * @param crop        commodity name to query (e.g. "Wheat", "Rice")
     * @param userDetails injected by Spring Security — confirms the caller is authenticated
     * @return 200 OK with a list of {@link MarketPriceDto} records
     */
    @GetMapping("/prices")
    @Operation(
            summary = "Get market prices for a specific crop",
            description = "Fetches commodity market prices from the data.gov.in Agmarknet API " +
                          "for the specified crop name. Results are cached for 6 hours."
    )
    public ResponseEntity<List<MarketPriceDto>> getPricesForCrop(
            @Parameter(description = "Commodity / crop name to query (e.g. Wheat, Rice, Tomato)")
            @RequestParam String crop,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MarketPriceDto> prices = marketPriceService.getPricesForCrop(crop);
        return ResponseEntity.ok(prices);
    }

    /**
     * GET /api/market/prices/all
     *
     * <p>Returns the latest available prices across all commodities reported
     * by the Agmarknet API. Results are cached for 6 hours.
     *
     * @param userDetails injected by Spring Security — confirms the caller is authenticated
     * @return 200 OK with a list of {@link MarketPriceDto} records
     */
    @GetMapping("/prices/all")
    @Operation(
            summary = "Get latest market prices for all crops",
            description = "Fetches the most recent commodity prices across all commodities " +
                          "from the data.gov.in Agmarknet API. Results are cached for 6 hours."
    )
    public ResponseEntity<List<MarketPriceDto>> getAllLatestPrices(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MarketPriceDto> prices = marketPriceService.getAllLatestPrices();
        return ResponseEntity.ok(prices);
    }
}
