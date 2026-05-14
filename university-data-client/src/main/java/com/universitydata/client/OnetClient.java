package com.universitydata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import com.universitydata.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for O*NET Web Services API.
 *
 * O*NET is the primary source for:
 *   1. CIP → SOC crosswalk (which occupations does a major lead to?)
 *   2. Occupation details (description, bright outlook, sample job titles)
 *   3. Education/experience requirements per occupation
 *
 * API registration (free): https://services.onetcenter.org/developer/
 * Docs: https://services.onetcenter.org/reference/
 *
 * Authentication: HTTP Basic Auth (username = your registered email, password = API password)
 * Rate limit: not publicly stated; be polite (delay between requests).
 *
 * Key endpoints used:
 *   GET /ws/crosswalks/education/programs/{cipCode}  → occupations for a major
 *   GET /ws/occupations/{socCode}                    → occupation details
 *   GET /ws/occupations?keyword={term}               → search occupations
 *   GET /ws/occupations/{socCode}/summary            → quick summary
 */
public class OnetClient {

    private static final Logger log = LoggerFactory.getLogger(OnetClient.class);

    private final DataSourceConfig config;
    private final HttpUtil http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public OnetClient(DataSourceConfig config) {
        this.config    = config;
        this.http      = new HttpUtil(config.getConnectTimeoutSeconds(),
                                      config.getReadTimeoutSeconds(),
                                      config.getMaxRetries());
        this.mapper    = new ObjectMapper();
	this.apiKey = config.getOnetApiKey();
    }

    // ---------------------------------------------------------------
    // CIP → SOC Crosswalk (most important feature)
    // ---------------------------------------------------------------

    /**
     * Get the list of occupations associated with a CIP code (major).
     * This is the key bridge between "what did someone study" and "what job do they have".
     *
     * @param cipCode 2-digit or dotted CIP, e.g. "11.0101" or "1101"
     * @return list of SOC-mapped occupations for that CIP
     */
    public List<OnetCrosswalkEntry> getOccupationsForCip(String cipCode) throws IOException {
        String cleanCip = normalizeCipForOnet(cipCode);
        String url = config.getOnetBaseUrl() + "/online/crosswalks/education?keyword=" + cleanCip;
        log.debug("O*NET CIP crosswalk for {}: {}", cipCode, url);
        String json = http.getWithApiKey(url, apiKey);
        return parseCrosswalkResponse(json, cipCode);
    }

    /**
     * Get occupations for a 2-digit CIP family (broad major area).
     * E.g. "11" = all Computer Science programs.
     *
     * NOTE: O*NET crosswalk requires the full CIP code. For broad searches,
     * call getOccupationsForCip for each sub-code or use keyword search.
     */
    public List<OnetCrosswalkEntry> searchOccupationsByKeyword(String keyword) throws IOException {
        String url = config.getOnetBaseUrl() + "/occupations?keyword="
                + keyword.replace(" ", "%20") + "&format=json";

        log.debug("O*NET keyword search '{}': {}", keyword, url);
        String json = http.getWithApiKey(url, apiKey);
        return parseOccupationSearchResponse(json);
    }

    // ---------------------------------------------------------------
    // Occupation Details
    // ---------------------------------------------------------------

    /**
     * Get detailed information about a specific O*NET occupation.
     *
     * @param socCode 8-digit O*NET-SOC code (e.g. "15-1252.00") or 6-digit SOC
     */
    public OnetOccupation getOccupationDetail(String socCode) throws IOException {
        String code = normalizeOnetCode(socCode);
        String url  = config.getOnetBaseUrl() + "/occupations/" + code + "?format=json";

        log.debug("O*NET occupation detail for {}", code);
        String json = http.getWithApiKey(url, apiKey);
        return parseOccupationDetail(json, socCode);
    }

    /**
     * Get summary for an O*NET occupation (lighter-weight than full detail).
     */
    public OnetOccupation getOccupationSummary(String socCode) throws IOException {
        String code = normalizeOnetCode(socCode);
        String url  = config.getOnetBaseUrl() + "/occupations/" + code + "/summary?format=json";

        String json = http.getWithApiKey(url, apiKey);
        return parseOccupationDetail(json, socCode);
    }

    /**
     * Convenience: given a CIP code, return full occupation details for all related SOCs.
     * Makes N+1 requests (one crosswalk + one per occupation), so use sparingly.
     */
    public List<OnetOccupation> getFullOccupationsForCip(String cipCode) throws IOException {
        List<OnetCrosswalkEntry> crosswalk = getOccupationsForCip(cipCode);
        List<OnetOccupation> results = new ArrayList<>();

        for (OnetCrosswalkEntry entry : crosswalk) {
            HttpUtil.politeDelay(300); // be polite
            try {
                OnetOccupation occ = getOccupationDetail(entry.socCode);
                results.add(occ);
            } catch (IOException e) {
                log.warn("Failed to fetch O*NET detail for SOC {}: {}", entry.socCode, e.getMessage());
            }
        }
        return results;
    }

