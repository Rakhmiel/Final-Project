package finalProject;

/** Flat POJO used for JSON input and output — no computed fields. */
public class PersonRecord {
    public String name        = "";
    public int    age         = 0;
    public double salary      = 0;
    public double gpa         = 0;
    public String institution = "";
    public String birthPlace  = "";
    public String major       = "";
    public String industry    = "";

    public PersonRecord() {}

    public PersonRecord(personObject p) {
        this.name        = p.getName();
        this.age         = p.getAge();
        this.salary      = p.getSalary();
        this.gpa         = p.getGpa();
        this.institution = p.getInstitution();
        this.birthPlace  = p.getBirthPlace();
        this.major       = p.getMajor();
        this.industry    = p.getIndustry();
    }

    public personObject toPerson(double medianWage, double gdpc) {
        return new personObject(name, age, salary, gpa,
                institution, birthPlace, major, industry,
                medianWage, gdpc);
    }
}
