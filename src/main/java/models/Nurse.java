package models;

public class Nurse extends Person {
    private int nurseId;
    private int doctorId;
    private String email;
    private String ssn;
    private String birthDate;
    private String shift;
    private String department;
    private int experienceYears;
    private String licenseNumber;
    private double salary;
    private boolean isActive;

    public Nurse() {}

    public int getNurseId() { return nurseId; }
    public void setNurseId(int nurseId) { this.nurseId = nurseId; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
