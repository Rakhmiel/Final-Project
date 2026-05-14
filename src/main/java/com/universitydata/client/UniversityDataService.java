package com.universitydata.client;

import com.universitydata.config.DataSourceConfig;
import com.universitydata.model.UniversityDataModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public MajorOutcomeSummary getMajorOutcome(String cipCode,
                                               int credentialLevel,
                                               String academicYear) throws IOException {
        MajorOutcomeSummary summary = new MajorOutcomeSummary();
        summary.cipCode         = cipCode;
        summary.credentialLevel = credentialLevel;
        summary.academicYear    = academicYear;

        log.info("[1/5] Fetching Scorecard data for CIP {}...", cipCode);
        try {
            List<ScorecardProgram> programs = scorecard.getProgramsByCip(cipTo4Digit(cipCode));
            List<ScorecardProgram> filtered = programs.stream()
                    .filter(p -> p.credentialLevel != null && p.credentialLevel == credentialLevel)
                    .filter(p -> p.medianEarnings4Yr != null)
                    .toList();
            if (!filtered.isEmpty()) {
                summary.cipTitle = filtered.get(0).cipTitle;
                summary.scorecardMedianEarnings1Yr = medianOf(filtered.stream().map(p -> p.medianEarnings1Yr).toList());
                summary.scorecardMedianEarnings4Yr = medianOf(filtered.stream().map(p -> p.medianEarnings4Yr).toList());
                summary.scorecardMedianDebt = medianOf(filtered.stream().map(p -> p.medianDebt).toList());
            }
        } catch (IOException e) {
            log.warn("Scorecard fetch failed for CIP {}: {}", cipCode, e.getMessage());
        }

        log.info("[2/5] Fetching IPEDS completions for CIP {}...", cipCode);
        try {
            List<IpedsCompletion> completions = ipeds.getCompletions(academicYear);
            int total = completions.stream()
                    .filter(c -> c.cipCode != null && c.cipCode.startsWith(cipCode.substring(0, 2)))
                    .filter(c -> c.awardLevel != null && c.awardLevel == credentialLevel)
                    .mapToInt(c -> c.totalCompletions != null ? c.totalCompletions : 0)
                    .sum();
            summary.annualCompletions = total;
        } catch (IOException e) {
            log.warn("IPEDS fetch failed for CIP {}: {}", cipCode, e.getMessage());
        }

        log.info("[3/5] Fetching O*NET crosswalk for CIP {}...", cipCode);
        List<String> relatedSocCodes = new ArrayList<>();
        List<String> relatedTitles   = new ArrayList<>();
        try {
            List<OnetCrosswalkEntry> crosswalk = onet.getOccupationsForCip(cipCode);
            for (OnetCrosswalkEntry e : crosswalk) {
                if (e.socCode != null) relatedSocCodes.add(e.socCode);
                if (e.socTitle != null) relatedTitles.add(e.socTitle);
            }
            summary.relatedSocCodes         = relatedSocCodes;
            summary.relatedOccupationTitles = relatedTitles;
        } catch (IOException e) {
            log.warn("O*NET crosswalk failed for CIP {}: {}", cipCode, e.getMessage());
        }

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

        if (!relatedTitles.isEmpty()) {
            log.info("[5/5] Fetching OOH outlook for '{}'...", relatedTitles.get(0));
            try {
                OohOccupation outlook = ooh.findOccupationByTitle(relatedTitles.get(0));
                if (outlook != null) {
                    summary.blsProjectedGrowthRate      = outlook.jobGrowthRate10Yr;
                    summary.blsProjectedOpenings10Yr    = outlook.projectedOpenings10Yr;
                    summary.entryLevelEducationRequired = outlook.entryLevelEducation;
                }
            } catch (IOException e) {
                log.warn("OOH scrape failed: {}", e.getMessage());
            }
        }

        log.info("MajorOutcomeSummary built: {}", summary);
        return summary;
    }

    public List<ScorecardProgram> getProgramsBySchool(String schoolName) throws IOException {
        return scorecard.getProgramsBySchool(schoolName);
    }

    public List<ScorecardProgram> getProgramsByCip(String cipCode) throws IOException {
        return scorecard.getProgramsByCip(cipTo4Digit(cipCode));
    }

    public List<CollegeScorecardClient.SchoolInfo> getAllSchools() throws IOException {
        return scorecard.getAllSchools();
    }

    public OewsOccupation getOccupationWages(String socCode) throws IOException {
        return oews.getOccupationBySOC(socCode);
    }

    public OohOccupation getOohOccupation(String occupationUrl) throws IOException {
        return ooh.scrapeOccupationPage(occupationUrl);
    }

    public List<OohOccupation> getAllOohOccupations() throws IOException {
        return ooh.scrapeAllOccupations();
    }

    public List<IpedsCompletion> getIpedsCompletions(String academicYear) throws IOException {
        return ipeds.getCompletions(academicYear);
    }

    public List<OnetCrosswalkEntry> getOccupationsForMajor(String cipCode) throws IOException {
        return onet.getOccupationsForCip(cipCode);
    }

    public CollegeScorecardClient getScorecardClient() { return scorecard; }
    public BlsOewsClient          getOewsClient()      { return oews; }
    public BlsOohClient           getOohClient()       { return ooh; }
    public IpedsClient            getIpedsClient()     { return ipeds; }
    public OnetClient             getOnetClient()      { return onet; }

    private static String cipTo4Digit(String cip) {
        String clean = cip.replace(".", "");
        return clean.length() >= 4 ? clean.substring(0, 4) : clean;
    }

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
