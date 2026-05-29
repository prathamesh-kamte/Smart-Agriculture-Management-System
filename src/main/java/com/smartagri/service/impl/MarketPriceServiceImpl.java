package com.smartagri.service.impl;

import com.smartagri.domain.dto.MarketPriceDto;
import com.smartagri.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MarketPriceService} implementation that calls the
 * <a href="https://data.gov.in">data.gov.in</a> Agmarknet commodity price API.
 *
 * <h3>Caching strategy</h3>
 * Results are kept in an in-memory {@link ConcurrentHashMap} for
 * {@value #CACHE_TTL_HOURS} hours per cache key.  This avoids hammering
 * the upstream API on every request while still reflecting daily price
 * changes.  The cache is intentionally simple — no eviction beyond TTL.
 *
 * <h3>Fallback</h3>
 * When the upstream API is unreachable or returns an unexpected payload
 * the service logs the error and returns three hard-coded sample records
 * so the rest of the application can continue to function during testing.
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@code market.api-key} – data.gov.in API key
 *       (defaults to the public demo key)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPriceServiceImpl implements MarketPriceService {

    // ── External API ──────────────────────────────────────────────────────────
    private static final String API_BASE_URL =
            "https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070";

    /** Number of hours a cached result is considered fresh. */
    private static final long CACHE_TTL_HOURS = 6;

    // ── Cache structures ──────────────────────────────────────────────────────
    /** Per-key price cache: cacheKey → list of DTOs. */
    private final ConcurrentHashMap<String, List<MarketPriceDto>> priceCache =
            new ConcurrentHashMap<>();

    /** Per-key timestamp: cacheKey → epoch millis of last population. */
    private final ConcurrentHashMap<String, Long> cacheTimestamps =
            new ConcurrentHashMap<>();

    // ── Collaborators / config ────────────────────────────────────────────────
    private final RestTemplate restTemplate;

    @Value("${market.api-key:579b464db66ec23bdd000001cdd3946e44ce4aad7209ff7b23ac571b}")
    private String apiKey;

    // ═════════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <p>Calls the Agmarknet API with a commodity filter set to {@code cropName}.
     * Results are cached for {@value #CACHE_TTL_HOURS} hours under the key
     * {@code "crop:<cropName>"}.
     */
    @Override
    public List<MarketPriceDto> getPricesForCrop(String cropName) {
        String cacheKey = "crop:" + cropName.trim().toLowerCase();

        if (isCacheValid(cacheKey)) {
            log.debug("Market price cache HIT for key={}", cacheKey);
            return priceCache.get(cacheKey);
        }

        log.debug("Market price cache MISS for key={} — calling upstream API", cacheKey);

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .queryParam("api-key", apiKey)
                .queryParam("format", "json")
                .queryParam("filters[commodity]", cropName.trim())
                .queryParam("limit", 20)
                .build(true)
                .toUriString();

        List<MarketPriceDto> results = fetchFromApi(url, cropName);
        storeInCache(cacheKey, results);
        return results;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls the Agmarknet API without a commodity filter to retrieve the
     * latest prices across all commodities. Results are cached under the key
     * {@code "all"}.
     */
    @Override
    public List<MarketPriceDto> getAllLatestPrices() {
        String cacheKey = "all";

        if (isCacheValid(cacheKey)) {
            log.debug("Market price cache HIT for key={}", cacheKey);
            return priceCache.get(cacheKey);
        }

        log.debug("Market price cache MISS for key={} — calling upstream API", cacheKey);

        String url = UriComponentsBuilder.fromHttpUrl(API_BASE_URL)
                .queryParam("api-key", apiKey)
                .queryParam("format", "json")
                .queryParam("limit", 50)
                .build(true)
                .toUriString();

        List<MarketPriceDto> results = fetchFromApi(url, null);
        storeInCache(cacheKey, results);
        return results;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – API call & parsing
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Calls the upstream REST API and maps the JSON response to a list of
     * {@link MarketPriceDto}.  Returns {@link #buildFallbackPrices(String)} if
     * the call fails or the response is unreadable.
     *
     * @param url       fully-built request URL (with query params)
     * @param cropName  commodity name used for fallback labelling; may be null
     */
    @SuppressWarnings("unchecked")
    private List<MarketPriceDto> fetchFromApi(String url, String cropName) {
        try {
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();

            if (response == null) {
                log.warn("Empty response from Agmarknet API — returning fallback data");
                return buildFallbackPrices(cropName);
            }

            Object recordsObj = response.get("records");
            if (!(recordsObj instanceof List)) {
                log.warn("Unexpected 'records' type in Agmarknet response — returning fallback");
                return buildFallbackPrices(cropName);
            }

            List<Map<String, Object>> records = (List<Map<String, Object>>) recordsObj;
            List<MarketPriceDto> result = new ArrayList<>(records.size());

            for (Map<String, Object> record : records) {
                try {
                    result.add(parseRecord(record));
                } catch (Exception e) {
                    log.warn("Failed to parse Agmarknet record {}: {}", record, e.getMessage());
                }
            }

            if (result.isEmpty()) {
                log.info("Agmarknet returned 0 parseable records — returning fallback data");
                return buildFallbackPrices(cropName);
            }

            log.info("Fetched {} market price records from Agmarknet", result.size());
            return result;

        } catch (RestClientException e) {
            log.error("Agmarknet API call failed: {} — returning fallback data", e.getMessage());
            return buildFallbackPrices(cropName);
        }
    }

    /**
     * Maps a single JSON record map to a {@link MarketPriceDto}.
     *
     * <p>Expected keys (from the data.gov.in Agmarknet schema):
     * <ul>
     *   <li>{@code commodity}   – crop name</li>
     *   <li>{@code market}      – APMC market name</li>
     *   <li>{@code state}       – state name</li>
     *   <li>{@code min_price}   – minimum price string</li>
     *   <li>{@code max_price}   – maximum price string</li>
     *   <li>{@code modal_price} – modal price string</li>
     *   <li>{@code arrival_date}– date string (dd/MM/yyyy)</li>
     * </ul>
     */
    private MarketPriceDto parseRecord(Map<String, Object> record) {
        return MarketPriceDto.builder()
                .cropName(stringOf(record, "commodity"))
                .market(stringOf(record, "market"))
                .state(stringOf(record, "state"))
                .minPrice(decimalOf(record, "min_price"))
                .maxPrice(decimalOf(record, "max_price"))
                .modalPrice(decimalOf(record, "modal_price"))
                .priceDate(parsePriceDate(stringOf(record, "arrival_date")))
                .unit("Quintal")   // Agmarknet prices are always per quintal
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – cache
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns {@code true} if the cache entry exists and has not expired. */
    private boolean isCacheValid(String key) {
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp == null || !priceCache.containsKey(key)) return false;
        long ageMs = System.currentTimeMillis() - timestamp;
        return ageMs < CACHE_TTL_HOURS * 60 * 60 * 1_000L;
    }

    /** Stores {@code prices} under {@code key} and records the current timestamp. */
    private void storeInCache(String key, List<MarketPriceDto> prices) {
        priceCache.put(key, prices);
        cacheTimestamps.put(key, System.currentTimeMillis());
        log.debug("Cached {} market price records under key={}", prices.size(), key);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – fallback data
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns three hard-coded sample market price records so the API can
     * serve meaningful data during local development or when the upstream is
     * unavailable.
     *
     * @param cropName commodity name; if {@code null} "Wheat" is used
     */
    private List<MarketPriceDto> buildFallbackPrices(String cropName) {
        String commodity = (cropName != null && !cropName.isBlank()) ? cropName : "Wheat";
        LocalDate today = LocalDate.now();

        return List.of(
                MarketPriceDto.builder()
                        .cropName(commodity)
                        .market("Pune APMC")
                        .state("Maharashtra")
                        .minPrice(new BigDecimal("2100.00"))
                        .maxPrice(new BigDecimal("2400.00"))
                        .modalPrice(new BigDecimal("2250.00"))
                        .priceDate(today)
                        .unit("Quintal")
                        .build(),
                MarketPriceDto.builder()
                        .cropName(commodity)
                        .market("Nashik APMC")
                        .state("Maharashtra")
                        .minPrice(new BigDecimal("2050.00"))
                        .maxPrice(new BigDecimal("2380.00"))
                        .modalPrice(new BigDecimal("2200.00"))
                        .priceDate(today)
                        .unit("Quintal")
                        .build(),
                MarketPriceDto.builder()
                        .cropName(commodity)
                        .market("Hubli APMC")
                        .state("Karnataka")
                        .minPrice(new BigDecimal("2000.00"))
                        .maxPrice(new BigDecimal("2350.00"))
                        .modalPrice(new BigDecimal("2175.00"))
                        .priceDate(today)
                        .unit("Quintal")
                        .build()
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – type coercion
    // ═════════════════════════════════════════════════════════════════════════

    /** Safely extracts a String value from the record map; returns {@code ""} if absent. */
    private String stringOf(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : "";
    }

    /**
     * Parses a numeric string to {@link BigDecimal}.
     * Commas are stripped before parsing (e.g. {@code "2,100"} → {@code 2100}).
     * Returns {@link BigDecimal#ZERO} on failure.
     */
    private BigDecimal decimalOf(Map<String, Object> map, String key) {
        String raw = stringOf(map, key).replace(",", "");
        if (raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            log.warn("Could not parse '{}' as BigDecimal for key '{}'", raw, key);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Parses the Agmarknet date format {@code dd/MM/yyyy}.
     * Returns {@link LocalDate#now()} as a safe fallback on any parse error.
     */
    private LocalDate parsePriceDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
        try {
            // Agmarknet format: dd/MM/yyyy
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                int day   = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim());
                int year  = Integer.parseInt(parts[2].trim());
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            log.warn("Could not parse arrival_date '{}': {}", dateStr, e.getMessage());
        }
        return LocalDate.now();
    }
}
