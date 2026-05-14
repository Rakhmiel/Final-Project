package finalProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import finalProject.HashSet.HashTableImpl;

public class comparisonProgram {

    // 2024 US data — overridable at runtime or via JSON
    private static double MEDIAN_WAGE = 59_000;
    private static double GDPC        = 82_000;

    private static final String DEFAULT_FILE = "people.json";
    private static final ArrayList<personObject> people = new ArrayList<>();
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Economic Success Statistics App ===");

        // Optional: load a JSON file passed as a command-line argument
        // Usage: mvn exec:java -Dexec.args="mydata.json"
        if (args.length > 0) {
            System.out.println("Loading: " + args[0]);
            loadFile(args[0]);
        }

        System.out.printf("Benchmarks: Median Wage = $%,.0f  |  GDPC = $%,.0f%n%n",
                MEDIAN_WAGE, GDPC);

        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1"  -> addPerson();
                case "2"  -> listAll();
                case "3"  -> rankBySalary();
                case "4"  -> rankByScore();
                case "5"  -> statsByGroup("major");
                case "6"  -> statsByGroup("industry");
                case "7"  -> statsByGroup("institution");
                case "8"  -> statsByGroup("birthplace");
                case "9"  -> updateBenchmarks();
                case "10" -> promptLoad();
                case "11" -> promptSave();
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
        System.out.printf(" People: %d  |  Median: $%,.0f  |  GDPC: $%,.0f%n",
                people.size(), MEDIAN_WAGE, GDPC);
        System.out.println("-----------------------------------");
        System.out.println(" 1. Add person");
        System.out.println(" 2. List all people");
        System.out.println(" 3. Rank by salary");
        System.out.println(" 4. Rank by success score");
        System.out.println(" 5. Stats by major");
        System.out.println(" 6. Stats by industry");
        System.out.println(" 7. Stats by institution");
        System.out.println(" 8. Stats by birth place");
        System.out.println(" 9. Update benchmarks");
        System.out.println("10. Load JSON file");
        System.out.println("11. Save to JSON file");
        System.out.println(" 0. Exit");
        System.out.print("Choice: ");
    }

    // ── Add person ────────────────────────────────────────────────────────────

    private static void addPerson() {
        System.out.println("--- Add Person ---");
        String name        = prompt("Name");
        int    age         = promptInt("Age");
        double salary      = promptDouble("Salary ($)");
        double gpa         = promptDouble("GPA (0.0 – 4.0)");
        String institution = prompt("Educational institution");
        String birthPlace  = prompt("Birth place (city / state / country)");
        String major       = prompt("Major / field of study");
        String industry    = prompt("Industry");

        personObject p = new personObject(name, age, salary, gpa,
                institution, birthPlace, major, industry,
                MEDIAN_WAGE, GDPC);
        people.add(p);
        System.out.printf("Added: %s  |  Score: %.1f  |  %s%n",
                name, p.getSuccess(),
                p.isSuccessful() ? "Above median wage *" : "Below median wage");
    }

    // ── List / rankings ───────────────────────────────────────────────────────

    private static void listAll() {
        if (empty()) return;
        printHeader("All People  (* = above median wage)");
        for (int i = 0; i < people.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, people.get(i));
        }
    }

    private static void rankBySalary() {
        if (empty()) return;
        ArrayList<personObject> sorted = new ArrayList<>(people);
        sorted.sort(Collections.reverseOrder());
        printHeader("Ranked by Salary  (* = above median wage)");
        for (int i = 0; i < sorted.size(); i++) {
            personObject p = sorted.get(i);
            System.out.printf("%3d. %-22s  $%,12.0f%s%n",
                    i + 1, p.getName(), p.getSalary(),
                    p.isSuccessful() ? "  *" : "");
        }
        printSalaryStats(sorted);
    }

    private static void rankByScore() {
        if (empty()) return;
        ArrayList<personObject> sorted = new ArrayList<>(people);
        sorted.sort((a, b) -> Double.compare(b.getSuccess(), a.getSuccess()));
        printHeader("Ranked by Success Score  (10.0 = benchmark)");
        System.out.printf("  Benchmark = (median $%,.0f + GDPC $%,.0f) / 2 = $%,.0f%n%n",
                MEDIAN_WAGE, GDPC, (MEDIAN_WAGE + GDPC) / 2);
        for (int i = 0; i < sorted.size(); i++) {
            personObject p = sorted.get(i);
            System.out.printf("%3d. %-22s  Score: %6.2f  Salary: $%,12.0f  Major: %s%n",
                    i + 1, p.getName(), p.getSuccess(), p.getSalary(), p.getMajor());
        }
        printSalaryStats(sorted);
    }

    // ── Group statistics ──────────────────────────────────────────────────────

    private static void statsByGroup(String field) {
        if (empty()) return;

        // Build aggregates using the custom hash table (keyed by group name)
        HashTableImpl<String, majorObject> table = new HashTableImpl<>(32);
        for (personObject p : people) {
            String key = switch (field) {
                case "major"       -> p.getMajor();
                case "industry"    -> p.getIndustry().toUpperCase();
                case "institution" -> p.getInstitution().toUpperCase();
                case "birthplace"  -> p.getBirthPlace().toUpperCase();
                default            -> p.getMajor();
            };
            if (!table.containsKey(key)) {
                table.put(key, new majorObject(key));
            }
            table.get(key).addSalary(p.getSalary());
        }

        ArrayList<majorObject> sorted = table.getMajors(); // already sorted desc by avg salary

        String title = "Stats by " + field.substring(0, 1).toUpperCase() + field.substring(1)
                + "  (ranked by average salary)";
        printHeader(title);
        System.out.printf("%-22s  %-14s  %-14s  %-14s  %s%n",
                "Group", "Avg Salary", "Min", "Max", "People");
        System.out.println("-".repeat(80));
        for (majorObject g : sorted) {
            System.out.println(g);
        }

        // Overall winner
        if (!sorted.isEmpty()) {
            System.out.printf("%n#1 group: %s  (avg $%,.0f)%n",
                    sorted.get(0).major(), sorted.get(0).averageSalary());
        }
    }

    // ── Benchmarks ────────────────────────────────────────────────────────────

    private static void updateBenchmarks() {
        System.out.println("--- Update Benchmarks ---");
        System.out.printf("Current median wage: $%,.0f%n", MEDIAN_WAGE);
        MEDIAN_WAGE = promptDouble("New median wage ($)");
        System.out.printf("Current GDPC: $%,.0f%n", GDPC);
        GDPC = promptDouble("New GDPC ($)");
        System.out.printf("Benchmark updated to $%,.0f%n", (MEDIAN_WAGE + GDPC) / 2);
        recalculate();
    }

    // Rebuild all personObjects with the current benchmarks
    private static void recalculate() {
        ArrayList<personObject> updated = new ArrayList<>(people.size());
        for (personObject p : people) {
            updated.add(new personObject(p.getName(), p.getAge(), p.getSalary(), p.getGpa(),
                    p.getInstitution(), p.getBirthPlace(), p.getMajor(), p.getIndustry(),
                    MEDIAN_WAGE, GDPC));
        }
        people.clear();
        people.addAll(updated);
        System.out.println("Scores recalculated for " + people.size() + " people.");
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
            DataManager.LoadResult result = DataManager.load(path, MEDIAN_WAGE, GDPC);
            people.clear();
            people.addAll(result.people);
            // Apply benchmark overrides from file if present
            if (result.medianWage != MEDIAN_WAGE || result.gdpc != GDPC) {
                MEDIAN_WAGE = result.medianWage;
                GDPC        = result.gdpc;
                System.out.printf("Benchmarks from file: Median Wage = $%,.0f  |  GDPC = $%,.0f%n",
                        MEDIAN_WAGE, GDPC);
            }
            System.out.printf("Loaded %d people from %s%n", people.size(), path);
        } catch (IOException e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }

    private static void saveFile(String path) {
        try {
            DataManager.save(people, MEDIAN_WAGE, GDPC, path);
            System.out.printf("Saved %d people to %s%n", people.size(), path);
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
            try { return Integer.parseInt(in.nextLine().trim()); }
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

    private static boolean empty() {
        if (people.isEmpty()) {
            System.out.println("No data yet. Add a person or load a JSON file.");
            return true;
        }
        return false;
    }

    private static void printHeader(String title) {
        System.out.println("=== " + title + " ===");
    }

    private static void printSalaryStats(ArrayList<personObject> sorted) {
        double total = 0;
        int above = 0;
        for (personObject p : sorted) {
            total += p.getSalary();
            if (p.isSuccessful()) above++;
        }
        double avg = total / sorted.size();
        System.out.printf("%nGroup average salary: $%,.0f  |  Above median wage: %d / %d (%.0f%%)%n",
                avg, above, sorted.size(), 100.0 * above / sorted.size());
    }
}
