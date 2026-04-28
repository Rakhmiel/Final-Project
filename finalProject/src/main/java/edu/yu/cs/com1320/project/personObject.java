import java.util.*;
import edu.yu.cs.com1320.project.*;

public class personObject implements Comparable<Object> {
    private double salary;
    private double medianWage;
    private double gdpc;
    private double success;
    private String major;
    private String name;
    private int uniqueID;
    private majorObject totalCalculator;
    private static Set<String> majors = new HashSet<>();

    public personObject(double salary, double medianWage, double gdpc, String major, String name) {
        this.salary = salary;
        this.medianWage = medianWage;
        this.gdpc = gdpc;
        this.major = major.strip().toUpperCase();
        this.success = calculateSuccess(this.salary, this.medianWage, this.gdpc);
        this.name = name;
        this.totalCalculator = new majorObject(salary, major);
        if (majors.contains(major)) {
            majors.
        }
        //calculates the individual's unique ID used, used when comparing two people
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
    //compares the salaries
    public int compareTo(Object Other) {
        return (int) this.getSalary() - (personObject) Other.getSalary();
    }
    @Override
    //hashes them based on their major
    public int hashCode() {
        return Objects.hash(major);
    }
    //this is used to see if two people are equal
    private int getID() {
        return this.uniqueID;
    }
    @Override
    //checks if two personObjects are equal
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
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