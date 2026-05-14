package com.universitydata;

import com.universitydata.model.UniversityDataModels.OewsOccupation;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class OewsParser {

    private static final String RECONSTRUCTED_FILE = "oe.data.1.AllData";

    public static Map<String, OewsOccupation> parse(String pathStr) throws IOException {

        Path path = Paths.get(pathStr).toAbsolutePath();
        Path dir;

        if (Files.isDirectory(path)) {
            dir = path;
        } else {
            // If it's a file, use its parent folder
            dir = path.getParent();
        }

        System.out.println("OEWS directory resolved to: " + dir);

        Path reconstructedFile = reconstructFile(dir);

        System.out.println("Reconstructed file size: " + Files.size(reconstructedFile));

        Map<String, OewsOccupation> result = parseFile(reconstructedFile);

        System.out.println("OEWS map size: " + result.size());

        splitFile(reconstructedFile);

        return result;
    }

    private static Path reconstructFile(Path dir) throws IOException {

        Path output = dir.resolve(RECONSTRUCTED_FILE);

        List<Path> parts = Files.list(dir)
                .filter(p -> p.getFileName().toString().startsWith("oe.data.part."))
                .sorted()
                .collect(Collectors.toList());

        System.out.println("Found OEWS parts: " + parts.size());

        if (parts.isEmpty()) {
            throw new IllegalStateException(
                    "No OEWS split files found in: " + dir.toAbsolutePath()
            );
        }

        try (OutputStream out = Files.newOutputStream(
                output,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {

            byte[] buffer = new byte[8192];

            for (Path part : parts) {

                System.out.println("Adding chunk: " + part.getFileName());

                try (InputStream in = Files.newInputStream(part)) {

                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }
        }

        return output;
    }

    private static Map<String, OewsOccupation> parseFile(Path file) throws IOException {

        Map<String, OewsOccupation> result = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {

            String line;
            boolean firstLine = true;
            int rowCount = 0;

            while ((line = reader.readLine()) != null) {

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                if (line.isBlank()) continue;

                String[] parts = line.split("\t");
                if (parts.length < 4) continue;

                String seriesId = parts[0].trim();
                String valueStr = parts[3].trim();

                if (rowCount < 5) {
                    System.out.println("Sample row: " + line);
                }

                rowCount++;

                if (valueStr.equals("-") || valueStr.isBlank()) continue;
                if (seriesId.length() < 25) continue;

                String area = seriesId.substring(4, 11);
                if (!area.equals("0000000")) continue;

                String socRaw = seriesId.substring(17, 23);
                if (socRaw.equals("000000")) continue;

                String dataType = "0" + seriesId.substring(23, 25);

                String socCode =
                        socRaw.substring(0, 2) + "-" + socRaw.substring(2) + ".00";

                double value;
                try {
                    value = Double.parseDouble(valueStr);
                } catch (NumberFormatException e) {
                    continue;
                }

                OewsOccupation occ = result.computeIfAbsent(socCode, k -> {
                    OewsOccupation o = new OewsOccupation();
                    o.socCode = k;
                    o.area = "National";
                    return o;
                });

                switch (dataType) {
                    case "001" -> occ.totalEmployed = (long) value;
                    case "008" -> occ.annualMeanWage = value;
                    case "011" -> occ.annualWage10thPct = value;
                    case "012" -> occ.annualWage25thPct = value;
                    case "013" -> occ.annualMedianWage = value;
                    case "014" -> occ.annualWage75thPct = value;
                    case "015" -> occ.annualWage90thPct = value;
                }
            }

            System.out.println("Rows read: " + rowCount);
        }

        return result;
    }

    private static void splitFile(Path file) throws IOException {

        System.out.println("Re-splitting file...");

        final long CHUNK_SIZE = 95L * 1024L * 1024L;

        byte[] buffer = new byte[8192];

        try (InputStream in = Files.newInputStream(file)) {

            int partIndex = 0;
            long bytesWritten = 0;

            OutputStream out = createPart(file.getParent(), partIndex);

            try {

                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {

                    if (bytesWritten + bytesRead > CHUNK_SIZE) {

                        int toWrite = (int) (CHUNK_SIZE - bytesWritten);
                        if (toWrite > 0) {
                            out.write(buffer, 0, toWrite);
                        }

                        out.close();

                        partIndex++;
                        out = createPart(file.getParent(), partIndex);

                        int remaining = bytesRead - toWrite;
                        if (remaining > 0) {
                            out.write(buffer, toWrite, remaining);
                        }

                        bytesWritten = remaining;

                    } else {

                        out.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                    }
                }

            } finally {
                out.close();
            }
        }

        System.out.println("Re-splitting complete");
    }

    private static OutputStream createPart(Path dir, int index) throws IOException {

        char a = (char) ('a' + (index / 26));
        char b = (char) ('a' + (index % 26));

        String name = "oe.data.part." + a + b;

        return Files.newOutputStream(
                dir.resolve(name),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
