import java.io.IOException;
import java.util.*;

import edu.yu.cs.com1320.project.*;

public class majorObject implements Comparable<Object> {
    private String major;
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
        totalsalary += salary;
        timesAccessed++;
        averageSalary = totalSalary / timesAccessed;
        if (salary > max) {
            max = salary;
        }
        if (salary < min) {
            min = salary;
        }
    }
    @Override
    public String toString() {
        String data = String.format("Major: " + major + "Average Salary: " + averageSalary);
        return data;
    }

    @Override
    //compares the salaries
    public int compareTo(Object Other) {
        return (int) this.getMajor() - (personObject) Other.getMajor();
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
        other = (majorObject) other;
        if (this.major() == other.major()) {
            return true;
        }
        return false;
    }
}