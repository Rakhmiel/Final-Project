package com.finalProject;

import java.util.Objects;

public class OccupationRecord implements Comparable<OccupationRecord> {

    public String  socCode;
    public String  title;
    public double  medianPay;           // OOH preferred, OEWS fallback
    public double  wage10thPct;         // OEWS
    public double  wage25thPct;         // OEWS
    public double  wage75thPct;         // OEWS
    public double  wage90thPct;         // OEWS
    public long    employmentCount;     // OEWS
    public double  jobGrowthRate10Yr;   // OOH (%)
    public long    projectedOpenings;   // OOH
    public String  entryLevelEducation; // OOH
    public boolean brightOutlook;       // O*NET

    public OccupationRecord() {}

    public OccupationRecord(String socCode, String title) {
        this.socCode = socCode.strip();
        this.title   = title.strip();
    }

    @Override
    public int compareTo(OccupationRecord other) {
        return Double.compare(this.medianPay, other.medianPay);
    }

    @Override
    public int hashCode() { return Objects.hash(socCode); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OccupationRecord)) return false;
        return Objects.equals(socCode, ((OccupationRecord) o).socCode);
    }

    @Override
    public String toString() {
        return String.format("%-10s %-40s | Pay: $%,8.0f | Growth: %5.1f%% | Employed: %,8d%s",
                socCode, title, medianPay, jobGrowthRate10Yr, employmentCount,
                brightOutlook ? "  ★" : "");
    }
}
