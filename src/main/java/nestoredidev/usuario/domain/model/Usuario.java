package nestoredidev.usuario.domain.model;

import nestoredidev.common.domain.Rol;
import nestoredidev.usuario.domain.exception.InvalidUsuarioException;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public class Usuario {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{4,50}$");
    public static final int MIN_PASSWORD = 8;

    private final Long id;
    private final String nombre;
    private final String username;
    private final String passwordHash;
    private final Rol rol;
    private final boolean activo;
    private final OffsetDateTime createdAt;

    private Usuario(Long id, String nombre, String username, String passwordHash, Rol rol, boolean activo,
                    OffsetDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
        this.createdAt = createdAt;
    }

    /** Nuevo usuario: valida RN-USU-01 (la contraseña ya llega codificada). */
    public static Usuario crear(String nombre, String username, String passwordHash, Rol rol) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new InvalidUsuarioException("nombre es obligatorio (máximo 100 caracteres)");
        }
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new InvalidUsuarioException(
                    "RN-USU-01: username de 4 a 50 caracteres, en minúsculas y sin espacios");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidUsuarioException("password es obligatorio");
        }
        if (rol == null) {
            throw new InvalidUsuarioException("rol es obligatorio");
        }
        return new Usuario(null, nombre.trim(), username, passwordHash, rol, true, null);
    }

    public static Usuario restaurar(Long id, String nombre, String username, String passwordHash, Rol rol,
                                    boolean activo, OffsetDateTime createdAt) {
        return new Usuario(id, nombre, username, passwordHash, rol, activo, createdAt);
    }

    public static void validarPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new InvalidUsuarioException("RN-USU-02: la contraseña tiene mínimo " + MIN_PASSWORD + " caracteres");
        }
    }

    public Usuario actualizar(String nombre, Rol rol) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new InvalidUsuarioException("nombre es obligatorio (máximo 100 caracteres)");
        }
        if (rol == null) {
            throw new InvalidUsuarioException("rol es obligatorio");
        }
        return new Usuario(id, nombre.trim(), username, passwordHash, rol, activo, createdAt);
    }

    public Usuario conPassword(String nuevoHash) {
        return new Usuario(id, nombre, username, nuevoHash, rol, activo, createdAt);
    }

    public Usuario conActivo(boolean nuevoActivo) {
        return new Usuario(id, nombre, username, passwordHash, rol, nuevoActivo, createdAt);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
