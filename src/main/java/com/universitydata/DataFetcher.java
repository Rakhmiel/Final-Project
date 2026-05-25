package com.universitydata;

import com.universitydata.client.UniversityDataService;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import com.universitydata.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFetcher {

    public static void main(String[] args) throws Exception {
        DataSourceConfig config = DataSourceConfig.builder()
                .collegeScorecardApiKey("UErh64HhHezs7idMZ9uYbhqgDAQif6cpgjcUISvz")
                .onetApiKey("RlSNc-wN5Tq-UZIHq-bMVDn")
                .ipedsLocalCacheDir(".")
                .build();

        UniversityDataService svc = new UniversityDataService(config);

        List<String> cipCodes = List.of("11.0101", "52.0201");

        List<String> schools = List.of(
                "Yeshiva University",
                "Massachusetts Institute of Technology",
                "Cornell University",
                "University of Michigan",
                "Arizona State University",
                "University of Florida"
        );

        // 1. Fetch majors
        List<MajorOutcomeSummary> majors = new ArrayList<>();
        for (String cip : cipCodes) {
            System.out.println("Fetching major: " + cip);
            MajorOutcomeSummary m = new MajorOutcomeSummary();
            m.cipCode = cip;

            List<ScorecardProgram> programs = svc.getProgramsByCip(cip.replace(".", "").substring(0, 4));
            programs.stream()
                    .filter(p -> p.credentialLevel != null && p.credentialLevel == 3)
                    .filter(p -> p.medianEarnings4Yr != null)
                    .filter(p -> p.schoolName != null && schools.stream()
                            .anyMatch(s -> p.schoolName.toLowerCase().contains(s.toLowerCase())))
                    .findFirst()
                    .ifPresent(p -> {
                        m.cipTitle = p.cipTitle;
                        m.credentialLevel = p.credentialLevel;
                        m.scorecardMedianEarnings1Yr = p.medianEarnings1Yr;
                        m.scorecardMedianEarnings4Yr = p.medianEarnings4Yr;
                    });

            List<OnetCrosswalkEntry> crosswalk = svc.getOccupationsForMajor(cip);
            m.relatedSocCodes = crosswalk.stream()
                    .map(e -> e.socCode)
                    .filter(s -> s != null)
                    .toList();
            m.relatedOccupationTitles = crosswalk.stream()
                    .map(e -> e.socTitle)
                    .filter(s -> s != null)
                    .toList();

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
                int total = completions.stream()
                        .filter(c -> c.cipCode != null && c.cipCode.startsWith(cipPrefix))
                        .filter(c -> c.awardLevel != null && c.awardLevel == 5)
                        .mapToInt(c -> c.totalCompletions != null ? c.totalCompletions : 0)
                        .sum();
                m.annualCompletions = total;
                System.out.println("Completions for " + m.cipCode + ": " + total);
            }
        } catch (Exception e) {
            System.out.println("IPEDS failed: " + e.getMessage());
        }

        // 3. OEWS flat file
        Map<String, OewsOccupation> oewsMap = new HashMap<>();
        try {
            oewsMap = OewsParser.parse("oe.data.1.AllData");
        } catch (Exception e) {
            System.out.println("OEWS parse failed: " + e.getMessage());
        }

        System.out.println("OEWS map size: " + oewsMap.size());

        final Map<String, OewsOccupation> finalOewsMap = oewsMap;

        // 4. Collect unique SOC codes
        List<String> allSocs = majors.stream()
                .filter(m -> m.relatedSocCodes != null)
                .flatMap(m -> m.relatedSocCodes.stream())
                .distinct()
                .toList();

        // 5. Collect all crosswalk entries
        List<OnetCrosswalkEntry> allCrosswalk = new ArrayList<>();
        for (MajorOutcomeSummary m : majors) {
            try {
                allCrosswalk.addAll(svc.getOccupationsForMajor(m.cipCode));
                HttpUtil.politeDelay(500);
            } catch (Exception e) {
                System.out.println("Crosswalk fetch failed: " + e.getMessage());
            }
        }

        // 6. Fetch OOH for each unique occupation title
        List<OohOccupation> oohList = new ArrayList<>();
        List<String> oohUrls = new ArrayList<>();
        try {
            oohUrls = svc.getOohClient().getAllOccupationUrls();
        } catch (IOException e) {
            System.out.println("OOH index fetch failed (skipping OOH data): " + e.getMessage());
        }
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
                            System.out.println("OOH fetch failed: " + e.getMessage());
                        }
                    });
        }

        // 7. Build brightOutlook map
        Map<String, OnetCrosswalkEntry> brightOutlookMap = new HashMap<>();
        allCrosswalk.stream()
                .filter(e -> e.tags != null && e.tags.contains("Bright Outlook"))
                .forEach(e -> brightOutlookMap.put(e.socCode, e));

        // 8. Build occupation list with OEWS data
        List<OewsOccupation> occupations = allSocs.stream().map(soc -> {
            OewsOccupation o = finalOewsMap.getOrDefault(soc, new OewsOccupation());
            o.socCode = soc;
            allCrosswalk.stream()
                    .filter(e -> soc.equals(e.socCode))
                    .findFirst()
                    .ifPresent(e -> o.title = e.socTitle);
            return o;
        }).toList();

        // 9. Export
        new DataExporter().export(majors, occupations, oohList, brightOutlookMap, "universitydata.json");
    }
}
