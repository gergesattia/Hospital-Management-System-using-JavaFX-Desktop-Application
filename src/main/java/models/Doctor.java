package models;

public class Doctor extends User {
    private int doctorId;
    private int specializationId;
    private String roomNumber;
    private double consultationFee;
    
    // For joining with specializations table in views
    private String specializationName;

    public Doctor() {}

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public int getSpecializationId() { return specializationId; }
    public void setSpecializationId(int specializationId) { this.specializationId = specializationId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public double getConsultationFee() { return consultationFee; }
    public void setConsultationFee(double consultationFee) { this.consultationFee = consultationFee; }

    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }
}
