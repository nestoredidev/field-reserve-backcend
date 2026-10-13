package nestoredidev.usuario.application.ports.output;

public interface PasswordHasherPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
