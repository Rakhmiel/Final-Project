package com.universitydata.client;

import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.IpedsCompletion;
import com.universitydata.util.HttpUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Client for IPEDS (Integrated Postsecondary Education Data System) data.
 *
 * IPEDS is the federal census of all US higher-education institutions.
 * It does NOT have a REST API — data is distributed as annual CSV/zip downloads.
 *
 * Strategy:
 *   1. Download the Completions survey zip file (C20XX_A.zip) from NCES
 *   2. Cache it locally in config.ipedsLocalCacheDir
 *   3. Parse the CSV and return structured records
 *
 * Completions file (C_A suffix) contains:
 *   - UNITID (institution ID, joins with College Scorecard)
 *   - CIPCODE (6-digit CIP, e.g. "11.0101")
 *   - AWLEVEL (award level: 1=<1yr cert, 2=1-2yr cert, 3=assoc, 5=bach,
 *              7=post-bacc cert, 8=masters, 17=doctoral research, 18=doctoral professional)
 *   - CTOTALT (total completions, all genders/races)
 *
 * Download page: https://nces.ed.gov/ipeds/datacenter/DataFiles.aspx
 * Direct file:   https://nces.ed.gov/ipeds/data/C{YEAR}_A.zip (last 2 digits of year)
 *
 * Join key: UNITID — matches "id" in College Scorecard.
 */
public class IpedsClient {

    private static final Logger log = LoggerFactory.getLogger(IpedsClient.class);

    // Base URL pattern for IPEDS completions data
    // Replace {YY} with e.g. "22" for academic year 2022-23
    private static final String IPEDS_COMPLETIONS_URL =
            "https://nces.ed.gov/ipeds/data/C{YY}_A.zip";

    // Institution name lookup file (HD survey)
    private static final String IPEDS_HD_URL =
            "https://nces.ed.gov/ipeds/data/HD{YYYY}.zip";

    private final DataSourceConfig config;
    private final Path cacheDir;

