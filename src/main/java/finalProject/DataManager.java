package finalProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load a JSON file. Accepts two formats:
     *   - Wrapped:  { "medianWage": ..., "gdpc": ..., "people": [ {...}, ... ] }
     *   - Array:    [ { "name": ..., ... }, ... ]
     *
     * Returns a LoadResult containing the person list and optional benchmark overrides.
     */
    public static LoadResult load(String filePath, double defaultMedianWage, double defaultGdpc)
            throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            JsonElement root = JsonParser.parseReader(reader);

            double medianWage = defaultMedianWage;
            double gdpc       = defaultGdpc;
            JsonArray jsonPeople;

            if (root.isJsonArray()) {
                // Plain array of person records
                jsonPeople = root.getAsJsonArray();
            } else {
                // Wrapped object
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("medianWage")) medianWage = obj.get("medianWage").getAsDouble();
                if (obj.has("gdpc"))       gdpc       = obj.get("gdpc").getAsDouble();
                jsonPeople = obj.has("people") ? obj.getAsJsonArray("people") : new JsonArray();
            }

            ArrayList<personObject> people = new ArrayList<>();
            for (JsonElement el : jsonPeople) {
                PersonRecord rec = GSON.fromJson(el, PersonRecord.class);
                people.add(rec.toPerson(medianWage, gdpc));
            }

            return new LoadResult(people, medianWage, gdpc);
        }
    }

    /**
     * Save the dataset in the clean wrapped format:
     * { "medianWage": ..., "gdpc": ..., "people": [ {...}, ... ] }
     */
    public static void save(ArrayList<personObject> people, double medianWage, double gdpc,
                            String filePath) throws IOException {
        DataFile file = new DataFile();
        file.medianWage = medianWage;
        file.gdpc       = gdpc;
        for (personObject p : people) {
            file.people.add(new PersonRecord(p));
        }
        try (Writer writer = new FileWriter(filePath)) {
            GSON.toJson(file, writer);
        }
    }

    // ── Result container ─────────────────────────────────────────────────────

    public static class LoadResult {
        public final ArrayList<personObject> people;
        public final double medianWage;
        public final double gdpc;

        LoadResult(ArrayList<personObject> people, double medianWage, double gdpc) {
            this.people     = people;
            this.medianWage = medianWage;
            this.gdpc       = gdpc;
        }
    }
}
