package finalProject;

import java.util.Objects;

public class majorObject implements Comparable<majorObject> {
    private final String major;
    private double totalSalary;
    private double averageSalary;
    private int count;
    private double min;
    private double max;

    public majorObject(String major) {
        this.major = major.strip().toUpperCase();
        this.totalSalary = 0;
        this.averageSalary = 0;
        this.count = 0;
        this.min = Double.MAX_VALUE;
        this.max = 0;
    }

    public void addSalary(double salary) {
        totalSalary += salary;
        count++;
        averageSalary = totalSalary / count;
        if (salary > max) max = salary;
        if (salary < min) min = salary;
    }

    public double totalSalary() { 
        return totalSalary; 
    }
    public double averageSalary() { 
        return averageSalary; 
    }
    public int count() { 
        return count; 
    }
    public double min() { 
        return count == 0 ? 0 : min;
    }
    public double max() { 
        return max;
    }
    public String major() { 
        return major;
    }

    @Override
    public int compareTo(majorObject other) {
        return Double.compare(this.averageSalary, other.averageSalary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        if (this == other) return true;
        return this.major.equals(((majorObject) other).major);
    }

    @Override
    public String toString() {
        return String.format("%-20s | Avg: $%,10.0f | Min: $%,10.0f | Max: $%,10.0f | People: %d",
                major, averageSalary, min(), max, count);
    }
}
