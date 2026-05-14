package com.universitydata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.universitydata.model.UniversityDataModels.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public LoadedData load(String filePath) throws IOException {
        JsonNode root = mapper.readTree(new File(filePath));
        LoadedData data = new LoadedData();

        for (JsonNode n : root.path("majors")) {
            MajorOutcomeSummary m = new MajorOutcomeSummary();
            m.cipCode                    = n.path("cipCode").asText(null);
            m.cipTitle                   = n.path("cipTitle").asText(null);
            m.credentialLevel            = n.path("credentialLevel").asInt(0);
            m.annualCompletions          = n.path("annualCompletions").asInt(0);
            m.scorecardMedianEarnings1Yr = n.path("medianEarnings1Yr").asInt(0);
            m.scorecardMedianEarnings4Yr = n.path("medianEarnings4Yr").asInt(0);
            List<String> socs = new ArrayList<>();
            for (JsonNode s : n.path("relatedSocCodes")) socs.add(s.asText());
            m.relatedSocCodes = socs;
            data.majors.add(m);
        }

        for (JsonNode n : root.path("occupations")) {
            OewsOccupation o = new OewsOccupation();
            o.socCode           = n.path("socCode").asText(null);
            o.title             = n.path("title").asText(null);
            o.annualMedianWage  = n.path("medianPay").asDouble(0);
            o.annualWage10thPct = n.path("wage10thPct").asDouble(0);
            o.annualWage25thPct = n.path("wage25thPct").asDouble(0);
            o.annualWage75thPct = n.path("wage75thPct").asDouble(0);
            o.annualWage90thPct = n.path("wage90thPct").asDouble(0);
            o.totalEmployed     = n.path("employmentCount").asLong(0);
            data.occupations.add(o);
        }

        return data;
    }

    public static class LoadedData {
        public List<MajorOutcomeSummary> majors     = new ArrayList<>();
        public List<OewsOccupation>      occupations = new ArrayList<>();
    }
}