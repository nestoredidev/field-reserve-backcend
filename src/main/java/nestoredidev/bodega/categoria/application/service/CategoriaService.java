package nestoredidev.bodega.categoria.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.bodega.categoria.application.ports.input.CategoriaServicePort;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaCommand;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaDto;
import nestoredidev.bodega.categoria.application.ports.output.CategoriaPersistencePort;
import nestoredidev.bodega.categoria.domain.exception.CategoriaNotFoundException;
import nestoredidev.bodega.categoria.domain.model.Categoria;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService implements CategoriaServicePort {
    private final CategoriaPersistencePort persistence;

    @Override
    @Transactional
    public CategoriaDto crear(CategoriaCommand command) {
        Categoria nueva = Categoria.crear(command.nombre());
        validarNombreUnico(nueva.getNombre(), null);
        return CategoriaDto.fromDomain(persistence.guardar(nueva));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaDto obtener(Long id) {
        return CategoriaDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CategoriaDto> listar(PageQuery query) {
        return persistence.listar(query).map(CategoriaDto::fromDomain);
    }

    @Override
    @Transactional
    public CategoriaDto actualizar(Long id, CategoriaCommand command) {
        Categoria actualizada = buscar(id).renombrar(command.nombre());
        validarNombreUnico(actualizada.getNombre(), id);
        return CategoriaDto.fromDomain(persistence.guardar(actualizada));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        if (persistence.tieneProductos(id)) {
            throw new DomainException("RN-CAT-02: no se elimina una categoría que tiene productos");
        }
        persistence.eliminar(id);
    }

    private Categoria buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new CategoriaNotFoundException(id));
    }

    private void validarNombreUnico(String nombre, Long excluirId) {
        if (persistence.existePorNombre(nombre, excluirId)) {
            throw new ConflictException("RN-CAT-01: ya existe la categoría '" + nombre + "'");
        }
    }
}
