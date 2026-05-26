package com.smartagri.service.impl;

import com.smartagri.domain.dto.WeatherDto;
import com.smartagri.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link WeatherService} implementation that calls the OpenWeatherMap REST API.
 *
 * <p>Both endpoints use metric units so temperatures are in °C and wind speed
 * is in m/s.  The {@link RestTemplate} bean is resolved from the Spring context
 * (declared in {@code AppConfig}).
 *
 * <p>Configuration keys (resolved from {@code application.yml}):
 * <ul>
 *   <li>{@code weather.api-key}   – OWM API key (defaults to {@code demo_key})</li>
 *   <li>{@code weather.default-city} – fallback city when none is supplied by the caller</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    // ── Base URL ──────────────────────────────────────────────────────────────
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";

    // ── Injected config values ────────────────────────────────────────────────
    @Value("${weather.api-key}")
    private String apiKey;

    @Value("${weather.default-city}")
    private String defaultCity;

    // ── Collaborators ─────────────────────────────────────────────────────────
    private final RestTemplate restTemplate;

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@code GET /weather?q={city}&appid={apiKey}&units=metric}.
     */
    @Override
    public WeatherDto getCurrentWeather(String city) {
        String resolvedCity = resolveCity(city);
        String url = BASE_URL + "/weather?q={city}&appid={apiKey}&units=metric";

        log.debug("Fetching current weather for city={}", resolvedCity);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                resolvedCity,
                apiKey
        ).getBody();

        return mapCurrentResponse(response, resolvedCity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@code GET /forecast?q={city}&appid={apiKey}&units=metric&cnt=7}.
     * The OWM /forecast endpoint returns data in 3-hour slots; we emit one
     * {@link WeatherDto} per slot (up to {@code cnt=7} entries).
     */
    @Override
    public List<WeatherDto> getForecast(String city) {
        String resolvedCity = resolveCity(city);
        String url = BASE_URL + "/forecast?q={city}&appid={apiKey}&units=metric&cnt=7";

        log.debug("Fetching forecast for city={}", resolvedCity);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                resolvedCity,
                apiKey
        ).getBody();

        return mapForecastResponse(response, resolvedCity);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns {@code city} if non-blank, otherwise falls back to {@link #defaultCity}. */
    private String resolveCity(String city) {
        return (city != null && !city.isBlank()) ? city : defaultCity;
    }

    /**
     * Maps the OWM {@code /weather} JSON response body into a {@link WeatherDto}.
     *
     * <p>Expected top-level keys: {@code name}, {@code main}, {@code wind},
     * {@code weather} (array), {@code dt}.
     */
    @SuppressWarnings("unchecked")
    private WeatherDto mapCurrentResponse(Map<String, Object> body, String city) {
        if (body == null) {
            log.warn("Empty response from OpenWeatherMap /weather for city={}", city);
            return WeatherDto.builder().city(city).build();
        }

        // ── main block ────────────────────────────────────────────────────────
        Map<String, Object> main = (Map<String, Object>) body.get("main");
        double temperature = toDouble(main, "temp");
        double feelsLike   = toDouble(main, "feels_like");
        int    humidity    = toInt(main, "humidity");

        // ── wind block ────────────────────────────────────────────────────────
        Map<String, Object> wind = (Map<String, Object>) body.get("wind");
        double windSpeed = wind != null ? toDouble(wind, "speed") : 0.0;

        // ── weather array (first entry) ───────────────────────────────────────
        List<Map<String, Object>> weatherList =
                (List<Map<String, Object>>) body.get("weather");
        String description = "";
        String icon        = "";
        if (weatherList != null && !weatherList.isEmpty()) {
            Map<String, Object> w = weatherList.get(0);
            description = (String) w.getOrDefault("description", "");
            icon        = (String) w.getOrDefault("icon", "");
        }

        // ── date from unix timestamp ──────────────────────────────────────────
        long dt = toLong(body, "dt");
        LocalDate date = Instant.ofEpochSecond(dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // ── resolved city name (OWM may normalise spelling) ───────────────────
        String cityName = body.containsKey("name") ? (String) body.get("name") : city;

        return buildDto(cityName, temperature, feelsLike, humidity,
                windSpeed, description, icon, date);
    }

    /**
     * Maps the OWM {@code /forecast} JSON response body into a list of
     * {@link WeatherDto} entries.
     *
     * <p>Expected top-level key: {@code list} — an array of forecast slots.
     */
    @SuppressWarnings("unchecked")
    private List<WeatherDto> mapForecastResponse(Map<String, Object> body, String city) {
        List<WeatherDto> result = new ArrayList<>();
        if (body == null) {
            log.warn("Empty response from OpenWeatherMap /forecast for city={}", city);
            return result;
        }

        // ── resolve city name from nested city block ───────────────────────────
        String cityName = city;
        if (body.containsKey("city")) {
            Map<String, Object> cityBlock = (Map<String, Object>) body.get("city");
            cityName = (String) cityBlock.getOrDefault("name", city);
        }

        List<Map<String, Object>> list =
                (List<Map<String, Object>>) body.get("list");
        if (list == null) return result;

        for (Map<String, Object> slot : list) {
            // main block
            Map<String, Object> main  = (Map<String, Object>) slot.get("main");
            double temperature = toDouble(main, "temp");
            double feelsLike   = toDouble(main, "feels_like");
            int    humidity    = toInt(main, "humidity");

            // wind block
            Map<String, Object> wind = (Map<String, Object>) slot.get("wind");
            double windSpeed = wind != null ? toDouble(wind, "speed") : 0.0;

            // weather description
            List<Map<String, Object>> weatherList =
                    (List<Map<String, Object>>) slot.get("weather");
            String description = "";
            String icon        = "";
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> w = weatherList.get(0);
                description = (String) w.getOrDefault("description", "");
                icon        = (String) w.getOrDefault("icon", "");
            }

            // date from unix timestamp
            long dt = toLong(slot, "dt");
            LocalDate date = Instant.ofEpochSecond(dt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            result.add(buildDto(cityName, temperature, feelsLike, humidity,
                    windSpeed, description, icon, date));
        }

        return result;
    }

    /**
     * Constructs a {@link WeatherDto} and derives the {@code isRainy} and
     * {@code isFrosty} flags from the raw values.
     */
    private WeatherDto buildDto(String city, double temperature, double feelsLike,
                                int humidity, double windSpeed,
                                String description, String icon, LocalDate date) {
        boolean isRainy  = description != null &&
                description.toLowerCase().contains("rain");
        boolean isFrosty = temperature < 5.0;

        return WeatherDto.builder()
                .city(city)
                .temperature(temperature)
                .feelsLike(feelsLike)
                .humidity(humidity)
                .windSpeed(windSpeed)
                .description(description)
                .icon(icon)
                .date(date)
                .isRainy(isRainy)
                .isFrosty(isFrosty)
                .build();
    }

    // ── Numeric extraction helpers ─────────────────────────────────────────────

    private double toDouble(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0.0;
        Object val = map.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private int toInt(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0;
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    private long toLong(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return 0L;
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }
}
