public class personObject {
    private double salary;
    private double medianWage;
    private double gdpc;
    private double success;
    private String major;

    public personObject(double salary, double medianWage, double gdpc, String major) {
        this.salary = salary;
        this.medianWage = medianWage;
        this.gdpc = gdpc;
        this.major = 
        this.success = calculateSuccess(this.salary, this.medianWage, this.gdpc);
    }
    private double calculateSuccess (double salary, double medianWage, double gdpc) {
        success = salary / ((medianWage * gdpc) * 10);
        return success
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
}