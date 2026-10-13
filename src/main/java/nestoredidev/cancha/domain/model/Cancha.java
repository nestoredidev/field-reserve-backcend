package nestoredidev.cancha.domain.model;

import nestoredidev.cancha.domain.exception.InvalidCanchaException;

import java.time.OffsetDateTime;

public class Cancha {
    private final Long id;
    private final String nombre;
    private final String descripcion;
    private final boolean activa;
    private final OffsetDateTime createdAt;

    private Cancha(Long id, String nombre, String descripcion, boolean activa, OffsetDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = activa;
        this.createdAt = createdAt;
    }

    public static Cancha crear(String nombre, String descripcion) {
        return new Cancha(null, validarNombre(nombre), validarDescripcion(descripcion), true, null);
    }

    public static Cancha restaurar(Long id, String nombre, String descripcion, boolean activa,
                                   OffsetDateTime createdAt) {
        return new Cancha(id, nombre, descripcion, activa, createdAt);
    }

    public Cancha actualizar(String nombre, String descripcion) {
        return new Cancha(id, validarNombre(nombre), validarDescripcion(descripcion), activa, createdAt);
    }

    public Cancha conActiva(boolean nuevaActiva) {
        return new Cancha(id, nombre, descripcion, nuevaActiva, createdAt);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidCanchaException("RN-CAN-01: el nombre es obligatorio");
        }
        if (nombre.trim().length() > 80) {
            throw new InvalidCanchaException("nombre no debe exceder 80 caracteres");
        }
        return nombre.trim();
    }

    private static String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        if (descripcion.trim().length() > 255) {
            throw new InvalidCanchaException("descripcion no debe exceder 255 caracteres");
        }
        return descripcion.trim();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isActiva() { return activa; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
