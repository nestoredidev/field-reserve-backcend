package nestoredidev.bodega.categoria.application.service;

import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaCommand;
import nestoredidev.bodega.categoria.application.ports.input.dto.CategoriaDto;
import nestoredidev.bodega.categoria.application.ports.output.CategoriaPersistencePort;
import nestoredidev.bodega.categoria.domain.exception.CategoriaNotFoundException;
import nestoredidev.bodega.categoria.domain.exception.InvalidCategoriaException;
import nestoredidev.bodega.categoria.domain.model.Categoria;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaPersistencePort persistence;

    @InjectMocks
    private CategoriaService service;

    @Test
    void crear_valida_guardaConNombreRecortado() {
        when(persistence.existePorNombre("Bebidas", null)).thenReturn(false);
        when(persistence.guardar(any(Categoria.class))).thenAnswer(inv ->
                Categoria.restaurar(1L, ((Categoria) inv.getArgument(0)).getNombre()));

        CategoriaDto dto = service.crear(new CategoriaCommand("  Bebidas "));

        assertThat(dto).isEqualTo(new CategoriaDto(1L, "Bebidas"));
    }

    @Test
    void crear_nombreDuplicado_lanzaConflictRNCAT01() {
        when(persistence.existePorNombre("Bebidas", null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CategoriaCommand("Bebidas")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-CAT-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_nombreVacio_lanzaInvalidCategoria() {
        assertThatThrownBy(() -> service.crear(new CategoriaCommand(" ")))
                .isInstanceOf(InvalidCategoriaException.class);
    }

    @Test
    void crear_nombreMayorA60_lanzaInvalidCategoria() {
        assertThatThrownBy(() -> service.crear(new CategoriaCommand("x".repeat(61))))
                .isInstanceOf(InvalidCategoriaException.class);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(CategoriaNotFoundException.class);
    }

    @Test
    void listar_mapeaResultados() {
        PageQuery q = new PageQuery(0, 10, "nombre", "asc");
        when(persistence.listar(q)).thenReturn(new PageResult<>(List.of(Categoria.restaurar(1L, "A")), 0, 10, 1, 1));

        assertThat(service.listar(q).content()).containsExactly(new CategoriaDto(1L, "A"));
    }

    @Test
    void actualizar_valida_excluyeElPropioIdEnLaUnicidad() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(Categoria.restaurar(1L, "Viejo")));
        when(persistence.existePorNombre("Nuevo", 1L)).thenReturn(false);
        when(persistence.guardar(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.actualizar(1L, new CategoriaCommand("Nuevo"))).isEqualTo(new CategoriaDto(1L, "Nuevo"));
    }

    @Test
    void actualizar_nombreDeOtra_lanzaConflict() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(Categoria.restaurar(1L, "Viejo")));
        when(persistence.existePorNombre("Otra", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new CategoriaCommand("Otra")))
                .isInstanceOf(ConflictException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void eliminar_sinProductos_elimina() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(Categoria.restaurar(1L, "A")));
        when(persistence.tieneProductos(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(persistence).eliminar(1L);
    }

    @Test
    void eliminar_conProductos_lanzaDomainRNCAT02() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(Categoria.restaurar(1L, "A")));
        when(persistence.tieneProductos(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-CAT-02");

        verify(persistence, never()).eliminar(any());
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(1L)).isInstanceOf(CategoriaNotFoundException.class);

        verify(persistence, never()).eliminar(any());
    }
}
