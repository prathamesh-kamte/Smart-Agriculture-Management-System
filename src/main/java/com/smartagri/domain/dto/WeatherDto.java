package com.smartagri.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO representing a weather snapshot for a city — either current conditions
 * or a single entry within a multi-day forecast.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDto {

    /** City name as returned by OpenWeatherMap. */
    private String city;

    /** Current / average temperature in °C (metric units). */
    private double temperature;

    /** "Feels like" temperature in °C. */
    @JsonProperty("feelsLike")
    private double feelsLike;

    /** Relative humidity in percent (0–100). */
    private int humidity;

    /** Wind speed in m/s. */
    private double windSpeed;

    /** Short human-readable description, e.g. "light rain", "clear sky". */
    private String description;

    /** OpenWeatherMap icon code, e.g. "10d". */
    private String icon;

    /** Calendar date this entry represents. */
    private LocalDate date;

    /** {@code true} when the description contains "rain". */
    private boolean isRainy;

    /** {@code true} when the temperature is below 5 °C. */
    private boolean isFrosty;
}
