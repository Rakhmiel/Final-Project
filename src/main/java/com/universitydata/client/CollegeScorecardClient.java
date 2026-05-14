package com.universitydata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import com.universitydata.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the U.S. Department of Education College Scorecard API.
 *
 * Documentation: https://collegescorecard.ed.gov/data/api-documentation/
 * Key signup:    https://api.data.gov/signup/
 *
 * Rate limit: 1,000 requests/hour per API key.
 *
 * Key concepts:
 *   - /schools endpoint — institution-level data
 *   - fields-of-study data is nested as programs.cip_4_digit[] inside school results
 *   - CIP codes identify majors (e.g. 11.01 = Computer and Information Sciences)
 *   - Earnings data comes from IRS tax records matched to student loan data
 */
public class CollegeScorecardClient {

    private static final Logger log = LoggerFactory.getLogger(CollegeScorecardClient.class);

    private static final String SCHOOLS_PATH = "/schools";

    /**
     * Fields requested from Scorecard. Adjust to add/remove data points.
     * Full field list: https://collegescorecard.ed.gov/data/api-documentation/#schema
     */
    private static final String PROGRAM_FIELDS = String.join(",",
            "id",
            "school.name",
            "latest.programs.cip_4_digit.code",
            "latest.programs.cip_4_digit.title",
            "latest.programs.cip_4_digit.credential.level",
            "latest.programs.cip_4_digit.earnings.highest.2_yr.overall_median_earnings",
            "latest.programs.cip_4_digit.earnings.highest.4_yr.overall_median_earnings",
            "latest.programs.cip_4_digit.debt.median_debt"
    );

    private final DataSourceConfig config;
    private final HttpUtil http;
    private final ObjectMapper mapper;

    public CollegeScorecardClient(DataSourceConfig config) {
        this.config = config;
        this.http   = new HttpUtil(config.getConnectTimeoutSeconds(),
                                   config.getReadTimeoutSeconds(),
                                   config.getMaxRetries());
        this.mapper = new ObjectMapper();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Fetch all field-of-study records for a given CIP 4-digit code across all schools.
     * Automatically paginates through all result pages.
     *
     * @param cip4Code e.g. "1101" for Computer Science (drop the dot)
     */
    public List<ScorecardProgram> getProgramsByCip(String cip4Code) throws IOException {
        String filter = "latest.programs.cip_4_digit.code=" + cip4Code;
        return fetchAllPrograms(filter);
    }

    /**
     * Fetch field-of-study records for a specific school (by name fragment).
     */
    public List<ScorecardProgram> getProgramsBySchool(String schoolName) throws IOException {
        String filter = "school.name=" + URLEncoder.encode(schoolName, StandardCharsets.UTF_8);
        return fetchAllPrograms(filter);
    }

    /**
     * Fetch programs by credential level.
     * Levels: 1=certificate <1yr, 2=certificate 1-2yr, 3=associate, 4=bachelor,
     *         5=post-bacc, 6=master, 7=doctoral research, 8=doctoral professional, 17=graduate
     */
    public List<ScorecardProgram> getProgramsByCredentialLevel(int level) throws IOException {
        String filter = "latest.programs.cip_4_digit.credential.level=" + level;
        return fetchAllPrograms(filter);
    }

    /**
     * Fetch programs by school state (2-letter code, e.g. "NY").
     */
    public List<ScorecardProgram> getProgramsByState(String stateCode) throws IOException {
        String filter = "school.state=" + stateCode;
        return fetchAllPrograms(filter);
    }

    /**
     * Fetch the full institution list with basic info (no program nesting).
     * Useful for building a school→UNITID lookup table.
     */
    public List<SchoolInfo> getAllSchools() throws IOException {
        List<SchoolInfo> results = new ArrayList<>();
        int page = 0;
        int perPage = config.getCollegeScorecardPageSize();

        while (true) {
            String url = buildUrl(
                    SCHOOLS_PATH,
                    "fields=id,school.name,school.state,school.city,school.school_url" +
                    "&school.currently_operating=1" +
                    "&page=" + page + "&per_page=" + perPage
            );
            log.debug("Fetching schools page {}: {}", page, url);
            String json = http.get(url);
            JsonNode root = mapper.readTree(json);
            JsonNode data = root.path("results");

            if (!data.isArray() || data.size() == 0) break;

            for (JsonNode node : data) {
                SchoolInfo s = new SchoolInfo();
                s.unitId     = node.path("id").asText(null);
                s.name       = node.path("school.name").asText(null);
                s.state      = node.path("school.state").asText(null);
                s.city       = node.path("school.city").asText(null);
                s.websiteUrl = node.path("school.school_url").asText(null);
                results.add(s);
            }

            int total = root.path("metadata").path("total").asInt(0);
            if ((long)(page + 1) * perPage >= total) break;
            page++;
        }

        log.info("Fetched {} schools from College Scorecard", results.size());
        return results;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private List<ScorecardProgram> fetchAllPrograms(String filter) throws IOException {
    List<ScorecardProgram> all = new ArrayList<>();
    int page = 0;
    int perPage = config.getCollegeScorecardPageSize();

    while (true) {
        String url = buildUrl(
                SCHOOLS_PATH,
                filter + "&fields=latest.programs.cip_4_digit" +
                "&page=" + page + "&per_page=" + perPage
        );
        log.debug("Scorecard request (page {}): {}", page, url);
        String json = http.get(url);
        JsonNode root = mapper.readTree(json);
        JsonNode results = root.path("results");

        if (!results.isArray() || results.size() == 0) break;

        for (JsonNode schoolNode : results) {
            JsonNode programs = schoolNode.path("latest.programs.cip_4_digit");
            if (programs.isArray()) {
                for (JsonNode prog : programs) {
                    all.add(parseProgram(prog));
                }
            }
        }

        int total = root.path("metadata").path("total").asInt(0);
        if ((long)(page + 1) * perPage >= total) break;
        page++;
    }

    log.info("Fetched {} program records from College Scorecard (filter={})", all.size(), filter);
    return all;
}

    private ScorecardProgram parseProgram(JsonNode prog) {
    ScorecardProgram p = new ScorecardProgram();
    p.unitId          = prog.path("unit_id").asText(null);
    p.schoolName      = prog.path("school").path("name").asText(null);
    p.cipCode         = prog.path("code").asText(null);
    p.cipTitle        = prog.path("title").asText(null);
    p.credentialLevel = nodeIntOrNull(prog.path("credential").path("level"));
    p.medianEarnings1Yr = nodeIntOrNull(prog.path("earnings").path("1_yr").path("overall_median_earnings"));
    p.medianEarnings4Yr = nodeIntOrNull(prog.path("earnings").path("4_yr").path("overall_median_earnings_national"));
    p.medianDebt      = null; // not available at program level
    return p;
}

    private String buildUrl(String path, String query) {
        return config.getCollegeScorecardBaseUrl() + path
                + "?api_key=" + config.getCollegeScorecardApiKey()
                + "&" + query;
    }

    private static Integer nodeIntOrNull(JsonNode node) {
        return (!node.isNull() && node.isNumber()) ? node.asInt() : null;
    }

    // ---------------------------------------------------------------
    // Supplementary model — school info without program nesting
    // ---------------------------------------------------------------

    public static class SchoolInfo {
        public String unitId;
        public String name;
        public String state;
        public String city;
        public String websiteUrl;

        @Override
        public String toString() {
            return String.format("School{unitId='%s', name='%s', state='%s', city='%s'}",
                    unitId, name, state, city);
        }
    }
}
