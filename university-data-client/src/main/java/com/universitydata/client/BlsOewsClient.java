package com.universitydata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.OewsOccupation;
import com.universitydata.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for BLS Occupational Employment and Wage Statistics (OEWS).
 *
 * Two access strategies are provided:
 *
 *   1. BLS Public Data API (JSON) — query specific SOC series by ID.
 *      Docs: https://www.bls.gov/developers/api_signature_v2.htm
 *      Key:  https://data.bls.gov/registrationEngine/ (free; unauthenticated = 25 req/day)
 *
 *   2. Bulk flat-file download — parse the annual national OE.data.0.Current file
 *      directly from the BLS download server. No key required, covers all ~830 occupations.
 *      URL: https://download.bls.gov/pub/time.series/oe/oe.data.0.Current
 *
 * The flat-file approach is recommended for bulk lookups (building a local cache).
 * The API approach is better for targeted real-time queries.
 *
 * SOC codes: https://www.bls.gov/soc/
 * OEWS series ID format: OE + area + industry + occupation + datatype
 *   e.g. OEU000000000000015125200008 = national, all industries, SOC 15-1252, annual mean wage
 */
public class BlsOewsClient {

    private static final Logger log = LoggerFactory.getLogger(BlsOewsClient.class);

    // BLS Public Data API v2 endpoint
    private static final String BLS_SERIES_URL = "/timeseries/data/";

    // Bulk download base
    private static final String BLS_BULK_BASE  = "https://download.bls.gov/pub/time.series/oe/";

    // Data type codes used in series IDs
    // 01=employment, 04=hourly mean, 08=annual mean, 09=wage%rse,
    // 11=10th pct annual, 12=25th pct annual, 13=50th pct annual (median),
    // 14=75th pct annual, 15=90th pct annual
    private static final String[] WAGE_DATA_TYPES = {"04","08","11","12","13","14","15"};

    private final DataSourceConfig config;
    private final HttpUtil http;
    private final ObjectMapper mapper;

    public BlsOewsClient(DataSourceConfig config) {
        this.config = config;
        this.http   = new HttpUtil(config.getConnectTimeoutSeconds(),
                                   config.getReadTimeoutSeconds(),
                                   config.getMaxRetries());
        this.mapper = new ObjectMapper();
    }

    // ---------------------------------------------------------------
    // Strategy 1: BLS Public Data API (targeted queries)
    // ---------------------------------------------------------------

    /**
     * Fetch national wage data for a given SOC code via the BLS series API.
     * Makes one API call per data type (employment, wages, percentiles).
     *
     * @param socCode 6-digit SOC code, e.g. "151252" or "15-1252"
     * @return OewsOccupation populated with all available wage fields
     */
    public OewsOccupation getOccupationBySOC(String socCode) throws IOException {
        String cleanSoc = socCode.replace("-", "");  // ensure no hyphens
        OewsOccupation occ = new OewsOccupation();
        occ.socCode = socCode;
        occ.area    = "National";

        // Build series IDs for each data type we want
        // Series format: OE + U (national) + 0000000000 (all industries) + SOC + datatype
        List<String> seriesIds = new ArrayList<>();
        seriesIds.add(buildSeriesId(cleanSoc, "01")); // employment
        for (String dtype : WAGE_DATA_TYPES) {
            seriesIds.add(buildSeriesId(cleanSoc, dtype));
        }

        String requestBody = buildApiRequestBody(seriesIds);
        String json = postToBlsApi(requestBody);
        JsonNode root = mapper.readTree(json);

        if (!"REQUEST_SUCCEEDED".equals(root.path("status").asText())) {
            log.warn("BLS API returned non-success status for SOC {}: {}",
                    socCode, root.path("message"));
            return occ;
        }

        for (JsonNode series : root.path("Results").path("series")) {
            String sid    = series.path("seriesID").asText();
            String dtype  = sid.substring(sid.length() - 2); // last 2 chars = data type
            JsonNode data = series.path("data");

            // Take the most recent value
            if (data.isArray() && data.size() > 0) {
                double value = data.get(0).path("value").asDouble(0);

                switch (dtype) {
                    case "01" -> occ.totalEmployed   = (long) value;
                    case "04" -> occ.hourlyMeanWage  = value;
                    case "08" -> occ.annualMeanWage  = value;
                    case "11" -> occ.annualWage10thPct = value;
                    case "12" -> occ.annualWage25thPct = value;
                    case "13" -> occ.annualMedianWage  = value;
                    case "14" -> occ.annualWage75thPct = value;
                    case "15" -> occ.annualWage90thPct = value;
                }
            }
        }

        // Try to extract title from OOH (not in OEWS API directly)
        log.debug("Fetched OEWS data for SOC {}: median=${}", socCode, occ.annualMedianWage);
        return occ;
    }

