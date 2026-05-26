package com.smartagri.controller;

import com.smartagri.domain.dto.WeatherDto;
import com.smartagri.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for weather data retrieval.
 *
 * <p>All endpoints require a valid JWT Bearer token (enforced by the global
 * {@code SecurityFilterChain}).
 *
 * <ul>
 *   <li>{@code GET /api/weather/current?city=} — current weather conditions</li>
 *   <li>{@code GET /api/weather/forecast?city=} — 7-slot short-range forecast</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Weather", description = "Real-time weather data and short-range forecasts via OpenWeatherMap")
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * GET /api/weather/current?city={city}
     *
     * <p>Returns the current weather conditions for the supplied city.
     * If {@code city} is omitted or blank the service falls back to the
     * configured {@code weather.default-city} value.
     *
     * @param city optional city name (e.g. {@code Pune}, {@code Mumbai})
     * @return 200 OK with a single {@link WeatherDto}
     */
    @GetMapping("/current")
    @Operation(
            summary = "Get current weather",
            description = "Fetches live weather for a given city from OpenWeatherMap. " +
                          "Falls back to the default city when the query parameter is absent."
    )
    public ResponseEntity<WeatherDto> getCurrentWeather(
            @Parameter(description = "City name to query (e.g. Pune). Optional — defaults to configured city.")
            @RequestParam(required = false) String city) {

        WeatherDto dto = weatherService.getCurrentWeather(city);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/weather/forecast?city={city}
     *
     * <p>Returns a short-range forecast (up to 7 three-hour slots) for the
     * supplied city.
     *
     * @param city optional city name
     * @return 200 OK with a list of {@link WeatherDto} forecast entries
     */
    @GetMapping("/forecast")
    @Operation(
            summary = "Get weather forecast",
            description = "Fetches a 7-slot (≈ 21-hour) forecast for a given city from OpenWeatherMap."
    )
    public ResponseEntity<List<WeatherDto>> getForecast(
            @Parameter(description = "City name to query (e.g. Pune). Optional — defaults to configured city.")
            @RequestParam(required = false) String city) {

        List<WeatherDto> forecast = weatherService.getForecast(city);
        return ResponseEntity.ok(forecast);
    }
}
