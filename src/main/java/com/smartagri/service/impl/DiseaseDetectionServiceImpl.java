package com.smartagri.service.impl;

import com.smartagri.domain.dto.DiseaseDetectionDto;
import com.smartagri.domain.entity.Crop;
import com.smartagri.domain.entity.DiseaseDetection;
import com.smartagri.domain.entity.User;
import com.smartagri.exception.ResourceNotFoundException;
import com.smartagri.repository.CropRepository;
import com.smartagri.repository.DiseaseDetectionRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.service.DiseaseDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link DiseaseDetectionService} implementation.
 *
 * <h3>PlantNet mode ({@code plantnet.enabled=true})</h3>
 * Posts the image to {@code https://my-api.plantnet.org/v2/identify/all},
 * extracts the top result's species name and confidence score, maps it to
 * a disease profile from an in-memory lookup table, and persists the result.
 *
 * <h3>Mock mode ({@code plantnet.enabled=false})</h3>
 * Returns a rotating selection of three common crop disease mock records so
 * the UI and downstream features can be developed without live API access.
 *
 * <h3>Fallback rules</h3>
 * <ul>
 *   <li>API call throws exception → return "unable to identify" fallback.</li>
 *   <li>API returns confidence &lt; 0.30 → return same fallback.</li>
 *   <li>Disease name not in lookup table → return generic advice.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseDetectionServiceImpl implements DiseaseDetectionService {

    // ── Config ────────────────────────────────────────────────────────────────

    private static final String PLANTNET_URL =
            "https://my-api.plantnet.org/v2/identify/all";
    private static final double MIN_CONFIDENCE = 0.30;

    @Value("${plantnet.api-key:}")
    private String apiKey;

    @Value("${plantnet.enabled:false}")
    private boolean plantnetEnabled;

    // ── Collaborators ─────────────────────────────────────────────────────────

    private final RestTemplate              restTemplate;
    private final UserRepository            userRepository;
    private final CropRepository            cropRepository;
    private final DiseaseDetectionRepository detectionRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Disease knowledge base
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Maps lower-case disease / species keyword fragments to a treatment profile.
     * Keys are matched by {@link String#contains} against the PlantNet species
     * name (case-insensitive), so broad genus-level keys work too.
     */
    private static final Map<String, DiseaseProfile> DISEASE_PROFILES;

    static {
        Map<String, DiseaseProfile> m = new LinkedHashMap<>();

        m.put("blast", new DiseaseProfile(
                "Rice Blast (Magnaporthe oryzae)",
                "HIGH",
                "Fungal disease causing diamond-shaped lesions on leaves, leading to crop failure if untreated.",
                List.of(
                        "Apply tricyclazole or carbendazim fungicide immediately",
                        "Remove and destroy infected plant parts",
                        "Drain standing water from fields to reduce humidity",
                        "Avoid excessive nitrogen fertilisation"
                ),
                List.of(
                        "Use blast-resistant rice varieties",
                        "Maintain proper plant spacing for air circulation",
                        "Apply silicon-based soil amendments",
                        "Monitor fields regularly during humid conditions"
                )
        ));

        m.put("blight", new DiseaseProfile(
                "Leaf Blight",
                "HIGH",
                "Bacterial or fungal blight causing rapid browning and wilting of leaves and stems.",
                List.of(
                        "Apply copper-based bactericide or mancozeb fungicide",
                        "Remove infected leaves and burn them away from the field",
                        "Irrigate at soil level to keep foliage dry",
                        "Consult agronomist for systemic fungicide recommendation"
                ),
                List.of(
                        "Use certified disease-free seeds",
                        "Rotate crops — avoid planting the same species consecutively",
                        "Ensure proper drainage to prevent waterlogging",
                        "Sanitise farm equipment between fields"
                )
        ));

        m.put("rust", new DiseaseProfile(
                "Wheat / Leaf Rust (Puccinia spp.)",
                "MEDIUM",
                "Fungal rust disease causing orange-brown pustules on leaves, reducing photosynthesis and yield.",
                List.of(
                        "Apply propiconazole or tebuconazole fungicide at first sign",
                        "Repeat fungicide application after 14 days if infection persists",
                        "Remove heavily infected plants from the field",
                        "Monitor neighbouring fields for spread"
                ),
                List.of(
                        "Plant rust-resistant wheat varieties",
                        "Avoid late planting — early-season crops have lower rust risk",
                        "Keep field edges weed-free to reduce inoculum sources",
                        "Apply balanced potassium fertilisation to strengthen cell walls"
                )
        ));

        m.put("wilt", new DiseaseProfile(
                "Fusarium Wilt",
                "HIGH",
                "Soil-borne fungal disease causing yellowing, wilting, and eventual plant death by blocking vascular tissue.",
                List.of(
                        "Remove and destroy infected plants immediately — do not compost",
                        "Drench soil with carbendazim or thiophanate-methyl solution",
                        "Apply Trichoderma-based bio-fungicide to surrounding soil",
                        "Discontinue planting susceptible crops in affected plots for 2+ seasons"
                ),
                List.of(
                        "Use Fusarium-resistant or grafted rootstock varieties",
                        "Solarise soil before planting to reduce fungal load",
                        "Maintain soil pH between 6.5 and 7.0",
                        "Avoid overwatering and improve field drainage"
                )
        ));

        m.put("mosaic", new DiseaseProfile(
                "Mosaic Virus",
                "MEDIUM",
                "Viral disease spread by aphids causing mottled yellow-green leaf patterns and stunted growth.",
                List.of(
                        "Remove and destroy infected plants immediately",
                        "Control aphid vectors using imidacloprid or neem-based insecticide",
                        "Wash hands and tools after handling infected plants",
                        "There is no chemical cure — focus on vector control"
                ),
                List.of(
                        "Plant virus-resistant varieties",
                        "Use reflective mulches to deter aphids",
                        "Introduce natural aphid predators (ladybirds, lacewings)",
                        "Keep weeds controlled as alternate virus hosts"
                )
        ));

        m.put("powdery mildew", new DiseaseProfile(
                "Powdery Mildew",
                "LOW",
                "Fungal disease creating white powdery coating on leaves, reducing photosynthesis.",
                List.of(
                        "Apply sulphur-based or potassium bicarbonate fungicide",
                        "Spray neem oil solution (5 ml per litre) every 7 days",
                        "Improve air circulation by pruning dense canopy",
                        "Water at soil level in the morning only"
                ),
                List.of(
                        "Choose mildew-resistant varieties where available",
                        "Avoid excessive nitrogen fertilisation",
                        "Maintain adequate plant spacing",
                        "Apply preventive sulphur spray at season start"
                )
        ));

        m.put("downy mildew", new DiseaseProfile(
                "Downy Mildew",
                "MEDIUM",
                "Water mould disease causing yellow patches on upper leaf surface and grey mould below.",
                List.of(
                        "Apply metalaxyl-M or fosetyl-aluminium fungicide",
                        "Remove infected leaves and dispose off-site",
                        "Reduce humidity by improving ventilation",
                        "Avoid overhead irrigation"
                ),
                List.of(
                        "Plant resistant varieties",
                        "Rotate crops every season",
                        "Avoid working in wet fields to reduce spread",
                        "Apply preventive copper hydroxide spray"
                )
        ));

        DISEASE_PROFILES = Collections.unmodifiableMap(m);
    }

    // ── Fallback / unknown profile ────────────────────────────────────────────

    private static final DiseaseProfile UNKNOWN_PROFILE = new DiseaseProfile(
            "Unable to identify - please consult agronomist",
            "UNKNOWN",
            "The AI model could not confidently identify a known disease. "
                    + "Submit a clearer, well-lit photo or consult a certified agronomist.",
            List.of(
                    "Take a clear, well-lit close-up photo of the affected area",
                    "Consult a local agronomist or Krishi Vigyan Kendra",
                    "Isolate potentially infected plants as a precaution"
            ),
            List.of(
                    "Regular scouting — inspect crops at least twice a week",
                    "Maintain a field diary to track symptoms and their progression",
                    "Use certified disease-free planting material"
            )
    );

    // ═════════════════════════════════════════════════════════════════════════
    // DiseaseDetectionService implementation
    // ═════════════════════════════════════════════════════════════════════════

    /** {@inheritDoc} */
    @Override
    @Transactional
    public DiseaseDetectionDto analyzePhoto(MultipartFile photo, Long cropId,
                                             String farmerEmail) {
        User farmer = findUser(farmerEmail);
        Crop crop   = (cropId != null) ? findCrop(cropId) : null;

        DiseaseDetectionDto result;

        if (!plantnetEnabled) {
            log.info("PlantNet disabled — returning mock disease data for farmer={}", farmerEmail);
            result = buildMockResult(cropId, farmer.getId());
        } else {
            result = callPlantNetApi(photo, cropId, farmer.getId());
        }

        // Persist the detection record
        DiseaseDetection entity = DiseaseDetection.builder()
                .crop(crop)
                .farmer(farmer)
                .photoUrl(result.getPhotoUrl())
                .diseaseName(result.getDiseaseName())
                .confidence(result.getConfidence())
                .severity(result.getSeverity())
                .description(result.getDescription())
                .treatments(join(result.getTreatments()))
                .preventions(join(result.getPreventions()))
                .detectedAt(LocalDateTime.now())
                .build();

        DiseaseDetection saved = detectionRepository.save(entity);
        result.setId(saved.getId());
        result.setDetectedAt(saved.getDetectedAt());
        result.setFarmerId(farmer.getId());

        log.info("Saved disease detection id={} disease='{}' for farmer={}",
                saved.getId(), saved.getDiseaseName(), farmerEmail);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<DiseaseDetectionDto> getHistoryForFarmer(String farmerEmail) {
        User farmer = findUser(farmerEmail);
        return detectionRepository
                .findByFarmerIdOrderByDetectedAtDesc(farmer.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<DiseaseDetectionDto> getHistoryForCrop(Long cropId, String farmerEmail) {
        findUser(farmerEmail); // ensures authenticated user exists
        return detectionRepository
                .findByCropIdOrderByDetectedAtDesc(cropId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – PlantNet API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Calls the PlantNet API, parses the response, and builds a DTO.
     * Returns the {@link #UNKNOWN_PROFILE} fallback on any error or low confidence.
     */
    @SuppressWarnings("unchecked")
    private DiseaseDetectionDto callPlantNetApi(MultipartFile photo, Long cropId, Long farmerId) {
        try {
            String url = PLANTNET_URL + "?api-key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename() != null
                            ? photo.getOriginalFilename() : "photo.jpg";
                }
            };
            body.add("images", fileResource);
            body.add("organs", "leaf");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() == null) {
                log.warn("PlantNet returned empty body — using fallback");
                return buildFallbackDto(cropId, farmerId);
            }

            // Parse: response.results[0].species.scientificNameWithoutAuthor + score
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("results");
            if (results == null || results.isEmpty()) {
                log.warn("PlantNet returned no results — using fallback");
                return buildFallbackDto(cropId, farmerId);
            }

            Map<String, Object> top = results.get(0);
            double score = top.containsKey("score")
                    ? ((Number) top.get("score")).doubleValue() : 0.0;

            if (score < MIN_CONFIDENCE) {
                log.info("PlantNet confidence {:.1f}% below threshold — using fallback", score * 100);
                return buildFallbackDto(cropId, farmerId);
            }

            Map<String, Object> species = (Map<String, Object>) top.get("species");
            String speciesName = species != null
                    ? (String) species.getOrDefault("scientificNameWithoutAuthor", "Unknown")
                    : "Unknown";

            String confidenceStr = String.format("%.1f%%", score * 100);
            DiseaseProfile profile = matchProfile(speciesName);

            return DiseaseDetectionDto.builder()
                    .diseaseName(profile.name)
                    .confidence(confidenceStr)
                    .severity(profile.severity)
                    .description(profile.description)
                    .treatments(new ArrayList<>(profile.treatments))
                    .preventions(new ArrayList<>(profile.preventions))
                    .cropId(cropId)
                    .farmerId(farmerId)
                    .build();

        } catch (RestClientException e) {
            log.error("PlantNet API call failed: {} — using fallback", e.getMessage());
            return buildFallbackDto(cropId, farmerId);
        } catch (IOException e) {
            log.error("Failed to read uploaded photo bytes: {} — using fallback", e.getMessage());
            return buildFallbackDto(cropId, farmerId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – mock & fallback
    // ═════════════════════════════════════════════════════════════════════════

    /** 3 rotating mock diseases for dev/test mode. */
    private static final List<DiseaseProfile> MOCK_DISEASES = List.of(
            DISEASE_PROFILES.get("blast"),
            DISEASE_PROFILES.get("rust"),
            DISEASE_PROFILES.get("powdery mildew")
    );

    private static int mockIndex = 0;

    private DiseaseDetectionDto buildMockResult(Long cropId, Long farmerId) {
        DiseaseProfile p = MOCK_DISEASES.get(mockIndex % MOCK_DISEASES.size());
        mockIndex++;
        return DiseaseDetectionDto.builder()
                .diseaseName("[MOCK] " + p.name)
                .confidence("92.0%")
                .severity(p.severity)
                .description("[Test mode] " + p.description)
                .treatments(new ArrayList<>(p.treatments))
                .preventions(new ArrayList<>(p.preventions))
                .cropId(cropId)
                .farmerId(farmerId)
                .build();
    }

    private DiseaseDetectionDto buildFallbackDto(Long cropId, Long farmerId) {
        return DiseaseDetectionDto.builder()
                .diseaseName(UNKNOWN_PROFILE.name)
                .confidence("< 30%")
                .severity(UNKNOWN_PROFILE.severity)
                .description(UNKNOWN_PROFILE.description)
                .treatments(new ArrayList<>(UNKNOWN_PROFILE.treatments))
                .preventions(new ArrayList<>(UNKNOWN_PROFILE.preventions))
                .cropId(cropId)
                .farmerId(farmerId)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers – lookup, mapping, conversion
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Scans the disease profile map for a key that is a substring of the
     * species name (case-insensitive).  Returns {@link #UNKNOWN_PROFILE} if
     * no match is found.
     */
    private DiseaseProfile matchProfile(String speciesName) {
        String lower = speciesName.toLowerCase();
        for (Map.Entry<String, DiseaseProfile> entry : DISEASE_PROFILES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        log.debug("No disease profile matched for species '{}' — using unknown profile", speciesName);
        // Return a generic profile carrying the actual species name
        return new DiseaseProfile(
                speciesName,
                "MEDIUM",
                "Detected: " + speciesName + ". No specific treatment data available. Consult an agronomist.",
                new ArrayList<>(UNKNOWN_PROFILE.treatments),
                new ArrayList<>(UNKNOWN_PROFILE.preventions)
        );
    }

    private DiseaseDetectionDto toDto(DiseaseDetection d) {
        return DiseaseDetectionDto.builder()
                .id(d.getId())
                .diseaseName(d.getDiseaseName())
                .confidence(d.getConfidence())
                .severity(d.getSeverity())
                .description(d.getDescription())
                .treatments(split(d.getTreatments()))
                .preventions(split(d.getPreventions()))
                .photoUrl(d.getPhotoUrl())
                .detectedAt(d.getDetectedAt())
                .cropId(d.getCrop() != null ? d.getCrop().getId() : null)
                .farmerId(d.getFarmer().getId())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Crop findCrop(Long cropId) {
        return cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + cropId));
    }

    /** Joins a list into a pipe-delimited string for DB storage. */
    private String join(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join("|", list);
    }

    /** Splits a pipe-delimited DB string back into a list. */
    private List<String> split(String value) {
        if (value == null || value.isBlank()) return Collections.emptyList();
        return Arrays.asList(value.split("\\|"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Inner record – disease profile value object
    // ═════════════════════════════════════════════════════════════════════════

    /** Immutable value object holding all data for one disease type. */
    private static final class DiseaseProfile {
        final String       name;
        final String       severity;
        final String       description;
        final List<String> treatments;
        final List<String> preventions;

        DiseaseProfile(String name, String severity, String description,
                        List<String> treatments, List<String> preventions) {
            this.name        = name;
            this.severity    = severity;
            this.description = description;
            this.treatments  = treatments;
            this.preventions = preventions;
        }
    }
}
