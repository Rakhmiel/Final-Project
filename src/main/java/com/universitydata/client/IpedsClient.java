package com.universitydata.client;

import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.IpedsCompletion;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IpedsClient {

    private static final Logger log = LoggerFactory.getLogger(IpedsClient.class);

    private final DataSourceConfig config;
    private final Path cacheDir;

    public IpedsClient(DataSourceConfig config) {
        this.config   = config;
        this.cacheDir = Paths.get(config.getIpedsLocalCacheDir());
    }

    public List<IpedsCompletion> getCompletions(String academicYear) throws IOException {
        Path csvPath = cacheDir.resolve("C2024_A.csv");
        if (!Files.exists(csvPath)) {
            throw new IOException("IPEDS file not found at: " + csvPath.toAbsolutePath());
        }
        log.info("Parsing IPEDS completions from: {}", csvPath);
        return parseCompletionsCsv(csvPath, academicYear);
    }

    public List<IpedsCompletion> getCompletionsByAwardLevel(String academicYear,
                                                             int awardLevel) throws IOException {
        return getCompletions(academicYear).stream()
                .filter(c -> c.awardLevel != null && c.awardLevel == awardLevel)
                .toList();
    }

    public List<IpedsCompletion> getCompletionsByUnitId(String academicYear,
                                                         String unitId) throws IOException {
        return getCompletions(academicYear).stream()
                .filter(c -> unitId.equals(c.unitId))
                .toList();
    }

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
                    c.unitId           = record.get("UNITID");
                    c.cipCode          = normalizeCip(record.get("CIPCODE"));
                    c.awardLevel       = parseIntOrNull(record.get("AWLEVEL"));
                    c.awardLevelLabel  = awardLevelLabel(c.awardLevel);
                    c.totalCompletions = parseIntOrNull(record.get("CTOTALT"));
                    c.academicYear     = academicYear;
                    c.institutionName  = null;

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