    public IpedsClient(DataSourceConfig config) {
        this.config   = config;
        this.cacheDir = Paths.get(config.getIpedsLocalCacheDir());
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Download and parse the IPEDS Completions data for a given academic year.
     * Data is cached locally — subsequent calls use the cache.
     *
     * @param academicYear e.g. "2022-23" → downloads C22_A.zip
     * @return all completion records (millions of rows — filter downstream)
     */
    public List<IpedsCompletion> getCompletions(String academicYear) throws IOException {
        String twoDigitYear = extractTwoDigitYear(academicYear);
        Path csvPath = ensureCachedCsv(twoDigitYear, "C" + twoDigitYear + "_A.csv",
                IPEDS_COMPLETIONS_URL.replace("{YY}", twoDigitYear));

        log.info("Parsing IPEDS completions from: {}", csvPath);
        return parseCompletionsCsv(csvPath, academicYear);
    }

    /**
     * Filter completions by CIP code prefix.
     * E.g. cipPrefix="11" returns all Computer Science programs (CIP 11.xxxx).
     */
    public List<IpedsCompletion> getCompletionsByCipPrefix(String academicYear,
                                                            String cipPrefix) throws IOException {
        List<IpedsCompletion> all = getCompletions(academicYear);
        return all.stream()
                .filter(c -> c.cipCode != null && c.cipCode.startsWith(cipPrefix))
                .toList();
    }

    /**
     * Filter completions by award level (degree type).
     * Common values: 3=associate, 5=bachelor, 8=master, 17=doctoral research
     */
    public List<IpedsCompletion> getCompletionsByAwardLevel(String academicYear,
                                                             int awardLevel) throws IOException {
        List<IpedsCompletion> all = getCompletions(academicYear);
        return all.stream()
                .filter(c -> c.awardLevel != null && c.awardLevel == awardLevel)
                .toList();
    }

    /**
     * Get completions for a specific institution.
     */
    public List<IpedsCompletion> getCompletionsByUnitId(String academicYear,
                                                         String unitId) throws IOException {
        List<IpedsCompletion> all = getCompletions(academicYear);
        return all.stream()
                .filter(c -> unitId.equals(c.unitId))
                .toList();
    }

    /**
     * Force re-download, ignoring any local cache.
     */
    public List<IpedsCompletion> refreshCompletions(String academicYear) throws IOException {
        String twoDigitYear = extractTwoDigitYear(academicYear);
        String filename = "C" + twoDigitYear + "_A.csv";
        Path cached = cacheDir.resolve(filename);
        if (Files.exists(cached)) {
            Files.delete(cached);
            log.info("Deleted cached file: {}", cached);
        }
        return getCompletions(academicYear);
    }

    // ---------------------------------------------------------------
    // CSV parsing
    // ---------------------------------------------------------------

    private List<IpedsCompletion> parseCompletionsCsv(Path csvPath, String academicYear)
            throws IOException {

        List<IpedsCompletion> results = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    IpedsCompletion c = new IpedsCompletion();
                    c.unitId       = record.get("UNITID");
                    c.cipCode      = normalizeCip(record.get("CIPCODE"));
                    c.awardLevel   = parseIntOrNull(record.get("AWLEVEL"));
                    c.awardLevelLabel = awardLevelLabel(c.awardLevel);
                    c.totalCompletions = parseIntOrNull(record.get("CTOTALT"));
                    c.academicYear = academicYear;

                    // Institution name is in a separate HD file — populate if available
                    // (requires joining with HD data; left as null here for simplicity)
                    c.institutionName = null;

                    // Skip suppressed rows (CTOTALT = -1 or -2 in IPEDS = suppressed/missing)
                    if (c.totalCompletions != null && c.totalCompletions >= 0) {
                        results.add(c);
                    }
                } catch (Exception e) {
                    log.trace("Skipping malformed CSV row: {}", e.getMessage());
                }
            }
        }

        log.info("Parsed {} IPEDS completion records for {}", results.size(), academicYear);
        return results;
    }

    // ---------------------------------------------------------------
    // Download + cache
    // ---------------------------------------------------------------

    private Path ensureCachedCsv(String yearKey, String csvFilename, String zipUrl) throws IOException {
        Files.createDirectories(cacheDir);
        Path csvPath = cacheDir.resolve(csvFilename);

        if (Files.exists(csvPath)) {
            log.info("Using cached IPEDS file: {}", csvPath);
            return csvPath;
        }

        // Download zip
        Path zipPath = cacheDir.resolve(csvFilename.replace(".csv", ".zip"));
        log.info("Downloading IPEDS data from: {}", zipUrl);

        try (ReadableByteChannel rbc = Channels.newChannel(new URL(zipUrl).openStream());
             FileOutputStream fos = new FileOutputStream(zipPath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        log.info("Downloaded {} ({}KB)", zipPath.getFileName(),
                Files.size(zipPath) / 1024);

        // Unzip and find the CSV
        unzip(zipPath, cacheDir, csvFilename);
        Files.deleteIfExists(zipPath); // clean up zip

        if (!Files.exists(csvPath)) {
            throw new IOException("Expected CSV not found after unzip: " + csvPath);
        }
        return csvPath;
    }

    private void unzip(Path zipFile, Path destDir, String targetFilename) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // IPEDS zips contain multiple files; pick the one we want
                // (case-insensitive match on filename)
                if (!entry.isDirectory() &&
                        name.toUpperCase().endsWith(targetFilename.toUpperCase())) {
                    Path outPath = destDir.resolve(targetFilename);
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        zis.transferTo(os);
                    }
                    log.info("Extracted: {}", outPath);
                }
                zis.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------

    /** Convert "2022-23" → "22" */
    private String extractTwoDigitYear(String academicYear) {
        // Handles formats: "2022-23", "2022", "22"
        if (academicYear.length() == 2) return academicYear;
        if (academicYear.contains("-")) {
            String[] parts = academicYear.split("-");
            return parts[0].substring(2); // "2022" → "22"
        }
        return academicYear.substring(2); // "2022" → "22"
    }

    /** Normalize CIP: "11.0101" or "110101" → "11.0101" */
    private static String normalizeCip(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        if (raw.contains(".")) return raw;
        if (raw.length() == 6) return raw.substring(0, 2) + "." + raw.substring(2);
        return raw;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /** Human-readable label for IPEDS award level codes */
    private static String awardLevelLabel(Integer code) {
        if (code == null) return null;
        return switch (code) {
            case 1  -> "Certificate < 1 year";
            case 2  -> "Certificate 1-2 years";
            case 3  -> "Associate's degree";
            case 4  -> "Certificate > 2 years";
            case 5  -> "Bachelor's degree";
            case 6  -> "Post-baccalaureate certificate";
            case 7  -> "Master's degree";
            case 8  -> "Post-master's certificate";
            case 17 -> "Doctor's degree – research/scholarship";
            case 18 -> "Doctor's degree – professional practice";
            case 19 -> "Doctor's degree – other";
            default -> "Other (code " + code + ")";
        };
    }
}