    /**
     * Fetch wage data for multiple SOC codes in a single API call (up to 50 series per call).
     */
    public List<OewsOccupation> getOccupationsBySOCs(List<String> socCodes) throws IOException {
        List<OewsOccupation> results = new ArrayList<>();

        // Build series IDs for annual median wage (code 13) for each SOC
        List<String> seriesIds = new ArrayList<>();
        for (String soc : socCodes) {
            String clean = soc.replace("-", "");
            seriesIds.add(buildSeriesId(clean, "08")); // annual mean
            seriesIds.add(buildSeriesId(clean, "13")); // annual median
            seriesIds.add(buildSeriesId(clean, "01")); // employment
        }

        // BLS API allows max 50 series per request
        for (int i = 0; i < seriesIds.size(); i += 50) {
            List<String> batch = seriesIds.subList(i, Math.min(i + 50, seriesIds.size()));
            String json = postToBlsApi(buildApiRequestBody(batch));
            JsonNode root = mapper.readTree(json);
            parseMultiSeriesResponse(root, socCodes, results);
        }

        return results;
    }

    // ---------------------------------------------------------------
    // Strategy 2: Bulk flat-file download
    // ---------------------------------------------------------------

    /**
     * Download and parse the BLS OEWS national flat file.
     * Returns ALL ~830 occupations with their wage data.
     *
     * The file is a tab-delimited text file updated annually.
     * Cache locally and re-use — it's ~5MB.
     *
     * File format: series_id | year | period | value | footnote_codes
     */
    public List<OewsOccupation> downloadAllNationalOccupations() throws IOException {
        // The national cross-industry data file
        String dataUrl   = BLS_BULK_BASE + "oe.data.1.AllData";
        String seriesUrl = BLS_BULK_BASE + "oe.series";

        log.info("Downloading BLS OEWS bulk data files (this may take a moment)...");
        String seriesData = http.get(seriesUrl);
        String occData    = http.get(dataUrl);

        return parseBulkFiles(seriesData, occData);
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    /**
     * Build a BLS OEWS series ID.
     *
     * Format: OE [survey] + U [seasonal adj] + 0000000000 [area: national] +
     *         000000 [industry: all] + [8-char SOC padded] + [2-char data type]
     *
     * Full series ID spec: https://www.bls.gov/help/hlpforma.htm#OE
     */
    private String buildSeriesId(String socCode6, String dataType) {
        // National, not seasonally adjusted, all industries
        // SOC is 8 chars with trailing 00 (detail level)
        String socPadded = socCode6 + "00"; // e.g. 15125200
        return "OEU000000000000" + socPadded + dataType;
    }

    private String buildApiRequestBody(List<String> seriesIds) {
        StringBuilder sb = new StringBuilder("{\"seriesid\":[");
        for (int i = 0; i < seriesIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(seriesIds.get(i)).append("\"");
        }
        sb.append("]");
        if (!config.getBlsApiKey().isBlank()) {
            sb.append(",\"registrationkey\":\"").append(config.getBlsApiKey()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String postToBlsApi(String jsonBody) throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                jsonBody, okhttp3.MediaType.parse("application/json"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(config.getBlsBaseUrl() + BLS_SERIES_URL)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("BLS API HTTP " + response.code());
            }
            okhttp3.ResponseBody rb = response.body();
            return rb != null ? rb.string() : "{}";
        }
    }

    private void parseMultiSeriesResponse(JsonNode root, List<String> requestedSocs,
                                          List<OewsOccupation> results) {
        // Build a map from series ID suffix (SOC + type) to value
        for (JsonNode series : root.path("Results").path("series")) {
            String sid = series.path("seriesID").asText();
            if (sid.length() < 10) continue;

            // Extract SOC from series ID (chars 15..22 = 8-char SOC)
            String socRaw   = sid.substring(15, 23); // 8 chars
            String socClean = socRaw.substring(0, 2) + "-" + socRaw.substring(2, 6);
            String dtype    = sid.substring(sid.length() - 2);

            JsonNode data = series.path("data");
            if (!data.isArray() || data.size() == 0) continue;
            double value = data.get(0).path("value").asDouble(0);

            // Find or create
            OewsOccupation occ = results.stream()
                    .filter(o -> socClean.equals(o.socCode))
                    .findFirst()
                    .orElseGet(() -> {
                        OewsOccupation o = new OewsOccupation();
                        o.socCode = socClean;
                        o.area = "National";
                        results.add(o);
                        return o;
                    });

            switch (dtype) {
                case "01" -> occ.totalEmployed    = (long) value;
                case "08" -> occ.annualMeanWage   = value;
                case "13" -> occ.annualMedianWage = value;
            }
        }
    }

    private List<OewsOccupation> parseBulkFiles(String seriesData, String occData) {
        // TODO: Parse tab-delimited flat files
        // Series file columns: series_id, supersector_code, industry_code,
        //   area_code, datatype_code, occupation_code, footnote_codes, begin_year, end_year
        // Data file columns: series_id, year, period, value, footnote_codes
        //
        // This is a larger parsing task — recommend downloading once,
        // caching locally, and loading from disk on subsequent runs.
        // See: https://download.bls.gov/pub/time.series/oe/oe.txt for full spec
        log.warn("parseBulkFiles: Full flat-file parser not yet implemented. " +
                "See BLS OEWS flat-file spec at https://download.bls.gov/pub/time.series/oe/oe.txt");
        return new ArrayList<>();
    }
}