    /**
     * Get all Bright Outlook occupations (fast-growing, high openings, or new/emerging).
     */
    public List<OnetOccupation> getBrightOutlookOccupations() throws IOException {
        String url = config.getOnetBaseUrl() + "/occupations?category=bright_outlook&format=json";
        String json = http.getWithApiKey(url, apiKey);
        return parseBrightOutlookList(json);
    }

    // ---------------------------------------------------------------
    // Parsing helpers
    // ---------------------------------------------------------------

    private List<OnetCrosswalkEntry> parseCrosswalkResponse(String json, String cipCode) {
        List<OnetCrosswalkEntry> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);

            JsonNode matches = root.path("match");
        if (matches.isArray()) {
            for (JsonNode match : matches) {
                String cipTitle = match.path("title").asText(null);
                JsonNode occs = match.path("occupation");
                if (occs.isArray()) {
                    for (JsonNode occ : occs) {
                        OnetCrosswalkEntry e = new OnetCrosswalkEntry();
                        e.cipCode  = cipCode;
                        e.cipTitle = cipTitle;
                        e.socCode  = occ.path("code").asText(null);
                        e.socTitle = occ.path("title").asText(null);
                        e.tags     = parseTags(occ.path("tags"));
                        results.add(e);
                    }
                }
            }
        }
        } catch (Exception ex) {
            log.warn("Failed to parse O*NET crosswalk response: {}", ex.getMessage());
        }
        return results;
    }

    private List<OnetCrosswalkEntry> parseOccupationSearchResponse(String json) {
        List<OnetCrosswalkEntry> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode occs = root.path("occupation");
            if (occs.isArray()) {
                for (JsonNode occ : occs) {
                    OnetCrosswalkEntry e = new OnetCrosswalkEntry();
                    e.socCode  = occ.path("code").asText(null);
                    e.socTitle = occ.path("title").asText(null);
                    e.tags     = parseTags(occ.path("tags"));
                    results.add(e);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse O*NET search response: {}", ex.getMessage());
        }
        return results;
    }

    private OnetOccupation parseOccupationDetail(String json, String socCode) {
        OnetOccupation occ = new OnetOccupation();
        occ.code = socCode;
        try {
            JsonNode root = mapper.readTree(json);

            occ.code        = root.path("code").asText(socCode);
            occ.title       = root.path("title").asText(null);
            occ.description = root.path("description").asText(null);

            JsonNode tags = root.path("tags");
            occ.brightOutlook = tags.path("bright_outlook").asBoolean(false);
            occ.green         = tags.path("green").asBoolean(false);

            // Sample job titles (if present in response)
            JsonNode sample = root.path("sample_of_reported_job_titles").path("title");
            List<String> titles = new ArrayList<>();
            if (sample.isArray()) {
                for (JsonNode t : sample) {
                    titles.add(t.asText());
                }
            }
            occ.sampleJobTitles = titles;

        } catch (Exception ex) {
            log.warn("Failed to parse O*NET occupation detail for {}: {}", socCode, ex.getMessage());
        }
        return occ;
    }

    private List<OnetOccupation> parseBrightOutlookList(String json) {
        List<OnetOccupation> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode occs = root.path("occupation");
            if (occs.isArray()) {
                for (JsonNode node : occs) {
                    OnetOccupation occ = new OnetOccupation();
                    occ.code         = node.path("code").asText(null);
                    occ.title        = node.path("title").asText(null);
                    occ.brightOutlook = true;
		    occ.sampleJobTitles = new java.util.ArrayList<>();
                    results.add(occ);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse Bright Outlook list: {}", ex.getMessage());
        }
        return results;
    }

    private List<String> parseTags(JsonNode tagsNode) {
        List<String> tags = new ArrayList<>();
        if (tagsNode == null || tagsNode.isNull()) return tags;
        if (tagsNode.path("bright_outlook").asBoolean(false)) tags.add("Bright Outlook");
        if (tagsNode.path("green").asBoolean(false))          tags.add("Green");
        if (tagsNode.path("apprenticeship").asBoolean(false)) tags.add("Apprenticeship");
        return tags;
    }

    // Normalize CIP for O*NET: "1101" → "11.01", "110101" → "11.0101"
    private static String normalizeCipForOnet(String cip) {
        String clean = cip.replace(".", "").trim();
        if (clean.length() == 4) return clean.substring(0, 2) + "." + clean.substring(2);
        if (clean.length() == 6) return clean.substring(0, 2) + "." + clean.substring(2);
        return cip;
    }

    // Normalize SOC code: "151252" or "15-1252" → "15-1252.00"
    private static String normalizeOnetCode(String soc) {
        if (soc.contains(".")) return soc; // already has sub-code
        String clean = soc.replace("-", "");
        if (clean.length() == 6) {
            return clean.substring(0, 2) + "-" + clean.substring(2) + ".00";
        }
        return soc;
    }
}
