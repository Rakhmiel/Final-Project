import java.util.*;
import edu.yu.cs.com1320.project.*;
public class majorObject implements Comparable<Object> {
    private String major;
    private double totalSalary;
    private double averageSalary;
    private personObject[] = new personObject[];

    public majorObject(Double salary, String major) {
        this.major = major;
        this.totalSalary += salary;
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
}