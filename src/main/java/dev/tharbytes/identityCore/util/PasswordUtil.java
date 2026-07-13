package dev.tharbytes.identityCore.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        if (!hashedPassword.startsWith("$2a$") &&
            !hashedPassword.startsWith("$2b$") &&
            !hashedPassword.startsWith("$2y$")) return false;
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
