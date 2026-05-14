package com.universitydata.client;

import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UniversityDataService — the main entry point for your application code.
 *
 * This facade wires together all five data sources and exposes high-level
 * methods that combine data across sources into unified {@link MajorOutcomeSummary}
 * objects. Downstream code should depend only on this class, not on the
 * individual clients.
 *
 * Quick start:
 * <pre>
 *   DataSourceConfig config = DataSourceConfig.builder()
 *       .collegeScorecardApiKey("YOUR_KEY")
 *       .onetUsername("your@email.com")
 *       .onetPassword("yourpassword")
 *       .build();
 *
 *   UniversityDataService svc = new UniversityDataService(config);
 *
 *   // Get enriched outcome data for Computer Science (CIP 11.0101)
 *   MajorOutcomeSummary summary = svc.getMajorOutcome("11.0101", 5, "2022-23");
 *   System.out.println(summary);
 *
 *   // Get all programs from one school
 *   List&lt;ScorecardProgram&gt; programs = svc.getProgramsBySchool("MIT");
 * </pre>
 */
public class UniversityDataService {

    private static final Logger log = LoggerFactory.getLogger(UniversityDataService.class);

    private final CollegeScorecardClient scorecard;
    private final BlsOewsClient          oews;
    private final BlsOohClient           ooh;
    private final IpedsClient            ipeds;
    private final OnetClient             onet;

    public UniversityDataService(DataSourceConfig config) {
        this.scorecard = new CollegeScorecardClient(config);
        this.oews      = new BlsOewsClient(config);
        this.ooh       = new BlsOohClient(config);
        this.ipeds     = new IpedsClient(config);
        this.onet      = new OnetClient(config);
    }

    // ---------------------------------------------------------------
    // High-level: enriched, cross-source summaries
    // ---------------------------------------------------------------

    /**
     * Build a fully enriched MajorOutcomeSummary for a given CIP code.
     * Pulls from Scorecard (earnings/debt), IPEDS (completions), O*NET (crosswalk),
     * OEWS (wages), and OOH (job outlook).
     *
     * @param cipCode       e.g. "11.0101" (Computer Science)
     * @param credentialLevel 5 = bachelor's; 7 = master's; 17 = doctoral
     * @param academicYear  e.g. "2022-23" (for IPEDS completions)
     */
    public MajorOutcomeSummary getMajorOutcome(String cipCode,
                                               int credentialLevel,
                                               String academicYear) throws IOException {
        MajorOutcomeSummary summary = new MajorOutcomeSummary();
        summary.cipCode         = cipCode;
        summary.credentialLevel = credentialLevel;
        summary.academicYear    = academicYear;

        // 1. College Scorecard — earnings and debt
        log.info("[1/5] Fetching Scorecard data for CIP {}...", cipCode);
        try {
            String cip4 = cipTo4Digit(cipCode);
            List<ScorecardProgram> programs = scorecard.getProgramsByCip(cip4);
            List<ScorecardProgram> filtered = programs.stream()
                    .filter(p -> p.credentialLevel != null && p.credentialLevel == credentialLevel)
                    .filter(p -> p.medianEarnings4Yr != null)
                    .toList();

            if (!filtered.isEmpty()) {
                summary.cipTitle = filtered.get(0).cipTitle;
                summary.scorecardMedianEarnings1Yr = medianOf(
                        filtered.stream().map(p -> p.medianEarnings1Yr).toList());
                summary.scorecardMedianEarnings4Yr = medianOf(
                        filtered.stream().map(p -> p.medianEarnings4Yr).toList());
                summary.scorecardMedianDebt = medianOf(
                        filtered.stream().map(p -> p.medianDebt).toList());
            }
        } catch (IOException e) {
            log.warn("Scorecard fetch failed for CIP {}: {}", cipCode, e.getMessage());
        }

        // 2. IPEDS — annual completions
        log.info("[2/5] Fetching IPEDS completions for CIP {}...", cipCode);
        try {
            String cipPrefix = cipCode.replace(".", "").substring(0, Math.min(4, cipCode.replace(".", "").length()));
            List<IpedsCompletion> completions = ipeds.getCompletionsByCipPrefix(academicYear, cipCode.substring(0, 2));
            int total = completions.stream()
                    .filter(c -> c.cipCode != null && c.cipCode.startsWith(cipCode.substring(0, 2)))
                    .filter(c -> c.awardLevel != null && c.awardLevel == credentialLevel)
                    .mapToInt(c -> c.totalCompletions != null ? c.totalCompletions : 0)
                    .sum();
            summary.annualCompletions = total;
        } catch (IOException e) {
            log.warn("IPEDS fetch failed for CIP {}: {}", cipCode, e.getMessage());
        }

        // 3. O*NET — CIP → SOC crosswalk
        log.info("[3/5] Fetching O*NET crosswalk for CIP {}...", cipCode);
        List<String> relatedSocCodes = new ArrayList<>();
        List<String> relatedTitles   = new ArrayList<>();
        try {
            List<OnetCrosswalkEntry> crosswalk = onet.getOccupationsForCip(cipCode);
            for (OnetCrosswalkEntry e : crosswalk) {
                if (e.socCode != null) relatedSocCodes.add(e.socCode);
                if (e.socTitle != null) relatedTitles.add(e.socTitle);
            }
            summary.relatedSocCodes        = relatedSocCodes;
            summary.relatedOccupationTitles = relatedTitles;
        } catch (IOException e) {
            log.warn("O*NET crosswalk failed for CIP {}: {}", cipCode, e.getMessage());
        }

        // 4. BLS OEWS — wages for related occupations
        if (!relatedSocCodes.isEmpty()) {
            log.info("[4/5] Fetching OEWS wages for {} related occupations...", relatedSocCodes.size());
            try {
                List<OewsOccupation> wages = oews.getOccupationsBySOCs(relatedSocCodes);
                OptionalDouble median = wages.stream()
                        .filter(o -> o.annualMedianWage != null)
                        .mapToDouble(o -> o.annualMedianWage)
                        .average();
                summary.blsMedianAnnualWage = median.isPresent() ? median.getAsDouble() : null;
            } catch (IOException e) {
                log.warn("OEWS fetch failed: {}", e.getMessage());
            }
        }

        // 5. BLS OOH — job outlook for primary occupation
        if (!relatedTitles.isEmpty()) {
            log.info("[5/5] Fetching OOH outlook for '{}'...", relatedTitles.get(0));
            try {
                OohOccupation outlook = ooh.findOccupationByTitle(relatedTitles.get(0));
                if (outlook != null) {
                    summary.blsProjectedGrowthRate    = outlook.jobGrowthRate10Yr;
                    summary.blsProjectedOpenings10Yr  = outlook.projectedOpenings10Yr;
                    summary.entryLevelEducationRequired = outlook.entryLevelEducation;
                }
            } catch (IOException e) {
                log.warn("OOH scrape failed: {}", e.getMessage());
            }
        }

        log.info("MajorOutcomeSummary built: {}", summary);
        return summary;
    }

