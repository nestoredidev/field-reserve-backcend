package nestoredidev.bodega.categoria.application.ports.input.dto;

import nestoredidev.bodega.categoria.domain.model.Categoria;

public record CategoriaDto(Long id, String nombre) {
    public static CategoriaDto fromDomain(Categoria c) {
        return new CategoriaDto(c.getId(), c.getNombre());
    }
}
