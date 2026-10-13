package nestoredidev.cliente.domain.model;

import nestoredidev.cliente.domain.exception.InvalidClienteException;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public class Cliente {
    private static final Pattern CELULAR_PE = Pattern.compile("^9\\d{8}$");

    private final Long id;
    private final String nombre;
    private final String telefono;
    private final String notas;
    private final OffsetDateTime createdAt;

    private Cliente(Long id, String nombre, String telefono, String notas, OffsetDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.notas = notas;
        this.createdAt = createdAt;
    }

    public static Cliente crear(String nombre, String telefono, String notas) {
        return new Cliente(null, validarNombre(nombre), validarTelefono(telefono), validarNotas(notas), null);
    }

    public static Cliente restaurar(Long id, String nombre, String telefono, String notas, OffsetDateTime createdAt) {
        return new Cliente(id, nombre, telefono, notas, createdAt);
    }

    public Cliente actualizar(String nombre, String telefono, String notas) {
        return new Cliente(id, validarNombre(nombre), validarTelefono(telefono), validarNotas(notas), createdAt);
    }

    /** RN-CLI-01: celular peruano de 9 dígitos que empieza con 9. */
    public static String validarTelefono(String telefono) {
        if (telefono == null || !CELULAR_PE.matcher(telefono.trim()).matches()) {
            throw new InvalidClienteException("RN-CLI-01: el teléfono debe ser un celular peruano de 9 dígitos que empieza con 9");
        }
        return telefono.trim();
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidClienteException("RN-CLI-01: el nombre es obligatorio");
        }
        if (nombre.trim().length() > 100) {
            throw new InvalidClienteException("nombre no debe exceder 100 caracteres");
        }
        return nombre.trim();
    }

    private static String validarNotas(String notas) {
        if (notas == null || notas.isBlank()) {
            return null;
        }
        if (notas.trim().length() > 255) {
            throw new InvalidClienteException("notas no debe exceder 255 caracteres");
        }
        return notas.trim();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getNotas() { return notas; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
