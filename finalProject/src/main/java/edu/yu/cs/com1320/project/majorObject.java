import java.io.IOException;
import java.util.*;

import edu.yu.cs.com1320.project.*;

public class majorObject implements Comparable<Object> {
    private String major;
    private double totalSalary;
    private double averageSalary;
    private personObject[] = new personObject[];

    public majorObject(personObject person) {
        this.major = personObject.getMajor();
        this.totalSalary += personObject.getSalary();
    }
    public add(personObject person) throws IOException {
        if (person.getMajor() != major) {
            throw new IOException("Incorrect major.");
        }
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