package nestoredidev.bodega.proveedor.domain.model;

import nestoredidev.bodega.proveedor.domain.exception.InvalidProveedorException;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public class Proveedor {
    private static final Pattern RUC = Pattern.compile("^[0-9]{11}$");

    private final Long id;
    private final String nombre;
    private final String ruc;
    private final String telefono;
    private final boolean activo;
    private final OffsetDateTime createdAt;

    private Proveedor(Long id, String nombre, String ruc, String telefono, boolean activo, OffsetDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.ruc = ruc;
        this.telefono = telefono;
        this.activo = activo;
        this.createdAt = createdAt;
    }

    public static Proveedor crear(String nombre, String ruc, String telefono) {
        return new Proveedor(null, validarNombre(nombre), validarRuc(ruc), validarTelefono(telefono), true, null);
    }

    public static Proveedor restaurar(Long id, String nombre, String ruc, String telefono, boolean activo,
                                      OffsetDateTime createdAt) {
        return new Proveedor(id, nombre, ruc, telefono, activo, createdAt);
    }

    public Proveedor actualizar(String nombre, String ruc, String telefono) {
        return new Proveedor(id, validarNombre(nombre), validarRuc(ruc), validarTelefono(telefono), activo, createdAt);
    }

    public Proveedor conActivo(boolean nuevoActivo) {
        return new Proveedor(id, nombre, ruc, telefono, nuevoActivo, createdAt);
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidProveedorException("RN-PRV-01: el nombre es obligatorio");
        }
        if (nombre.trim().length() > 120) {
            throw new InvalidProveedorException("nombre no debe exceder 120 caracteres");
        }
        return nombre.trim();
    }

    private static String validarRuc(String ruc) {
        if (ruc == null || ruc.isBlank()) {
            return null;
        }
        if (!RUC.matcher(ruc.trim()).matches()) {
            throw new InvalidProveedorException("RN-PRV-01: el RUC debe tener 11 dígitos");
        }
        return ruc.trim();
    }

    private static String validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        if (telefono.trim().length() > 15) {
            throw new InvalidProveedorException("telefono no debe exceder 15 caracteres");
        }
        return telefono.trim();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRuc() { return ruc; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
