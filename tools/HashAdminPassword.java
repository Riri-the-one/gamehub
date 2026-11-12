import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local interactive utility. Never accepts a password on the command line. */
class HashAdminPassword {
    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run from an interactive terminal; piped input is refused.");
        }
        char[] password = console.readPassword("New administrator password: ");
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation)
                    || password.length < 12 || new String(password).getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Use matching passwords: at least 12 characters and at most 72 UTF-8 bytes.");
            }
            console.printf("BCrypt hash (keep local): %s%n", new BCryptPasswordEncoder(12).encode(new String(password)));
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
