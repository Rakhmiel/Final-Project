package com.finalProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MajorRecord implements Comparable<MajorRecord> {

    public String       cipCode;           // e.g. "11.0101"
    public String       cipTitle;          // e.g. "Computer Science"
    public int          credentialLevel;   // 1=cert 2=assoc 3=bach 5=master 7=doctoral
    public int          annualCompletions; // IPEDS
    public int          medianEarnings1Yr; // Scorecard — 1 yr post-grad
    public int          medianEarnings4Yr; // Scorecard — 4 yr post-grad
    public List<String> relatedSocCodes;   // O*NET crosswalk

    public MajorRecord() {
        this.relatedSocCodes = new ArrayList<>();
    }

    public MajorRecord(String cipCode, String cipTitle, int credentialLevel) {
        this.cipCode         = cipCode.strip().toUpperCase();
        this.cipTitle        = cipTitle.strip();
        this.credentialLevel = credentialLevel;
        this.relatedSocCodes = new ArrayList<>();
    }

    public String credentialLabel() {
        return switch (credentialLevel) {
            case 1  -> "Certificate";
            case 2  -> "Associate";
            case 3  -> "Bachelor's";
            case 4  -> "Post-bacc cert";
            case 5  -> "Master's";
            case 6  -> "Post-master's cert";
            case 7  -> "Doctoral";
            case 17 -> "Professional";
            default -> "Level " + credentialLevel;
        };
    }

    @Override
    public int compareTo(MajorRecord other) {
        return Integer.compare(this.medianEarnings4Yr, other.medianEarnings4Yr);
    }

    @Override
    public int hashCode() { return Objects.hash(cipCode); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MajorRecord)) return false;
        return Objects.equals(cipCode, ((MajorRecord) o).cipCode);
    }

    @Override
    public String toString() {
        return String.format("%-12s %-35s | %s | 1yr: $%,8d | 4yr: $%,8d | Completions: %,6d",
                cipCode, cipTitle, credentialLabel(),
                medianEarnings1Yr, medianEarnings4Yr, annualCompletions);
    }
}
