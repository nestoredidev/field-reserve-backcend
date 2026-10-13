package nestoredidev.usuario.infrastructure.security;

import lombok.RequiredArgsConstructor;
import nestoredidev.usuario.infrastructure.adapter.output.persistence.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** bd.sql inserta el admin con un hash de relleno; aquí se reemplaza por la contraseña configurada. */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private static final String PLACEHOLDER = "$2a$10$REEMPLAZAR";

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        repository.findByUsername("admin")
                .filter(u -> u.getPasswordHash().startsWith(PLACEHOLDER))
                .ifPresent(u -> {
                    u.setPasswordHash(encoder.encode(adminPassword));
                    repository.save(u);
                });
    }
}
