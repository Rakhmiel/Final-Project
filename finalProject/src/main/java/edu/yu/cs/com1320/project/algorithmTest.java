// this is the algorith tester 
//
import edu.yu.cs.com1320.project.*;
import java.util.*;

public class algorithmTest {
    private List<personObject> people = new ArrayList<>();
    private HashMap<String, personObject> dataMap = new HashMap<>();

    public static void main (String[] args) {
        double salary = args[0];
        double medianWage = args[1];
        double gdpc = args[2];
        String major = args[3];
        int i = 0;
        while (i < args.length) {
            int j = 0;

        }
    }
    private addToHash() {
        for (person : people) {
            dataMap.put(person.hashCode(), person);
        }
    }
    private calculateData() {
        for (major : dataMap) {
            double totalSalary = 0;
            for (person : major) {
                totalSalary += person.getSalary();
            }
        }
    }
}