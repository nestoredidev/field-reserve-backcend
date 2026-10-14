package nestoredidev.bodega.proveedor.application.service;

import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorCommand;
import nestoredidev.bodega.proveedor.application.ports.input.dto.ProveedorDto;
import nestoredidev.bodega.proveedor.application.ports.output.ProveedorPersistencePort;
import nestoredidev.bodega.proveedor.domain.exception.InvalidProveedorException;
import nestoredidev.bodega.proveedor.domain.exception.ProveedorNotFoundException;
import nestoredidev.bodega.proveedor.domain.model.Proveedor;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    private static final PageQuery PAGE = new PageQuery(0, 10, "nombre", "asc");

    @Mock
    private ProveedorPersistencePort persistence;

    @InjectMocks
    private ProveedorService service;

    private static Proveedor proveedor(Long id, String ruc, boolean activo) {
        return Proveedor.restaurar(id, "Distribuidora " + id, ruc, "999888777", activo, OffsetDateTime.now());
    }

    @Test
    void crear_conRuc_validaUnicidadYGuardaActivo() {
        when(persistence.existePorRuc("20123456789", null)).thenReturn(false);
        when(persistence.guardar(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorDto dto = service.crear(new ProveedorCommand(" Backus ", " 20123456789 ", " 987 "));

        assertThat(dto.nombre()).isEqualTo("Backus");
        assertThat(dto.ruc()).isEqualTo("20123456789");
        assertThat(dto.telefono()).isEqualTo("987");
        assertThat(dto.activo()).isTrue();
    }

    @Test
    void crear_sinRuc_noValidaUnicidad() {
        when(persistence.guardar(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorDto dto = service.crear(new ProveedorCommand("Informal", " ", null));

        assertThat(dto.ruc()).isNull();
        verify(persistence, never()).existePorRuc(anyString(), any());
    }

    @Test
    void crear_rucDuplicado_lanzaConflictRNPRV01() {
        when(persistence.existePorRuc("20123456789", null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new ProveedorCommand("Backus", "20123456789", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-PRV-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_rucInvalido_lanzaInvalidProveedor() {
        assertThatThrownBy(() -> service.crear(new ProveedorCommand("Backus", "123", null)))
                .isInstanceOf(InvalidProveedorException.class);
    }

    @Test
    void crear_nombreVacio_lanzaInvalidProveedor() {
        assertThatThrownBy(() -> service.crear(new ProveedorCommand("", null, null)))
                .isInstanceOf(InvalidProveedorException.class);
    }

    @Test
    void crear_telefonoMayorA15_lanzaInvalidProveedor() {
        assertThatThrownBy(() -> service.crear(new ProveedorCommand("Backus", null, "1".repeat(16))))
                .isInstanceOf(InvalidProveedorException.class);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(ProveedorNotFoundException.class);
    }

    @Test
    void listar_qEnBlanco_seEnviaNulo() {
        when(persistence.listar(null, true, PAGE)).thenReturn(new PageResult<>(List.of(), 0, 10, 0, 0));

        assertThat(service.listar("  ", true, PAGE).isEmpty()).isTrue();

        verify(persistence).listar(null, true, PAGE);
    }

    @Test
    void listar_conFiltros_recortaQYMapea() {
        when(persistence.listar("back", null, PAGE))
                .thenReturn(new PageResult<>(List.of(proveedor(1L, null, true)), 0, 10, 1, 1));

        assertThat(service.listar(" back ", null, PAGE).content()).extracting(ProveedorDto::id).containsExactly(1L);
    }

    @Test
    void actualizar_valida_excluyeElPropioIdEnLaUnicidadDelRuc() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(proveedor(1L, "20123456789", true)));
        when(persistence.existePorRuc("20999999999", 1L)).thenReturn(false);
        when(persistence.guardar(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProveedorDto dto = service.actualizar(1L, new ProveedorCommand("Nuevo", "20999999999", null));

        assertThat(dto.ruc()).isEqualTo("20999999999");
        assertThat(dto.activo()).isTrue();
    }

    @Test
    void actualizar_rucDeOtro_lanzaConflict() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(proveedor(1L, null, true)));
        when(persistence.existePorRuc("20999999999", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new ProveedorCommand("N", "20999999999", null)))
                .isInstanceOf(ConflictException.class);

        verify(persistence, never()).guardar(any());
    }

    @Test
    void activar_y_desactivar_cambianElEstado() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(proveedor(1L, null, false)));
        when(persistence.guardar(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.activar(1L).activo()).isTrue();

        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(proveedor(2L, null, true)));
        assertThat(service.desactivar(2L).activo()).isFalse();
    }

    @Test
    void desactivar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(1L)).isInstanceOf(ProveedorNotFoundException.class);
    }
}
