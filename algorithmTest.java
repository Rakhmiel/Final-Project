// this is the algorith tester 
//
public class algorithmTest {
    double salary;
    double medianWage;
    double gdpc;
    double success;
    public static void main (String[] args) {
        salary = args[0];
        medianWage = args[1];
        gdpc = args[2];
        success = calculateSuccess(this.salary, this.medianWage, this.gdpc)
    }
    private double calculateSuccess (double salary, double medianWage, double gdpc) {
        success = salary / ((medianWage * gdpc) * 10);
        return success
    }
}