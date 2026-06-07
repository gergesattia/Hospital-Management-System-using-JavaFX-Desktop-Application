package utils;

import java.time.LocalDate;
import java.time.Period;

public class ValidationUtils {

    /**
     * Phone number must be exactly 11 digits and start with '01'
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("01\\d{9}");
    }

    /**
     * National ID (SSN) must be exactly 14 digits
     */
    public static boolean isValidSSN(String ssn) {
        return ssn != null && ssn.matches("\\d{14}");
    }

    /**
     * Email must end with @gmail.com, @yahoo.com, or @medicore.gov
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return true; // Optional field
        return email.matches("^[a-zA-Z0-9._%+-]+@(gmail\\.com|yahoo\\.com|medicore\\.gov)$");
    }

    /**
     * Age must be between 1 and 100 years
     */
    public static boolean isValidAge(LocalDate birthDate) {
        if (birthDate == null) return false;
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        return age >= 1 && age <= 100;
    }

    /**
     * Date must be today or in the future
     */
    public static boolean isFutureOrToday(LocalDate date) {
        if (date == null) return false;
        return !date.isBefore(LocalDate.now());
    }
}
