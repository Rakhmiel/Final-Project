package com.universitydata.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * All model (data transfer) classes for university outcome data.
 * Each inner class corresponds to a distinct concept/source.
 * Jackson annotations are applied so raw API JSON deserializes cleanly.
 * Fields left null means the source didn't provide that data point.
 */
public class UniversityDataModels {

    // =========================================================
    // College Scorecard — Field of Study results
    // =========================================================

    /** One "program" record from the College Scorecard field-of-study endpoint. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScorecardProgram {
        /** School name */
        public String schoolName;
        /** School UNITID (joins with IPEDS) */
        public String unitId;
        /** CIP 4-digit code, e.g. "1101" for Computer Science */
        public String cipCode;
        /** Human-readable CIP title, e.g. "Computer Science" */
        public String cipTitle;
        /** Credential level: 1=certificate, 2=associate, 3=bachelor, 4=post-bacc, etc. */
        public Integer credentialLevel;
        /** Median earnings 1 year after completion, in USD */
        public Integer medianEarnings1Yr;
        /** Median earnings 4 years after completion, in USD */
        public Integer medianEarnings4Yr;
        /** Median debt at graduation, in USD */
        public Integer medianDebt;
        /** Number of students in the earnings cohort */
        public Integer earningsCohortSize;

        @Override
        public String toString() {
            return String.format("ScorecardProgram{school='%s', cip='%s %s', level=%d, earn1yr=$%s, earn4yr=$%s}",
                    schoolName, cipCode, cipTitle, credentialLevel, medianEarnings1Yr, medianEarnings4Yr);
        }
    }

    /** Paginated container returned from a Scorecard schools query. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScorecardPage {
        public int total;
        public int page;
        public int perPage;
        public List<ScorecardProgram> results;
    }

    // =========================================================
    // BLS OEWS — Occupation wages
    // =========================================================

    /** Occupation wage data from BLS Occupational Employment & Wage Statistics. */
    public static class OewsOccupation {
        /** 6-digit SOC code, e.g. "15-1252" for Software Developers */
        public String socCode;
        /** Occupation title */
        public String title;
        /** Total employed nationally */
        public Long totalEmployed;
        /** Hourly mean wage in USD */
        public Double hourlyMeanWage;
        /** Annual mean wage in USD */
        public Double annualMeanWage;
        /** 10th percentile annual wage */
        public Double annualWage10thPct;
        /** 25th percentile annual wage */
        public Double annualWage25thPct;
        /** Median (50th pct) annual wage */
        public Double annualMedianWage;
        /** 75th percentile annual wage */
        public Double annualWage75thPct;
        /** 90th percentile annual wage */
        public Double annualWage90thPct;
        /** State or area (null = national) */
        public String area;

        @Override
        public String toString() {
            return String.format("OewsOccupation{soc='%s', title='%s', median=$%.0f, employed=%d}",
                    socCode, title, annualMedianWage, totalEmployed);
        }
    }

    // =========================================================
    // BLS OOH — Job outlook (scraped)
    // =========================================================

    /** Occupation outlook from BLS Occupational Outlook Handbook (scraped). */
    public static class OohOccupation {
        /** Occupation name as listed in OOH */
        public String title;
        /** OOH page URL */
        public String url;
        /** Median annual pay in USD */
        public Integer medianAnnualPay;
        /** Entry-level education requirement, e.g. "Bachelor's degree" */
        public String entryLevelEducation;
        /** Work experience required */
        public String workExperience;
        /** On-the-job training */
        public String onJobTraining;
        /** Total employment (number of jobs) */
        public Long numberOfJobs;
        /** 10-year projected job growth rate, e.g. 25.0 = 25% */
        public Double jobGrowthRate10Yr;
        /** 10-year projected job openings (includes growth + replacements) */
        public Long projectedOpenings10Yr;
        /** Outlook summary, e.g. "Much faster than average" */
        public String outlookSummary;

        @Override
        public String toString() {
            return String.format("OohOccupation{title='%s', medianPay=$%d, growth=%.1f%%, edu='%s'}",
                    title, medianAnnualPay, jobGrowthRate10Yr, entryLevelEducation);
        }
    }

    // =========================================================
    // IPEDS — Completion counts by major
    // =========================================================

    /** Completion data from IPEDS (degrees awarded by CIP and credential level). */
    public static class IpedsCompletion {
        /** IPEDS UNITID */
        public String unitId;
        /** Institution name */
        public String institutionName;
        /** 6-digit CIP code */
        public String cipCode;
        /** CIP title */
        public String cipTitle;
        /** Award level code (1–17, corresponds to cert/assoc/bach/masters/doctoral) */
        public Integer awardLevel;
        /** Human-readable award level label */
        public String awardLevelLabel;
        /** Total completions (all genders, all races) */
        public Integer totalCompletions;
        /** Academic year, e.g. "2022-23" */
        public String academicYear;

        @Override
        public String toString() {
            return String.format("IpedsCompletion{cip='%s %s', level=%s, completions=%d, year=%s}",
                    cipCode, cipTitle, awardLevelLabel, totalCompletions, academicYear);
        }
    }

    // =========================================================
    // O*NET — CIP→SOC crosswalk + occupation details
    // =========================================================

    /** A single CIP→SOC mapping from the O*NET crosswalk. */
    public static class OnetCrosswalkEntry {
        /** 6-digit CIP code */
        public String cipCode;
        /** CIP title (major name) */
        public String cipTitle;
        /** 6-digit SOC code */
        public String socCode;
        /** SOC / O*NET occupation title */
        public String socTitle;
        /** Tags on the relationship, e.g. "Bright Outlook", "Green" */
        public List<String> tags;
    }

    /** Occupation detail from O*NET Web Services. */
    public static class OnetOccupation {
        /** O*NET-SOC code, e.g. "15-1252.00" */
        public String code;
        /** Occupation title */
        public String title;
        /** Description */
        public String description;
        /** Whether this is flagged as Bright Outlook (fast growing/high openings) */
        public boolean brightOutlook;
        /** Whether it's a Green economy occupation */
        public boolean green;
        /** Sample reported job titles */
        public List<String> sampleJobTitles;

        @Override
        public String toString() {
            return String.format("OnetOccupation{code='%s', title='%s', brightOutlook=%b}",
                    code, title, brightOutlook);
        }
    }

    // =========================================================
    // Unified / Enriched — for downstream use
    // =========================================================

    /**
     * A denormalized, enriched record combining data from multiple sources
     * around a single major (CIP code). Your downstream code can consume this
     * instead of juggling multiple source types.
     */
    public static class MajorOutcomeSummary {
        // --- Identity ---
        public String cipCode;
        public String cipTitle;
        public Integer credentialLevel;

        // --- Earnings (Scorecard) ---
        public Integer scorecardMedianEarnings1Yr;
        public Integer scorecardMedianEarnings4Yr;
        public Integer scorecardMedianDebt;

        // --- Completions (IPEDS) ---
        public Integer annualCompletions;
        public String academicYear;

        // --- Occupation outcomes (O*NET crosswalk → OEWS + OOH) ---
        public List<String> relatedSocCodes;
        public List<String> relatedOccupationTitles;
        public Double blsMedianAnnualWage;         // from OEWS, median across related SOCs
        public Double blsProjectedGrowthRate;      // from OOH
        public Long   blsProjectedOpenings10Yr;    // from OOH
        public String entryLevelEducationRequired; // from OOH

        @Override
        public String toString() {
            return String.format(
                "MajorOutcome{cip='%s %s', earn4yr=$%s, blsWage=$%.0f, growth=%.1f%%, completions=%d}",
                cipCode, cipTitle, scorecardMedianEarnings4Yr,
                blsMedianAnnualWage != null ? blsMedianAnnualWage : 0.0,
                blsProjectedGrowthRate != null ? blsProjectedGrowthRate : 0.0,
                annualCompletions != null ? annualCompletions : 0
            );
        }
    }
}
