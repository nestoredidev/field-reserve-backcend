package nestoredidev.cliente.application.service;

import nestoredidev.cliente.application.ports.input.dto.ClienteCommand;
import nestoredidev.cliente.application.ports.input.dto.ClienteDto;
import nestoredidev.cliente.application.ports.output.ClientePersistencePort;
import nestoredidev.cliente.domain.exception.ClienteNotFoundException;
import nestoredidev.cliente.domain.exception.InvalidClienteException;
import nestoredidev.cliente.domain.model.Cliente;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final PageQuery PAGE = new PageQuery(0, 20, "nombre", "asc");

    @Mock
    private ClientePersistencePort persistence;

    @InjectMocks
    private ClienteService service;

    private static Cliente cliente(Long id, String nombre, String telefono) {
        return Cliente.restaurar(id, nombre, telefono, null, OffsetDateTime.parse("2026-10-01T10:00:00-05:00"));
    }

    // ---------- crear ----------

    @Test
    void crear_datosValidos_guardaClienteNormalizadoYDevuelveDto() {
        when(persistence.existePorTelefono("987654321", null)).thenReturn(false);
        when(persistence.guardar(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return Cliente.restaurar(10L, c.getNombre(), c.getTelefono(), c.getNotas(), OffsetDateTime.now());
        });

        ClienteDto dto = service.crear(new ClienteCommand("  Juan Pérez  ", " 987654321 ", "  vip "));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Juan Pérez");
        assertThat(captor.getValue().getTelefono()).isEqualTo("987654321");
        assertThat(captor.getValue().getNotas()).isEqualTo("vip");
        assertThat(dto.id()).isEqualTo(10L);
    }

    @Test
    void crear_telefonoDuplicado_lanzaConflictRNCLI02() {
        when(persistence.existePorTelefono("987654321", null)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new ClienteCommand("Juan", "987654321", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-CLI-02");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_telefonoInvalido_lanzaInvalidClienteRNCLI01() {
        assertThatThrownBy(() -> service.crear(new ClienteCommand("Juan", "123456789", null)))
                .isInstanceOf(InvalidClienteException.class)
                .hasMessageContaining("RN-CLI-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_nombreEnBlanco_lanzaInvalidCliente() {
        assertThatThrownBy(() -> service.crear(new ClienteCommand("   ", "987654321", null)))
                .isInstanceOf(InvalidClienteException.class);
    }

    // ---------- obtener ----------

    @Test
    void obtener_existente_devuelveDto() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "Ana", "911111111")));

        assertThat(service.obtener(1L).nombre()).isEqualTo("Ana");
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(ClienteNotFoundException.class);
    }

    @Test
    void obtenerPorTelefono_recortaEspaciosAntesDeBuscar() {
        when(persistence.buscarPorTelefono("911111111")).thenReturn(Optional.of(cliente(1L, "Ana", "911111111")));

        assertThat(service.obtenerPorTelefono("  911111111 ").id()).isEqualTo(1L);
    }

    @Test
    void obtenerPorTelefono_inexistente_lanzaNotFound() {
        when(persistence.buscarPorTelefono("911111111")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorTelefono("911111111"))
                .isInstanceOf(ClienteNotFoundException.class);
    }

    @Test
    void obtenerPorTelefono_nulo_buscaConCadenaVacia() {
        when(persistence.buscarPorTelefono("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorTelefono(null)).isInstanceOf(ClienteNotFoundException.class);
    }

    // ---------- obtenerPorIds ----------

    @Test
    void obtenerPorIds_coleccionVacia_noConsultaPersistencia() {
        assertThat(service.obtenerPorIds(List.of())).isEmpty();

        verify(persistence, never()).buscarPorIds(any());
    }

    @Test
    void obtenerPorIds_conIds_mapeaADtos() {
        when(persistence.buscarPorIds(List.of(1L, 2L)))
                .thenReturn(List.of(cliente(1L, "Ana", "911111111"), cliente(2L, "Luis", "922222222")));

        assertThat(service.obtenerPorIds(List.of(1L, 2L))).extracting(ClienteDto::nombre).containsExactly("Ana", "Luis");
    }

    // ---------- listar ----------

    @Test
    void listar_qEnBlanco_seEnviaNulo() {
        PageResult<Cliente> vacia = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(persistence.listar(null, PAGE)).thenReturn(vacia);

        assertThat(service.listar("   ", PAGE).isEmpty()).isTrue();

        verify(persistence).listar(null, PAGE);
    }

    @Test
    void listar_conTexto_recortaYMapeaResultados() {
        when(persistence.listar("ana", PAGE))
                .thenReturn(new PageResult<>(List.of(cliente(1L, "Ana", "911111111")), 0, 20, 1, 1));

        PageResult<ClienteDto> resultado = service.listar("  ana ", PAGE);

        assertThat(resultado.content()).extracting(ClienteDto::nombre).containsExactly("Ana");
        assertThat(resultado.totalElements()).isEqualTo(1);
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_valido_validaTelefonoExcluyendoElPropioIdYGuarda() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "Ana", "911111111")));
        when(persistence.existePorTelefono("933333333", 1L)).thenReturn(false);
        when(persistence.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteDto dto = service.actualizar(1L, new ClienteCommand("Ana María", "933333333", "nota"));

        assertThat(dto.nombre()).isEqualTo("Ana María");
        assertThat(dto.telefono()).isEqualTo("933333333");
        assertThat(dto.id()).isEqualTo(1L);
    }

    @Test
    void actualizar_telefonoDeOtroCliente_lanzaConflict() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(cliente(1L, "Ana", "911111111")));
        when(persistence.existePorTelefono("933333333", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, new ClienteCommand("Ana", "933333333", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-CLI-02");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(5L, new ClienteCommand("Ana", "911111111", null)))
                .isInstanceOf(ClienteNotFoundException.class);
    }

    // ---------- buscarOCrear ----------

    @Test
    void buscarOCrear_existente_devuelveElMismoSinGuardar() {
        when(persistence.buscarPorTelefono("911111111")).thenReturn(Optional.of(cliente(1L, "Ana", "911111111")));

        ClienteDto dto = service.buscarOCrear("Otro nombre", " 911111111 ");

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nombre()).isEqualTo("Ana");
        verify(persistence, never()).guardar(any());
    }

    @Test
    void buscarOCrear_inexistente_creaClienteNuevo() {
        when(persistence.buscarPorTelefono("911111111")).thenReturn(Optional.empty());
        when(persistence.guardar(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return Cliente.restaurar(7L, c.getNombre(), c.getTelefono(), c.getNotas(), OffsetDateTime.now());
        });

        ClienteDto dto = service.buscarOCrear("Nuevo", "911111111");

        assertThat(dto.id()).isEqualTo(7L);
        assertThat(dto.nombre()).isEqualTo("Nuevo");
    }

    @Test
    void buscarOCrear_telefonoInvalido_lanzaInvalidCliente() {
        assertThatThrownBy(() -> service.buscarOCrear("Nuevo", "abc"))
                .isInstanceOf(InvalidClienteException.class);

        verify(persistence, never()).buscarPorTelefono(any());
    }
}
