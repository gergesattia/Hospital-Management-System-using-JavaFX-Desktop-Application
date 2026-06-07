package utils;

public class PasswordUtil {
    
    public static String hash(String plainPassword) {
        return plainPassword; // Hashing removed per request
    }
    
    public static boolean check(String plainPassword, String hashedPassword) {
        if (hashedPassword == null) return false;
        return hashedPassword.equals(plainPassword);
    }
}
