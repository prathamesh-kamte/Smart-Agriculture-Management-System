package com.smartagri.service;

import com.smartagri.domain.dto.MarketPriceDto;

import java.util.List;

/**
 * Contract for retrieving commodity market price data from the
 * data.gov.in Agmarknet API.
 *
 * <p>Implementations are expected to cache results to avoid redundant
 * external calls and to provide graceful fallback data when the upstream
 * API is unavailable.
 */
public interface MarketPriceService {

    /**
     * Fetch current market prices for a specific commodity.
     *
     * @param cropName the commodity name to query (e.g. "Wheat", "Rice")
     * @return list of {@link MarketPriceDto} records across markets;
     *         never {@code null} — returns fallback data on API failure
     */
    List<MarketPriceDto> getPricesForCrop(String cropName);

    /**
     * Fetch the latest available prices across all commodities.
     *
     * @return list of {@link MarketPriceDto} records;
     *         never {@code null} — returns fallback data on API failure
     */
    List<MarketPriceDto> getAllLatestPrices();
}
