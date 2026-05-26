package com.smartagri.service;

import com.smartagri.domain.dto.WeatherDto;

import java.util.List;

/**
 * Contract for weather data retrieval from an external provider.
 */
public interface WeatherService {

    /**
     * Fetch the current weather conditions for the given city.
     *
     * @param city city name (e.g. "Pune")
     * @return populated {@link WeatherDto} for today
     */
    WeatherDto getCurrentWeather(String city);

    /**
     * Fetch a short-range forecast for the given city.
     *
     * @param city city name (e.g. "Pune")
     * @return list of {@link WeatherDto} entries, one per forecast period
     */
    List<WeatherDto> getForecast(String city);
}
