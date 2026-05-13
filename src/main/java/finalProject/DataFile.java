package finalProject;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level JSON structure.
 * medianWage and gdpc are optional — null means "use app defaults".
 */
public class DataFile {
    public Double medianWage = null;
    public Double gdpc       = null;
    public List<PersonRecord> people = new ArrayList<>();
}
