import java.util.Objects;

public class personObject implements Comparable<Object> {
    private double salary;
    private double medianWage;
    private double gdpc;
    private double success;
    private String major;
    private String name;
    private int uniqueID;

    public personObject(double salary, double medianWage, double gdpc, String major, String name) {
        this.salary = salary;
        this.medianWage = medianWage;
        this.gdpc = gdpc;
        this.major = major.strip().toUpperCase();
        this.success = calculateSuccess(this.salary, this.medianWage, this.gdpc);
        this.name = name;
        this.uniqueID = Objects.hash(salary, medianWage, gdpc, major, name);
    }
    private double calculateSuccess (double salary, double medianWage, double gdpc) {
        success = salary / ((medianWage * gdpc) * 10);
        return success;
    }
    public double getSalary() {
        return salary;
    }
    public String getMajor() {
        return major;
    }
    public double getSuccess() {
        return success;
    }
    public String getName() {
        return name;
    }
    @Override
    public int compareTo(Object Other) {
        return (int) this.getSalary() - (personObject) Other.getSalary();
    }
    @Override
    public int hashCode() {
        return Objects.hash(major);
    }
    private int getID() {
        return this.uniqueID;
    }
    @Override
    public boolean equals(Object other) {
        if (other !instanceof personObject) {
            return false;
        }
        if (this == other) {
            return true;
        }
        other = (personObject) other;
        if (this.getID() == other.getID()) {
            return true;
        }
        return false;
    }
} 