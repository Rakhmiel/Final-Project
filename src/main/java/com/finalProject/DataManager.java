package com.finalProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Load a JSON file in the format:
     *   { "majors": [...], "occupations": [...] }
     */
    public static LoadResult load(String filePath) throws IOException {
        try (Reader reader = new FileReader(filePath)) {
            JsonElement root = JsonParser.parseReader(reader);
            List<MajorRecord>      majors      = new ArrayList<>();
            List<OccupationRecord> occupations = new ArrayList<>();

            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                Type majorListType = new TypeToken<List<MajorRecord>>(){}.getType();
                Type occListType   = new TypeToken<List<OccupationRecord>>(){}.getType();
                if (obj.has("majors"))
                    majors = GSON.fromJson(obj.get("majors"), majorListType);
                if (obj.has("occupations"))
                    occupations = GSON.fromJson(obj.get("occupations"), occListType);
            }

            return new LoadResult(majors, occupations);
        }
    }

    /**
     * Save majors and occupations to a JSON file.
     */
    public static void save(List<MajorRecord> majors, List<OccupationRecord> occupations,
                            String filePath) throws IOException {
        DataFile file = new DataFile();
        file.majors      = majors;
        file.occupations = occupations;
        try (Writer writer = new FileWriter(filePath)) {
            GSON.toJson(file, writer);
        }
    }

    public static class LoadResult {
        public final List<MajorRecord>      majors;
        public final List<OccupationRecord> occupations;

        LoadResult(List<MajorRecord> majors, List<OccupationRecord> occupations) {
            this.majors      = majors;
            this.occupations = occupations;
        }
    }
}
