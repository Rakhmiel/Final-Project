package finalProject;

import java.util.Objects;

public class personObject implements Comparable<personObject> {
    private String name;
    private int age;
    private double salary;
    private double gpa;
    private String institution;
    private String birthPlace;
    private String major;
    private String industry;
    private double medianWage;
    private double gdpc;
    private double success;
    private int uniqueID;

    public personObject(String name, int age, double salary, double gpa,
                        String institution, String birthPlace,
                        String major, String industry,
                        double medianWage, double gdpc) {
        this.name = name;
        this.age = age;
        this.salary = salary;
        this.gpa = gpa;
        this.institution = institution.strip();
        this.birthPlace = birthPlace.strip();
        this.major = major.strip().toUpperCase();
        this.industry = industry.strip();
        this.medianWage = medianWage;
        this.gdpc = gdpc;
        this.success = calculateSuccess(salary, medianWage, gdpc);
        this.uniqueID = Objects.hash(name, age, salary, major);
    }

    // Score of 10 means salary == benchmark; above 10 is above average
    private double calculateSuccess(double salary, double medianWage, double gdpc) {
        double benchmark = (medianWage + gdpc) / 2.0;
        return (salary / benchmark) * 10.0;
    }

    public String getName() { 
        return name; 
    }
    public int getAge() { 
        return age; 
    }
    public double getSalary() { 
        return salary; 
    }
    public double getGpa() { 
        return gpa; 
    }
    public String getInstitution() { 
        return institution; 
    }
    public String getBirthPlace() { 
        return birthPlace; 
    }
    public String getMajor() { 
        return major; 
    }
    public String getIndustry() { 
        return industry; 
    }
    public double getMedianWage() { 
        return medianWage; 
    }
    public double getGdpc() { 
        return gdpc; 
    }
    public double getSuccess() { 
        return success; 
    }
    public int getID() { 
        return uniqueID; 
    }

    public boolean isSuccessful() {
        return salary > medianWage;
    }

    @Override
    public int compareTo(personObject other) {
        return Double.compare(this.salary, other.salary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        if (this == other) return true;
        return this.uniqueID == ((personObject) other).uniqueID;
    }

    @Override
    public String toString() {
        return String.format("%-20s | Age: %3d | Salary: $%,10.0f | GPA: %.2f | Major: %-18s | Industry: %-15s | Born: %-12s | Institution: %s | Score: %.1f%s",
                name, age, salary, gpa, major, industry, birthPlace, institution, success,
                isSuccessful() ? " *" : "");
    }
}
