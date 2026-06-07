package models;

import java.time.LocalDate;

public class Patient extends Person {
    private String nationalId;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String bookedSpecialization;
    private int priorityLevel;
    private Long telegramChatId;

    public Patient() {}

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBookedSpecialization() { return bookedSpecialization; }
    public void setBookedSpecialization(String bookedSpecialization) { this.bookedSpecialization = bookedSpecialization; }

    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int priorityLevel) { this.priorityLevel = priorityLevel; }

    public Long getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(Long telegramChatId) { this.telegramChatId = telegramChatId; }
}
