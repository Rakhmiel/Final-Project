import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class govApi {

    // -------------------------
    // HTTP REQUEST
    // -------------------------
    public static String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );

        String line;
        StringBuilder sb = new StringBuilder();

        while ((line = in.readLine()) != null) {
            sb.append(line);
        }

        in.close();
        conn.disconnect();

        return sb.toString();
    }

    // -------------------------
    // VERY SIMPLE JSON EXTRACTOR
    // -------------------------
    public static String extract(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();

        int end1 = json.indexOf(",", start);
        int end2 = json.indexOf("}", start);

        int end;
        if (end1 == -1) end = end2;
        else if (end2 == -1) end = end1;
        else end = Math.min(end1, end2);

        return json.substring(start, end).replaceAll("[\" ]", "").trim();
    }

    // -------------------------
    // BLS JOB MARKET LAYER
    // -------------------------
    public static HashMap<String, Integer> getBLSMap() {
        HashMap<String, Integer> map = new HashMap<>();

        map.put("Software Developer", 127260);
        map.put("Data Scientist", 108020);
        map.put("Financial Analyst", 99890);
        map.put("Accountant", 79100);
        map.put("Registered Nurse", 86110);
        map.put("Marketing Manager", 140040);
        map.put("Lawyer", 145760);

        return map;
    }

    // -------------------------
    // MAJOR → JOB MAPPING (SIMULATED PSEO LAYER)
    // -------------------------
    public static HashMap<String, String[]> getMajorMap() {

        HashMap<String, String[]> map = new HashMap<>();

        map.put("Computer Science",
                new String[]{"Software Developer", "Data Scientist"});

        map.put("Finance",
                new String[]{"Financial Analyst", "Accountant"});

        map.put("Biology",
                new String[]{"Lab Technician", "Registered Nurse"});

        map.put("Business",
                new String[]{"Marketing Manager", "Financial Analyst"});

        return map;
    }

    // -------------------------
    // MAIN
    // -------------------------
    public static void main(String[] args) throws Exception {

        String apiKey = "UErh64HhHezs7idMZ9uYbhqgDAQif6cpgjcUISvz";

        // =====================================================
        // 1. SCORECARD DATA
        // =====================================================
        String url =
                "https://api.data.gov/ed/collegescorecard/v1/schools"
                + "?api_key=" + apiKey
                + "&id=197708"
                + "&fields="
                + "school.name,"
                + "school.city,"
                + "school.state,"
                + "latest.student.size,"
                + "latest.admissions.admission_rate.overall,"
                + "latest.earnings.6_yrs_after_entry.median";

        String json = fetch(url);

        String name = extract(json, "school.name");
        String city = extract(json, "school.city");
        String state = extract(json, "school.state");

        String size = extract(json, "latest.student.size");
        String acceptance = extract(json, "latest.admissions.admission_rate.overall");
        String earnings = extract(json, "latest.earnings.6_yrs_after_entry.median");

        // =====================================================
        // 2. BLS DATA
        // =====================================================
        HashMap<String, Integer> bls = getBLSMap();

        // =====================================================
        // 3. MAJOR → JOB MODEL
        // =====================================================
        HashMap<String, String[]> majors = getMajorMap();

        // =====================================================
        // 4. OUTPUT
        // =====================================================
        System.out.println("{");

        System.out.println("  \"school\": {");
        System.out.println("    \"name\": \"" + name + "\",");
        System.out.println("    \"city\": \"" + city + "\",");
        System.out.println("    \"state\": \"" + state + "\",");
        System.out.println("    \"size\": " + size);
        System.out.println("  },");

        System.out.println("  \"admissions\": {");
        System.out.println("    \"acceptance_rate\": " + acceptance);
        System.out.println("  },");

        System.out.println("  \"outcomes\": {");
        System.out.println("    \"earnings_6yr\": " + earnings);
        System.out.println("  },");

        // =====================================================
        // MAJOR → JOB → SALARY PIPELINE
        // =====================================================
        System.out.println("  \"majors\": {");

        int mIndex = 0;
        for (String major : majors.keySet()) {

            System.out.println("    \"" + major + "\": {");
            System.out.println("      \"jobs\": {");

            String[] jobs = majors.get(major);

            for (int i = 0; i < jobs.length; i++) {

                String job = jobs[i];
                Integer salary = bls.get(job);

                System.out.println("        \"" + job + "\": " + salary);

                if (i < jobs.length - 1)
                    System.out.println(",");
            }

            System.out.println("      }");
            System.out.print("    }");

            if (mIndex < majors.size() - 1)
                System.out.println(",");
            else
                System.out.println();

            mIndex++;
        }

        System.out.println("  }");

        System.out.println("}");
    }
}
