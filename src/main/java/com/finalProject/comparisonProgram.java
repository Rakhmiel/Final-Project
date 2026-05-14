package com.finalProject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.finalProject.HashSet.HashTableImpl;

public class comparisonProgram {

    private static final String DEFAULT_FILE = "data.json";

    private static final HashTableImpl<String, MajorRecord>      MAJORS      = new HashTableImpl<>(64);
    private static final HashTableImpl<String, OccupationRecord> OCCUPATIONS = new HashTableImpl<>(64);
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Major & Occupation Outcomes Explorer ===");

        if (args.length > 0) {
            System.out.println("Loading: " + args[0]);
            loadFile(args[0]);
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1"  -> addMajor();
                case "2"  -> addOccupation();
                case "3"  -> listMajors();
                case "4"  -> listOccupations();
                case "5"  -> viewMajorOccupations();
                case "6"  -> rankMajorsByEarnings();
                case "7"  -> rankOccupationsByPay();
                case "8"  -> rankOccupationsByGrowth();
                case "9"  -> promptLoad();
                case "10" -> promptSave();
                case "0"  -> running = false;
                default   -> System.out.println("Unknown option. Try again.");
            }
            System.out.println();
        }
        System.out.println("Goodbye.");
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private static void printMenu() {
        System.out.println("-----------------------------------");
        System.out.printf(" Majors: %d  |  Occupations: %d%n", MAJORS.size(), OCCUPATIONS.size());
        System.out.println("-----------------------------------");
        System.out.println(" 1. Add major (CIP)");
        System.out.println(" 2. Add occupation (SOC)");
        System.out.println(" 3. List all majors");
        System.out.println(" 4. List all occupations");
        System.out.println(" 5. View major → linked occupations");
        System.out.println(" 6. Rank majors by 4-year earnings");
        System.out.println(" 7. Rank occupations by median pay");
        System.out.println(" 8. Rank occupations by job growth");
        System.out.println(" 9. Load JSON file");
        System.out.println("10. Save to JSON file");
        System.out.println(" 0. Exit");
        System.out.print("Choice: ");
    }

    // ── Add major ─────────────────────────────────────────────────────────────

    private static void addMajor() {
        System.out.println("--- Add Major (CIP) ---");
        String cipCode     = prompt("CIP code (e.g. 11.0101)").toUpperCase();
        String cipTitle    = prompt("Title (e.g. Computer Science)");
        int    level       = promptInt("Credential level  1=cert 2=assoc 3=bach 5=master 7=doctoral");
        int    completions = promptInt("Annual completions (IPEDS)");
        int    earn1yr     = promptInt("Median earnings 1yr post-grad ($)");
        int    earn4yr     = promptInt("Median earnings 4yr post-grad ($)");
        String socInput    = prompt("Related SOC codes, comma-separated (or press Enter to skip)");

        MajorRecord rec = new MajorRecord(cipCode, cipTitle, level);
        rec.annualCompletions = completions;
        rec.medianEarnings1Yr = earn1yr;
        rec.medianEarnings4Yr = earn4yr;

        if (!socInput.isBlank()) {
            for (String soc : socInput.split(",")) {
                String s = soc.strip();
                if (!s.isEmpty()) rec.relatedSocCodes.add(s);
            }
        }

        MAJORS.put(cipCode, rec);
        System.out.println("Added: " + rec);
    }

    // ── Add occupation ────────────────────────────────────────────────────────

    private static void addOccupation() {
        System.out.println("--- Add Occupation (SOC) ---");
        String  socCode = prompt("SOC code (e.g. 15-1252)");
        String  title   = prompt("Title");
        double  pay     = promptDouble("Median pay ($)");
        double  p10     = promptDouble("10th percentile wage ($)");
        double  p25     = promptDouble("25th percentile wage ($)");
        double  p75     = promptDouble("75th percentile wage ($)");
        double  p90     = promptDouble("90th percentile wage ($)");
        long    emp     = promptLong("Total employed (national)");
        double  growth  = promptDouble("10yr job growth rate (%)");
        long    opens   = promptLong("Projected openings over 10yr");
        String  edu     = prompt("Entry-level education required");
        boolean bright  = promptYesNo("Bright Outlook? (y/n)");

        OccupationRecord rec = new OccupationRecord(socCode, title);
        rec.medianPay           = pay;
        rec.wage10thPct         = p10;
        rec.wage25thPct         = p25;
        rec.wage75thPct         = p75;
        rec.wage90thPct         = p90;
        rec.employmentCount     = emp;
        rec.jobGrowthRate10Yr   = growth;
        rec.projectedOpenings   = opens;
        rec.entryLevelEducation = edu;
        rec.brightOutlook       = bright;

        OCCUPATIONS.put(socCode, rec);
        System.out.println("Added: " + rec);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    private static void listMajors() {
        if (MAJORS.size() == 0) { System.out.println("No majors loaded."); return; }
        printHeader("All Majors");
        List<MajorRecord> sorted = MAJORS.getSorted(Collections.reverseOrder());
        for (int i = 0; i < sorted.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, sorted.get(i));
        }
    }

    private static void listOccupations() {
        if (OCCUPATIONS.size() == 0) { System.out.println("No occupations loaded."); return; }
        printHeader("All Occupations");
        List<OccupationRecord> sorted = OCCUPATIONS.getSorted(Collections.reverseOrder());
        for (int i = 0; i < sorted.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, sorted.get(i));
        }
    }

    // ── Major → occupations ───────────────────────────────────────────────────

    private static void viewMajorOccupations() {
        String cipCode = prompt("CIP code").toUpperCase();
        MajorRecord major = MAJORS.get(cipCode);
        if (major == null) {
            System.out.println("Major not found: " + cipCode);
            return;
        }

        printHeader("Major: " + major.cipTitle + " (" + cipCode + ")");
        System.out.printf("  Credential:   %s%n", major.credentialLabel());
        System.out.printf("  Completions:  %,d / yr%n", major.annualCompletions);
        System.out.printf("  Earnings 1yr: $%,d%n", major.medianEarnings1Yr);
        System.out.printf("  Earnings 4yr: $%,d%n", major.medianEarnings4Yr);

        if (major.relatedSocCodes == null || major.relatedSocCodes.isEmpty()) {
            System.out.println("  No linked occupations.");
            return;
        }

        System.out.println("\n  Linked Occupations:");
        System.out.printf("  %-10s %-35s %12s %10s %12s %s%n",
                "SOC", "Title", "Median Pay", "Growth", "Employed", "Bright");
        System.out.println("  " + "-".repeat(90));

        for (String soc : major.relatedSocCodes) {
            OccupationRecord occ = OCCUPATIONS.get(soc);
            if (occ == null) {
                System.out.printf("  %-10s (not loaded)%n", soc);
            } else {
                System.out.printf("  %-10s %-35s $%,10.0f %9.1f%% %,12d %s%n",
                        occ.socCode, occ.title, occ.medianPay,
                        occ.jobGrowthRate10Yr, occ.employmentCount,
                        occ.brightOutlook ? "★" : "");
            }
        }
    }

    // ── Rankings ──────────────────────────────────────────────────────────────

    private static void rankMajorsByEarnings() {
        if (MAJORS.size() == 0) { System.out.println("No majors loaded."); return; }
        List<MajorRecord> sorted = MAJORS.getSorted(Collections.reverseOrder());
        printHeader("Majors Ranked by 4-Year Earnings");
        System.out.printf("%-4s %-12s %-35s %12s %12s %s%n",
                "#", "CIP", "Title", "1yr Earnings", "4yr Earnings", "Completions");
        System.out.println("-".repeat(85));
        for (int i = 0; i < sorted.size(); i++) {
            MajorRecord m = sorted.get(i);
            System.out.printf("%-4d %-12s %-35s $%,10d $%,10d %,8d%n",
                    i + 1, m.cipCode, m.cipTitle,
                    m.medianEarnings1Yr, m.medianEarnings4Yr, m.annualCompletions);
        }
    }

    private static void rankOccupationsByPay() {
        if (OCCUPATIONS.size() == 0) { System.out.println("No occupations loaded."); return; }
        printOccupationRanking("Occupations Ranked by Median Pay",
                OCCUPATIONS.getSorted(Collections.reverseOrder()));
    }

    private static void rankOccupationsByGrowth() {
        if (OCCUPATIONS.size() == 0) { System.out.println("No occupations loaded."); return; }
        printOccupationRanking("Occupations Ranked by 10-Year Job Growth",
                OCCUPATIONS.getSorted((a, b) -> Double.compare(b.jobGrowthRate10Yr, a.jobGrowthRate10Yr)));
    }

    private static void printOccupationRanking(String title, List<OccupationRecord> sorted) {
        printHeader(title);
        System.out.printf("%-4s %-10s %-35s %12s %10s %12s %s%n",
                "#", "SOC", "Title", "Median Pay", "Growth", "Employed", "Bright");
        System.out.println("-".repeat(90));
        for (int i = 0; i < sorted.size(); i++) {
            OccupationRecord o = sorted.get(i);
            System.out.printf("%-4d %-10s %-35s $%,10.0f %9.1f%% %,12d %s%n",
                    i + 1, o.socCode, o.title, o.medianPay,
                    o.jobGrowthRate10Yr, o.employmentCount,
                    o.brightOutlook ? "★" : "");
        }
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    private static void promptLoad() {
        System.out.print("File path [" + DEFAULT_FILE + "]: ");
        String path = in.nextLine().trim();
        if (path.isEmpty()) path = DEFAULT_FILE;
        loadFile(path);
    }

    private static void promptSave() {
        System.out.print("File path [" + DEFAULT_FILE + "]: ");
        String path = in.nextLine().trim();
        if (path.isEmpty()) path = DEFAULT_FILE;
        saveFile(path);
    }

    private static void loadFile(String path) {
        try {
            DataManager.LoadResult result = DataManager.load(path);
            for (MajorRecord m : result.majors)           MAJORS.put(m.cipCode, m);
            for (OccupationRecord o : result.occupations) OCCUPATIONS.put(o.socCode, o);
            System.out.printf("Loaded %d majors and %d occupations from %s%n",
                    result.majors.size(), result.occupations.size(), path);
        } catch (IOException e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }

    private static void saveFile(String path) {
        try {
            List<MajorRecord>      majorList = MAJORS.getSorted(Collections.reverseOrder());
            List<OccupationRecord> occList   = OCCUPATIONS.getSorted(Collections.reverseOrder());
            DataManager.save(majorList, occList, path);
            System.out.printf("Saved %d majors and %d occupations to %s%n",
                    majorList.size(), occList.size(), path);
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim();
    }

    private static int promptInt(String label) {
        while (true) {
            System.out.print(label + ": ");
            try { return Integer.parseInt(in.nextLine().trim().replace(",", "")); }
            catch (NumberFormatException e) { System.out.println("  Enter a whole number."); }
        }
    }

    private static long promptLong(String label) {
        while (true) {
            System.out.print(label + ": ");
            try { return Long.parseLong(in.nextLine().trim().replace(",", "")); }
            catch (NumberFormatException e) { System.out.println("  Enter a whole number."); }
        }
    }

    private static double promptDouble(String label) {
        while (true) {
            System.out.print(label + ": ");
            try { return Double.parseDouble(in.nextLine().trim().replace(",", "")); }
            catch (NumberFormatException e) { System.out.println("  Enter a number."); }
        }
    }

    private static boolean promptYesNo(String label) {
        System.out.print(label + ": ");
        return in.nextLine().trim().toLowerCase().startsWith("y");
    }

    private static void printHeader(String title) {
        System.out.println("=== " + title + " ===");
    }
}