    // ---------------------------------------------------------------
    // Direct access to individual clients
    // ---------------------------------------------------------------

    /** All programs from a specific school (by name fragment). */
    public List<ScorecardProgram> getProgramsBySchool(String schoolName) throws IOException {
        return scorecard.getProgramsBySchool(schoolName);
    }

    /** All programs for a specific CIP code across all schools. */
    public List<ScorecardProgram> getProgramsByCip(String cipCode) throws IOException {
        return scorecard.getProgramsByCip(cipTo4Digit(cipCode));
    }

    /** Complete list of schools with their UNITID (for joining with IPEDS). */
    public List<CollegeScorecardClient.SchoolInfo> getAllSchools() throws IOException {
        return scorecard.getAllSchools();
    }

    /** OEWS wage data for a single occupation by SOC code. */
    public OewsOccupation getOccupationWages(String socCode) throws IOException {
        return oews.getOccupationBySOC(socCode);
    }

    /** OOH scrape for a single occupation page URL. */
    public OohOccupation getOohOccupation(String occupationUrl) throws IOException {
        return ooh.scrapeOccupationPage(occupationUrl);
    }

    /** All OOH occupations (takes ~15 min with polite delays). */
    public List<OohOccupation> getAllOohOccupations() throws IOException {
        return ooh.scrapeAllOccupations();
    }

    /** IPEDS completions by academic year. */
    public List<IpedsCompletion> getIpedsCompletions(String academicYear) throws IOException {
        return ipeds.getCompletions(academicYear);
    }

    /** O*NET occupations related to a CIP code. */
    public List<OnetCrosswalkEntry> getOccupationsForMajor(String cipCode) throws IOException {
        return onet.getOccupationsForCip(cipCode);
    }

    // ---------------------------------------------------------------
    // Expose clients for advanced usage
    // ---------------------------------------------------------------

    public CollegeScorecardClient getScorecardClient() { return scorecard; }
    public BlsOewsClient          getOewsClient()      { return oews; }
    public BlsOohClient           getOohClient()       { return ooh; }
    public IpedsClient            getIpedsClient()     { return ipeds; }
    public OnetClient             getOnetClient()      { return onet; }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    /** Convert dotted CIP to 4-digit form for Scorecard: "11.0101" → "1101" */
    private static String cipTo4Digit(String cip) {
        String clean = cip.replace(".", "");
        return clean.length() >= 4 ? clean.substring(0, 4) : clean;
    }

    /** Compute the median of a list of (possibly null) integers */
    private static Integer medianOf(List<Integer> values) {
        List<Integer> sorted = values.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        if (sorted.isEmpty()) return null;
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(mid);
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2;
    }
}
