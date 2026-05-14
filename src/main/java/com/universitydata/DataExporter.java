package com.universitydata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.universitydata.model.UniversityDataModels.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DataExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    public void export(List<MajorOutcomeSummary> majors,
                       List<OewsOccupation> occupations,
                       List<OohOccupation> oohList,
                       Map<String, OnetCrosswalkEntry> brightOutlookMap,
                       String outputPath) throws IOException {

        ObjectNode root = mapper.createObjectNode();
        ArrayNode majorsArray = mapper.createArrayNode();
        ArrayNode occsArray   = mapper.createArrayNode();

        for (MajorOutcomeSummary m : majors) {
            ObjectNode node = mapper.createObjectNode();
            node.put("cipCode",           m.cipCode != null ? m.cipCode : "");
            node.put("cipTitle",          m.cipTitle != null ? m.cipTitle : "");
            node.put("credentialLevel",   m.credentialLevel != null ? m.credentialLevel : 0);
            node.put("annualCompletions", m.annualCompletions != null ? m.annualCompletions : 0);
            node.put("medianEarnings1Yr", m.scorecardMedianEarnings1Yr != null ? m.scorecardMedianEarnings1Yr : 0);
            node.put("medianEarnings4Yr", m.scorecardMedianEarnings4Yr != null ? m.scorecardMedianEarnings4Yr : 0);

            ArrayNode socArray = mapper.createArrayNode();
            if (m.relatedSocCodes != null) m.relatedSocCodes.forEach(socArray::add);
            node.set("relatedSocCodes", socArray);
            majorsArray.add(node);
        }

        for (OewsOccupation o : occupations) {
            ObjectNode node = mapper.createObjectNode();
            node.put("socCode",         o.socCode != null ? o.socCode : "");
            node.put("title",           o.title != null ? o.title : "");
            node.put("wage10thPct",     o.annualWage10thPct != null ? o.annualWage10thPct : 0);
            node.put("wage25thPct",     o.annualWage25thPct != null ? o.annualWage25thPct : 0);
            node.put("wage75thPct",     o.annualWage75thPct != null ? o.annualWage75thPct : 0);
            node.put("wage90thPct",     o.annualWage90thPct != null ? o.annualWage90thPct : 0);
            node.put("employmentCount", o.totalEmployed != null ? o.totalEmployed : 0);

            OohOccupation ooh = oohList.stream()
                    .filter(h -> h.title != null && o.title != null)
                    .filter(h -> h.medianAnnualPay != null)
                    .filter(h -> {
                        String oohTitle = h.title.toLowerCase();
                        String occTitle = o.title.toLowerCase();
                        for (String word : occTitle.split(" ")) {
                            if (word.length() > 4 && oohTitle.contains(word)) return true;
                        }
                        return false;
                    })
                    .findFirst().orElse(null);

            node.put("medianPay",           ooh != null && ooh.medianAnnualPay != null ? ooh.medianAnnualPay : 0);
            node.put("jobGrowthRate10Yr",   ooh != null && ooh.jobGrowthRate10Yr != null ? ooh.jobGrowthRate10Yr : 0);
            node.put("projectedOpenings",   ooh != null && ooh.projectedOpenings10Yr != null ? ooh.projectedOpenings10Yr : 0);
            node.put("entryLevelEducation", ooh != null && ooh.entryLevelEducation != null ? ooh.entryLevelEducation : "");
            node.put("brightOutlook",       brightOutlookMap != null && brightOutlookMap.containsKey(o.socCode));

            occsArray.add(node);
        }

        root.set("majors", majorsArray);
        root.set("occupations", occsArray);

        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), root);
        System.out.println("Exported to: " + outputPath);
    }
}
