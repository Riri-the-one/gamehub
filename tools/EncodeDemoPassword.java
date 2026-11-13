import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Internal helper for a randomly generated disposable demo password, never a production credential. */
class EncodeDemoPassword {
    public static void main(String[] args) {
        String password=System.getenv("GAMEHUB_DEMO_PASSWORD");
        if(password==null || !password.matches("[0-9a-f]{32}")) {
            throw new IllegalArgumentException("Only a disposable generated demo token is accepted.");
        }
        System.out.print(new BCryptPasswordEncoder(12).encode(password));
    }
}
