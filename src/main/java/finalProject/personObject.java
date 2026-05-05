package finalProject;

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
        personObject otherPerson = (personObject) Other;
        return (int) this.getSalary() - (int) otherPerson.getSalary();
    }
    @Override
    //hashes them based on their major
    public int hashCode() {
        return Objects.hash(major);
    }
    //this is used to see if two people are equal
    public int getID() {
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
        personObject otherPerson = (personObject) other;
        if (this.getID() == otherPerson.getID()) {
            return true;
        }
        return false;
    }
} 