package app;

import utils.PasswordUtil;

public class TestHash {
    public static void main(String[] args) {
        String hash = "$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDI//5RxnUpIG3vK";
        boolean match = PasswordUtil.check("admin123", hash);
        System.out.println("Match? " + match);
    }
}
