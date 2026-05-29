package com.smartagri.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only DTO representing a single market price record for a commodity.
 *
 * <p>Fields map directly to the data.gov.in Agmarknet API response fields.
 * Monetary values are in INR per {@link #unit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceDto {

    /** Commodity / crop name as reported by the market (e.g. "Wheat", "Rice"). */
    private String cropName;

    /** APMC market name where the price was recorded (e.g. "Pune"). */
    private String market;

    /** State in which the market is located (e.g. "Maharashtra"). */
    private String state;

    /** Lowest traded price on the given date (INR per unit). */
    private BigDecimal minPrice;

    /** Highest traded price on the given date (INR per unit). */
    private BigDecimal maxPrice;

    /**
     * Modal (most frequently occurring) price on the given date (INR per unit).
     * This is the most representative single price for that session.
     */
    private BigDecimal modalPrice;

    /** Date for which this price record is valid. */
    private LocalDate priceDate;

    /** Unit of measurement for the prices (e.g. "Quintal", "Kg"). */
    private String unit;
}
