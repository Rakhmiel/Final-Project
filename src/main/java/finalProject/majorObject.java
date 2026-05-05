package finalProject;

import java.util.Objects;

public class majorObject implements Comparable<Object> {
    private final String major;
    private double totalSalary;
    private double averageSalary;
    private int timesAccessed = 0;
    private double min;
    private double max;

    public majorObject(String major) {
        this.major = major;
        this.totalSalary = 0;
        this.averageSalary = 0;
        this.max = 0;
        this.min = 0;
    }
    public void addSalary(double salary) {
        totalSalary += salary;
        timesAccessed++;
        averageSalary = totalSalary / timesAccessed;
        if (salary > max) {
            max = salary;
        }
        if (salary < min) {
            min = salary;
        }
    }
    public double totalSalary() {
        return totalSalary;
    }
    public double averageSalary() {
        return averageSalary;
    }
    @Override
    public String toString() {
        String data = String.format("Major: " + major + "Average Salary: " + averageSalary + " People: " + timesAccessed + " Min: " + min + " Max: " + max);
        return data;
    }
    public String major() {
        return this.major;
    }
    @Override
    //compares the salaries
    public int compareTo(Object Other) {
        majorObject otherMajor = (majorObject) Other;
        return (int) this.averageSalary() - (int) otherMajor.averageSalary();
    }
    @Override
    //hashes them based on their major
    public int hashCode() {
        return Objects.hash(major);
    }
    @Override
    //checks if two majors are equal
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (this == other) {
            return true;
        }
        majorObject otherMajor = (majorObject) other;
        if (this.major().equals(otherMajor.major())) {
            return true;
        }
        return false;
    }
}