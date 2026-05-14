package com.universitydata;

import com.universitydata.client.UniversityDataService;
import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;

import java.util.List;

/**
 * Runnable demo showing how to use UniversityDataService.
 *
 * Set your API keys before running:
 *   - SCORECARD_API_KEY: https://api.data.gov/signup/
 *   - ONET_USER / ONET_PASS: https://services.onetcenter.org/developer/
 *
 * BLS (OEWS/OOH) and IPEDS require no keys.
 */
public class UniversityDataDemo {
    public static void main(String[] args) throws Exception {
        DataSourceConfig config = DataSourceConfig.builder()
            .collegeScorecardApiKey("UErh64HhHezs7idMZ9uYbhqgDAQif6cpgjcUISvz")
            .onetApiKey("RlSNc-wN5Tq-UZIHq-bMVDn")
            .build();

        UniversityDataService svc = new UniversityDataService(config);

    // 1. Scorecard
    System.out.println("\n=== SCORECARD ===");
    try {
        List<ScorecardProgram> r = svc.getProgramsByCip("1101");
        System.out.println("OK — records: " + r.size());
        if (!r.isEmpty()) System.out.println("Sample: " + r.get(0));
    } catch (Exception e) { System.out.println("FAIL: " + e.getMessage()); }

    // 2. OOH
    System.out.println("\n=== OOH (scrape) ===");
    try {
        OohOccupation r = svc.getOohOccupation(
            "https://www.bls.gov/ooh/computer-and-information-technology/software-developers.htm");
        System.out.println("OK — " + r);
    } catch (Exception e) { System.out.println("FAIL: " + e.getMessage()); }

    // 3. O*NET
    System.out.println("\n=== O*NET ===");
    try {
        List<OnetCrosswalkEntry> r = svc.getOccupationsForMajor("11.0101");
        System.out.println("OK — records: " + r.size());
        if (!r.isEmpty()) System.out.println("Sample: " + r.get(0).socCode + " " + r.get(0).socTitle);
    } catch (Exception e) { System.out.println("FAIL: " + e.getMessage()); }
}
    }
