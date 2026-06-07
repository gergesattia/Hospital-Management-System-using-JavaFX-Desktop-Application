package utils;

public class Session {
    private static Session instance;
    private int userId;
    private String fullName;
    private String role;
    private Integer doctorId;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void set(int userId, String fullName, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
    }

    public void clear() {
        userId = 0;
        fullName = null;
        role = null;
        doctorId = null;
    }

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }

    public Integer getDoctorId() { return doctorId; }
    public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }
}
