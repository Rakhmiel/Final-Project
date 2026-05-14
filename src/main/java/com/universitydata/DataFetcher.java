package com.universitydata;

import com.google.gson.Gson;
import com.universitydata.client.UniversityDataService;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import com.universitydata.util.HttpUtil;

import java.io.*;
import java.util.*;

/**
 * Reads universitydata.json, fetches data from College Scorecard, OEWS flat
 * file, O*NET, and BLS OOH, then writes a data.json file that comparisonProgram
 * can load directly.
 *
 * Usage:
 *   java -jar target/my-project-1.0-SNAPSHOT.jar [universitydata.json]
 */
public class DataFetcher {

    public static void main(String[] args) throws Exception {
        String inputFile = args.length > 0 ? args[0] : "universitydata.json";
        System.out.println("Reading config: " + inputFile);

        FetchConfig cfg;
        try (Reader r = new FileReader(inputFile)) {
            cfg = new Gson().fromJson(r, FetchConfig.class);
        }

        if (cfg.collegeScorecardApiKey == null || cfg.collegeScorecardApiKey.isBlank()) {
            System.err.println("Error: collegeScorecardApiKey is required in " + inputFile);
            System.exit(1);
        }

        DataSourceConfig config = DataSourceConfig.builder()
                .collegeScorecardApiKey(cfg.collegeScorecardApiKey)
                .onetApiKey(cfg.onetApiKey != null ? cfg.onetApiKey : "")
                .ipedsLocalCacheDir(cfg.oewsDataPath != null ? cfg.oewsDataPath : ".")
                .build();

        UniversityDataService svc = new UniversityDataService(config);

        List<String> cipCodes  = cfg.cipCodes  != null ? cfg.cipCodes  : List.of();
        List<String> schools   = cfg.schools   != null ? cfg.schools   : List.of();
        String       outputFile = cfg.outputFile != null ? cfg.outputFile : "data.json";

        // 1. Fetch majors from Scorecard + O*NET crosswalk
        List<MajorOutcomeSummary> majors = new ArrayList<>();
        for (String cip : cipCodes) {
            System.out.println("Fetching major: " + cip);
            MajorOutcomeSummary m = new MajorOutcomeSummary();
            m.cipCode = cip;

            List<ScorecardProgram> programs = svc.getProgramsByCip(
                    cip.replace(".", "").substring(0, 4));
            programs.stream()
                    .filter(p -> p.credentialLevel != null && p.credentialLevel == 3)
                    .filter(p -> p.medianEarnings4Yr != null)
                    .filter(p -> schools.isEmpty() || (p.schoolName != null &&
                            schools.stream().anyMatch(
                                    s -> p.schoolName.toLowerCase().contains(s.toLowerCase()))))
                    .findFirst()
                    .ifPresent(p -> {
                        m.cipTitle                   = p.cipTitle;
                        m.credentialLevel            = p.credentialLevel;
                        m.scorecardMedianEarnings1Yr = p.medianEarnings1Yr;
                        m.scorecardMedianEarnings4Yr = p.medianEarnings4Yr;
                    });

            List<OnetCrosswalkEntry> crosswalk = svc.getOccupationsForMajor(cip);
            m.relatedSocCodes = crosswalk.stream()
                    .map(e -> e.socCode).filter(s -> s != null).toList();
            m.relatedOccupationTitles = crosswalk.stream()
                    .map(e -> e.socTitle).filter(s -> s != null).toList();

            majors.add(m);
            HttpUtil.politeDelay(500);
        }

        // 2. IPEDS completions
        System.out.println("Loading IPEDS completions...");
        try {
            List<IpedsCompletion> completions = svc.getIpedsClient().getCompletions("2024");
            System.out.println("IPEDS completions loaded: " + completions.size());
            for (MajorOutcomeSummary m : majors) {
                String cipPrefix = m.cipCode != null ? m.cipCode.substring(0, 2) : "";
                m.annualCompletions = completions.stream()
                        .filter(c -> c.cipCode != null && c.cipCode.startsWith(cipPrefix))
                        .filter(c -> c.awardLevel != null && c.awardLevel == 5)
                        .mapToInt(c -> c.totalCompletions != null ? c.totalCompletions : 0)
                        .sum();
                System.out.println("  Completions for " + m.cipCode + ": " + m.annualCompletions);
            }
        } catch (Exception e) {
            System.out.println("IPEDS failed: " + e.getMessage());
        }

        // 3. OEWS flat file
        Map<String, OewsOccupation> oewsMap = new HashMap<>();
        String oewsPath = cfg.oewsDataPath != null ? cfg.oewsDataPath : ".";
        try {
            oewsMap = OewsParser.parse(oewsPath);
        } catch (Exception e) {
            System.out.println("OEWS parse failed (continuing without wage percentiles): "
                    + e.getMessage());
        }
        System.out.println("OEWS map size: " + oewsMap.size());
        final Map<String, OewsOccupation> finalOewsMap = oewsMap;

        // 4. Unique SOC codes across all majors
        List<String> allSocs = majors.stream()
                .filter(m -> m.relatedSocCodes != null)
                .flatMap(m -> m.relatedSocCodes.stream())
                .distinct().toList();

        // 5. Full crosswalk (for bright-outlook tags)
        List<OnetCrosswalkEntry> allCrosswalk = new ArrayList<>();
        for (MajorOutcomeSummary m : majors) {
            try {
                allCrosswalk.addAll(svc.getOccupationsForMajor(m.cipCode));
                HttpUtil.politeDelay(500);
            } catch (Exception e) {
                System.out.println("Crosswalk fetch failed for " + m.cipCode
                        + ": " + e.getMessage());
            }
        }

        // 6. OOH — one page per unique occupation title keyword
        List<OohOccupation> oohList = new ArrayList<>();
        List<String> oohUrls = svc.getOohClient().getAllOccupationUrls();
        List<String> fetchedKeywords = new ArrayList<>();
        for (String title : majors.stream()
                .filter(m -> m.relatedOccupationTitles != null)
                .flatMap(m -> m.relatedOccupationTitles.stream())
                .distinct().toList()) {
            String keyword = title.toLowerCase().split(" ")[0];
            if (fetchedKeywords.contains(keyword)) continue;
            fetchedKeywords.add(keyword);
            oohUrls.stream()
                    .filter(u -> u.toLowerCase().contains(keyword))
                    .findFirst()
                    .ifPresent(url -> {
                        try {
                            oohList.add(svc.getOohClient().scrapeOccupationPage(url));
                            HttpUtil.politeDelay(1500);
                        } catch (IOException e) {
                            System.out.println("OOH fetch failed for '" + title
                                    + "': " + e.getMessage());
                        }
                    });
        }

        // 7. Bright Outlook map from O*NET tags
        Map<String, OnetCrosswalkEntry> brightOutlookMap = new HashMap<>();
        allCrosswalk.stream()
                .filter(e -> e.tags != null && e.tags.contains("Bright Outlook"))
                .forEach(e -> brightOutlookMap.put(e.socCode, e));

        // 8. Build occupation list (OEWS data merged with crosswalk titles)
        List<OewsOccupation> occupations = allSocs.stream().map(soc -> {
            OewsOccupation o = finalOewsMap.getOrDefault(soc, new OewsOccupation());
            o.socCode = soc;
            allCrosswalk.stream()
                    .filter(e -> soc.equals(e.socCode))
                    .findFirst()
                    .ifPresent(e -> o.title = e.socTitle);
            return o;
        }).toList();

        // 9. Export to data.json (compatible with comparisonProgram)
        new DataExporter().export(majors, occupations, oohList, brightOutlookMap, outputFile);
    }

    // ── Config POJO ───────────────────────────────────────────────────────────

    static class FetchConfig {
        String       collegeScorecardApiKey;
        String       onetApiKey;
        /** Path to directory containing oe.data.part.* OEWS flat-file chunks. */
        String       oewsDataPath = ".";
        /** Output path for the generated data file. Default: data.json */
        String       outputFile   = "data.json";
        /** CIP codes to fetch, e.g. ["11.0101", "52.0201"] */
        List<String> cipCodes     = new ArrayList<>();
        /**
         * School name fragments to filter Scorecard results.
         * If empty, the first bachelor's program found for each CIP is used.
         */
        List<String> schools      = new ArrayList<>();
    }
}
