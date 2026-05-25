package com.civiccms.controller;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Department;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * AI endpoints used by the CivicBot chatbot.
 *
 *  POST /api/ai/analyze   – classify & enrich a complaint text
 *  POST /api/ai/translate – translate result fields to Tamil / Hindi
 *
 * When anthropic.api.key is blank (dev mode) a smart rule-based mock is used
 * so the chatbot works out-of-the-box without any API key.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ComplaintRepository  complaintRepo;
    private final DepartmentRepository deptRepo;
    private final ObjectMapper         mapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    // Approx 0.5 km radius for duplicate detection
    private static final double DELTA = 0.005;

    // ── Category → Department mapping ─────────────────────────────────
    private static final Map<String, String> CAT_TO_DEPT = Map.ofEntries(
        Map.entry("WATER",        "Water Supply Department"),
        Map.entry("ELECTRICITY",  "Electricity Department"),
        Map.entry("ROAD",         "Roads & Infrastructure Department"),
        Map.entry("GARBAGE",      "Sanitation & Waste Management"),
        Map.entry("DRAINAGE",     "Drainage & Sewage Department"),
        Map.entry("STREETLIGHT",  "Street Lighting Department"),
        Map.entry("PARK",         "Parks & Recreation Department"),
        Map.entry("NOISE",        "Public Health & Safety Department"),
        Map.entry("ENCROACHMENT", "Town Planning Department"),
        Map.entry("ANIMAL",       "Animal Welfare Department"),
        Map.entry("OTHER",        "General Administration")
    );

    // ── Keyword-based category detection ──────────────────────────────
    private static final Map<String, List<String>> CAT_KEYWORDS = Map.ofEntries(
        Map.entry("WATER",        List.of("water","pipe","leak","supply","tap","burst","flood","sewage")),
        Map.entry("ELECTRICITY",  List.of("power","electric","light","outage","voltage","wire","current","transformer","electricity")),
        Map.entry("ROAD",         List.of("road","pothole","traffic","footpath","pavement","bridge","signal","speed","accident")),
        Map.entry("GARBAGE",      List.of("garbage","trash","waste","dustbin","sanit","litter","dump","smell","rubbish")),
        Map.entry("DRAINAGE",     List.of("drain","sewage","sewer","overflow","blockage","gutter","clog","stagnant")),
        Map.entry("STREETLIGHT",  List.of("streetlight","street light","lamp","dark","night","bulb","pole")),
        Map.entry("PARK",         List.of("park","garden","tree","playground","grass","bench","plant","greenery")),
        Map.entry("NOISE",        List.of("noise","sound","loud","music","horn","pollution","speaker","bar")),
        Map.entry("ENCROACHMENT", List.of("encroach","illegal","construct","footpath","block","occupy","building")),
        Map.entry("ANIMAL",       List.of("dog","stray","animal","cow","bite","cattle","monkey","snake"))
    );

    public AiController(ComplaintRepository complaintRepo,
                        DepartmentRepository deptRepo) {
        this.complaintRepo = complaintRepo;
        this.deptRepo      = deptRepo;
    }

    // ── POST /api/ai/analyze ──────────────────────────────────────────
    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody Map<String, Object> body) {
        try {
            String text = str(body, "text", "");
            double lat  = dbl(body, "lat",  11.1271);
            double lng  = dbl(body, "lng",  78.6569);

            Map<String, Object> result;

            if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
                result = analyzeWithClaude(text, lat, lng);
            } else {
                result = analyzeWithMock(text, lat, lng);
            }

            // Duplicate detection (always done server-side)
            String category = str(result, "category", "OTHER");
            boolean isDuplicate = false;
            String  dupMessage  = "";
            try {
                List<Complaint> nearby = complaintRepo.findNearbyOpenComplaints(
                        lat - DELTA, lat + DELTA,
                        lng - DELTA, lng + DELTA, -1L);
                Optional<Complaint> dup = nearby.stream()
                        .filter(c -> c.getCategory() != null &&
                                     c.getCategory().equalsIgnoreCase(category))
                        .findFirst();
                if (dup.isPresent()) {
                    isDuplicate = true;
                    dupMessage  = "A similar " + category.toLowerCase().replace("_", " ") +
                                  " complaint (#" + dup.get().getId() +
                                  ") is already registered nearby.";
                }
            } catch (Exception e) {
                // Ignore duplicate check error — non-critical
            }

            result.put("isDuplicate",      isDuplicate);
            result.put("duplicateMessage", dupMessage);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AI analysis failed: " + e.getMessage()));
        }
    }

    // ── POST /api/ai/translate ────────────────────────────────────────
    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody Map<String, Object> body) {
        try {
            String lang = str(body, "language", "en");
            if ("en".equals(lang)) {
                return ResponseEntity.ok(body); // no-op
            }

            if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
                return ResponseEntity.ok(translateWithClaude(body, lang));
            } else {
                // Dev mode: return as-is (English fallback)
                return ResponseEntity.ok(body);
            }
        } catch (Exception e) {
            return ResponseEntity.ok(body); // Safe fallback — show English
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Claude (Anthropic) integration
    // ══════════════════════════════════════════════════════════════════

    private Map<String, Object> analyzeWithClaude(String text, double lat, double lng)
            throws Exception {

        String deptList = String.join(", ", CAT_TO_DEPT.values());
        String prompt = """
            You are an AI assistant for CivicCMS, an Indian civic complaint management system.
            Analyze the following civic complaint and respond with ONLY a valid JSON object.

            Complaint text: "%s"

            Respond with this exact JSON structure (no markdown, no extra text):
            {
              "title": "<concise 5-8 word title>",
              "category": "<one of: WATER,ELECTRICITY,ROAD,GARBAGE,DRAINAGE,STREETLIGHT,PARK,NOISE,ENCROACHMENT,ANIMAL,OTHER>",
              "urgency": "<one of: LOW,MEDIUM,HIGH,CRITICAL>",
              "urgencyReason": "<one sentence explaining urgency level>",
              "detailedDescription": "<2-3 sentence formal description>",
              "department": "<most appropriate department from: %s>",
              "suggestedAction": "<brief recommended action>"
            }
            """.formatted(text, deptList);

        String requestBody = mapper.writeValueAsString(Map.of(
            "model",      "claude-haiku-4-5-20251001",
            "max_tokens", 800,
            "messages",   List.of(Map.of("role", "user", "content", prompt))
        ));

        String responseText = callAnthropicApi(requestBody);
        JsonNode root = mapper.readTree(responseText);
        String content = root.path("content").get(0).path("text").asText();

        // Strip any accidental markdown fences
        content = content.replaceAll("(?s)```json\\s*|```", "").trim();

        JsonNode json = mapper.readTree(content);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title",              json.path("title").asText("Civic Issue Report"));
        result.put("category",           json.path("category").asText("OTHER").toUpperCase());
        result.put("urgency",            json.path("urgency").asText("MEDIUM").toUpperCase());
        result.put("urgencyReason",      json.path("urgencyReason").asText(""));
        result.put("detailedDescription",json.path("detailedDescription").asText(text));
        result.put("department",         json.path("department").asText("General Administration"));
        result.put("suggestedAction",    json.path("suggestedAction").asText(""));
        return result;
    }

    private Map<String, Object> translateWithClaude(Map<String, Object> fields, String lang)
            throws Exception {

        String langName = "ta".equals(lang) ? "Tamil" : "Hindi";
        String fieldsJson = mapper.writeValueAsString(fields);

        String prompt = """
            Translate the following JSON fields into %s.
            Translate only the string values. Keep keys in English. Keep category/urgency codes as-is.
            Respond with ONLY the translated JSON object, no markdown.

            %s
            """.formatted(langName, fieldsJson);

        String requestBody = mapper.writeValueAsString(Map.of(
            "model",      "claude-haiku-4-5-20251001",
            "max_tokens", 1000,
            "messages",   List.of(Map.of("role", "user", "content", prompt))
        ));

        String responseText = callAnthropicApi(requestBody);
        JsonNode root = mapper.readTree(responseText);
        String content = root.path("content").get(0).path("text").asText();
        content = content.replaceAll("(?s)```json\\s*|```", "").trim();

        JsonNode translated = mapper.readTree(content);
        Map<String, Object> result = new LinkedHashMap<>(fields);
        translated.fields().forEachRemaining(e ->
            result.put(e.getKey(), e.getValue().asText())
        );
        return result;
    }

    private String callAnthropicApi(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type",      "application/json")
            .header("x-api-key",         anthropicApiKey)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API error " + response.statusCode() +
                                       ": " + response.body());
        }
        return response.body();
    }

    // ══════════════════════════════════════════════════════════════════
    // Rule-based mock (no API key required — dev mode)
    // ══════════════════════════════════════════════════════════════════

    private Map<String, Object> analyzeWithMock(String text, double lat, double lng) {
        String lower    = text.toLowerCase();
        String category = detectCategory(lower);
        String urgency  = detectUrgency(lower);
        String dept     = resolveDepartment(category);

        String title = buildTitle(category, lower);
        String desc  = "Citizen has reported a " + category.toLowerCase().replace("_", " ") +
                       " issue: " + text.trim() +
                       ". The matter requires prompt attention from the relevant department.";
        String action = "Assign to " + dept + " for inspection and resolution within SLA.";
        String reason = urgencyReason(urgency, category);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title",               title);
        result.put("category",            category);
        result.put("urgency",             urgency);
        result.put("urgencyReason",       reason);
        result.put("detailedDescription", desc);
        result.put("department",          dept);
        result.put("suggestedAction",     action);
        return result;
    }

    private String detectCategory(String lower) {
        for (Map.Entry<String, List<String>> entry : CAT_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) return entry.getKey();
            }
        }
        return "OTHER";
    }

    private String detectUrgency(String lower) {
        if (lower.contains("fire") || lower.contains("flood") || lower.contains("burst") ||
            lower.contains("electric shock") || lower.contains("accident") ||
            lower.contains("critical") || lower.contains("emergency")) return "CRITICAL";
        if (lower.contains("urgent") || lower.contains("danger") || lower.contains("hazard") ||
            lower.contains("week") || lower.contains("days") || lower.contains("high")) return "HIGH";
        if (lower.contains("since") || lower.contains("month") || lower.contains("old")) return "MEDIUM";
        return "LOW";
    }

    private String urgencyReason(String urgency, String category) {
        return switch (urgency) {
            case "CRITICAL" -> "This issue poses an immediate safety risk and requires emergency response.";
            case "HIGH"     -> "The issue has persisted long enough to significantly impact residents.";
            case "MEDIUM"   -> "Issue affects daily life but is not an immediate emergency.";
            default         -> "Routine maintenance issue — can be scheduled at next available slot.";
        };
    }

    private String resolveDepartment(String category) {
        // Try DB first, fall back to static map
        try {
            List<Department> depts = deptRepo.findAll();
            String catLower = category.toLowerCase().replace("_", " ");
            for (Department d : depts) {
                if (d.getKeywordsCsv() != null) {
                    for (String kw : d.getKeywordsCsv().split(",")) {
                        if (catLower.contains(kw.trim().toLowerCase())) return d.getName();
                    }
                }
            }
        } catch (Exception ignored) {}
        return CAT_TO_DEPT.getOrDefault(category, "General Administration");
    }

    private String buildTitle(String category, String lower) {
        String base = switch (category) {
            case "WATER"        -> "Water Supply Issue Reported";
            case "ELECTRICITY"  -> "Power Outage Complaint";
            case "ROAD"         -> "Road Condition Issue";
            case "GARBAGE"      -> "Garbage Disposal Problem";
            case "DRAINAGE"     -> "Drainage Blockage Reported";
            case "STREETLIGHT"  -> "Street Light Not Working";
            case "PARK"         -> "Park Maintenance Issue";
            case "NOISE"        -> "Noise Pollution Complaint";
            case "ENCROACHMENT" -> "Encroachment Reported";
            case "ANIMAL"       -> "Stray Animal Concern";
            default             -> "Civic Issue Reported";
        };
        return base;
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : def;
    }
    private double dbl(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return def; }
    }
}
