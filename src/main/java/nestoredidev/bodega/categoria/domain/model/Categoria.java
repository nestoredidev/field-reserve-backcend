package nestoredidev.bodega.categoria.domain.model;

import nestoredidev.bodega.categoria.domain.exception.InvalidCategoriaException;

public class Categoria {
    private final Long id;
    private final String nombre;

    private Categoria(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public static Categoria crear(String nombre) {
        return new Categoria(null, validarNombre(nombre));
    }

    public static Categoria restaurar(Long id, String nombre) {
        return new Categoria(id, nombre);
    }

    public Categoria renombrar(String nuevoNombre) {
        return new Categoria(id, validarNombre(nuevoNombre));
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidCategoriaException("RN-CAT-01: el nombre es obligatorio");
        }
        if (nombre.trim().length() > 60) {
            throw new InvalidCategoriaException("nombre no debe exceder 60 caracteres");
        }
        return nombre.trim();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
