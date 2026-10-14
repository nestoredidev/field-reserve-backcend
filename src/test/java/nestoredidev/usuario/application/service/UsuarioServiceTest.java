package nestoredidev.usuario.application.service;

import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.AuthenticationFailedException;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.usuario.application.ports.input.dto.ChangePasswordCommand;
import nestoredidev.usuario.application.ports.input.dto.CreateUsuarioCommand;
import nestoredidev.usuario.application.ports.input.dto.LoginCommand;
import nestoredidev.usuario.application.ports.input.dto.LoginResponse;
import nestoredidev.usuario.application.ports.input.dto.UpdateUsuarioCommand;
import nestoredidev.usuario.application.ports.input.dto.UsuarioDto;
import nestoredidev.usuario.application.ports.output.PasswordHasherPort;
import nestoredidev.usuario.application.ports.output.TokenIssuerPort;
import nestoredidev.usuario.application.ports.output.UsuarioPersistencePort;
import nestoredidev.usuario.domain.exception.InvalidUsuarioException;
import nestoredidev.usuario.domain.exception.UsuarioNotFoundException;
import nestoredidev.usuario.domain.model.Usuario;
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
class UsuarioServiceTest {

    @Mock
    private UsuarioPersistencePort persistence;
    @Mock
    private PasswordHasherPort hasher;
    @Mock
    private TokenIssuerPort tokenIssuer;
    @Mock
    private CurrentUserPort currentUser;

    @InjectMocks
    private UsuarioService service;

    private static Usuario usuario(Long id, String username, Rol rol, boolean activo) {
        return Usuario.restaurar(id, "Nombre " + id, username, "hash-" + id, rol, activo, OffsetDateTime.now());
    }

    private void actuaComo(Long id) {
        when(currentUser.get()).thenReturn(new CurrentUser(id, "actual", Rol.ADMIN));
    }

    // ---------- crear ----------

    @Test
    void crear_valido_hasheaPasswordYGuardaActivo() {
        when(persistence.existePorUsername("juan.perez")).thenReturn(false);
        when(hasher.hash("password123")).thenReturn("HASH");
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return Usuario.restaurar(5L, u.getNombre(), u.getUsername(), u.getPasswordHash(), u.getRol(),
                    u.isActivo(), OffsetDateTime.now());
        });

        UsuarioDto dto = service.crear(new CreateUsuarioCommand(" Juan Pérez ", " juan.perez ", "password123", Rol.ENCARGADO));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(captor.getValue().getNombre()).isEqualTo("Juan Pérez");
        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.username()).isEqualTo("juan.perez");
        assertThat(dto.activo()).isTrue();
        assertThat(dto.rol()).isEqualTo(Rol.ENCARGADO);
    }

    @Test
    void crear_usernameDuplicado_lanzaConflictRNUSU01() {
        when(persistence.existePorUsername("juan.perez")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CreateUsuarioCommand("Juan", "juan.perez", "password123", Rol.ADMIN)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RN-USU-01");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void crear_passwordCorta_lanzaInvalidUsuarioRNUSU02SinConsultarNada() {
        assertThatThrownBy(() -> service.crear(new CreateUsuarioCommand("Juan", "juan.perez", "corta", Rol.ADMIN)))
                .isInstanceOf(InvalidUsuarioException.class)
                .hasMessageContaining("RN-USU-02");

        verify(persistence, never()).existePorUsername(any());
    }

    @Test
    void crear_usernameConMayusculas_lanzaInvalidUsuario() {
        when(persistence.existePorUsername("JuanPerez")).thenReturn(false);
        when(hasher.hash("password123")).thenReturn("HASH");

        assertThatThrownBy(() -> service.crear(new CreateUsuarioCommand("Juan", "JuanPerez", "password123", Rol.ADMIN)))
                .isInstanceOf(InvalidUsuarioException.class)
                .hasMessageContaining("RN-USU-01");

        verify(persistence, never()).guardar(any());
    }

    // ---------- obtener / listar ----------

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(1L)).isInstanceOf(UsuarioNotFoundException.class);
    }

    @Test
    void listar_mapeaADtosSinHash() {
        PageQuery q = new PageQuery(0, 10, "nombre", "asc");
        when(persistence.listar(q)).thenReturn(
                new PageResult<>(List.of(usuario(1L, "admin", Rol.ADMIN, true)), 0, 10, 1, 1));

        PageResult<UsuarioDto> res = service.listar(q);

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).username()).isEqualTo("admin");
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_encargadoACambiarNombre_noValidaAdmins() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "enc", Rol.ENCARGADO, true)));
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDto dto = service.actualizar(2L, new UpdateUsuarioCommand("Nuevo", Rol.ENCARGADO));

        assertThat(dto.nombre()).isEqualTo("Nuevo");
        verify(persistence, never()).contarAdminsActivos();
        verify(currentUser, never()).get();
    }

    @Test
    void actualizar_adminQueSeMantieneAdmin_noValidaAdmins() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.actualizar(1L, new UpdateUsuarioCommand("Otro", Rol.ADMIN)).rol()).isEqualTo(Rol.ADMIN);

        verify(persistence, never()).contarAdminsActivos();
    }

    @Test
    void actualizar_quitarseAdminASiMismo_lanzaDomainRNUSU04() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));
        actuaComo(1L);

        assertThatThrownBy(() -> service.actualizar(1L, new UpdateUsuarioCommand("Admin", Rol.ENCARGADO)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("RN-USU-04");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_quitarAdminAOtroSiendoElUltimo_lanzaDomainRNUSU04() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "admin2", Rol.ADMIN, true)));
        actuaComo(1L);
        when(persistence.contarAdminsActivos()).thenReturn(1L);

        assertThatThrownBy(() -> service.actualizar(2L, new UpdateUsuarioCommand("Admin", Rol.ENCARGADO)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("al menos un ADMIN activo");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void actualizar_quitarAdminAOtroHabiendoMasAdmins_guarda() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "admin2", Rol.ADMIN, true)));
        actuaComo(1L);
        when(persistence.contarAdminsActivos()).thenReturn(2L);
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.actualizar(2L, new UpdateUsuarioCommand("Admin2", Rol.ENCARGADO)).rol())
                .isEqualTo(Rol.ENCARGADO);
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        when(persistence.buscarPorId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(9L, new UpdateUsuarioCommand("X", Rol.ADMIN)))
                .isInstanceOf(UsuarioNotFoundException.class);
    }

    // ---------- cambiarPassword ----------

    @Test
    void cambiarPassword_valida_guardaNuevoHash() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "enc", Rol.ENCARGADO, true)));
        when(hasher.hash("nuevaClave1")).thenReturn("NUEVO-HASH");

        service.cambiarPassword(2L, new ChangePasswordCommand("nuevaClave1"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(persistence).guardar(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("NUEVO-HASH");
        assertThat(captor.getValue().getId()).isEqualTo(2L);
    }

    @Test
    void cambiarPassword_corta_lanzaInvalidUsuarioSinBuscar() {
        assertThatThrownBy(() -> service.cambiarPassword(2L, new ChangePasswordCommand("123")))
                .isInstanceOf(InvalidUsuarioException.class);

        verify(persistence, never()).buscarPorId(any());
    }

    @Test
    void cambiarPassword_usuarioInexistente_lanzaNotFound() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarPassword(2L, new ChangePasswordCommand("nuevaClave1")))
                .isInstanceOf(UsuarioNotFoundException.class);
    }

    // ---------- activar / desactivar ----------

    @Test
    void activar_marcaActivo() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "enc", Rol.ENCARGADO, false)));
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.activar(2L).activo()).isTrue();
    }

    @Test
    void desactivar_aSiMismo_lanzaDomainRNUSU04() {
        when(persistence.buscarPorId(1L)).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));
        actuaComo(1L);

        assertThatThrownBy(() -> service.desactivar(1L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("desactivarte a ti mismo");
    }

    @Test
    void desactivar_ultimoAdminActivo_lanzaDomain() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "admin2", Rol.ADMIN, true)));
        actuaComo(1L);
        when(persistence.contarAdminsActivos()).thenReturn(1L);

        assertThatThrownBy(() -> service.desactivar(2L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("al menos un ADMIN activo");

        verify(persistence, never()).guardar(any());
    }

    @Test
    void desactivar_adminActivoHabiendoOtros_desactiva() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "admin2", Rol.ADMIN, true)));
        actuaComo(1L);
        when(persistence.contarAdminsActivos()).thenReturn(2L);
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(2L).activo()).isFalse();
    }

    @Test
    void desactivar_encargado_noConsultaCantidadDeAdmins() {
        when(persistence.buscarPorId(3L)).thenReturn(Optional.of(usuario(3L, "enc", Rol.ENCARGADO, true)));
        actuaComo(1L);
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(3L).activo()).isFalse();

        verify(persistence, never()).contarAdminsActivos();
    }

    @Test
    void desactivar_adminYaInactivo_noExigeOtroAdmin() {
        when(persistence.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "admin2", Rol.ADMIN, false)));
        actuaComo(1L);
        when(persistence.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.desactivar(2L).activo()).isFalse();

        verify(persistence, never()).contarAdminsActivos();
    }

    // ---------- login ----------

    @Test
    void login_correcto_normalizaUsernameYEmiteToken() {
        Usuario u = usuario(1L, "admin", Rol.ADMIN, true);
        when(persistence.buscarPorUsername("admin")).thenReturn(Optional.of(u));
        when(hasher.matches("Admin12345", "hash-1")).thenReturn(true);
        when(tokenIssuer.issue(u)).thenReturn("jwt-token");
        when(tokenIssuer.expiresInSeconds()).thenReturn(28800L);

        LoginResponse res = service.login(new LoginCommand("  ADMIN ", "Admin12345"));

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresInSeconds()).isEqualTo(28800L);
        assertThat(res.usuario().username()).isEqualTo("admin");
    }

    @Test
    void login_usuarioInexistente_lanzaAuthenticationFailed() {
        when(persistence.buscarPorUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("nadie", "x")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("incorrectos");

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void login_passwordIncorrecta_lanzaMismoMensajeQueUsuarioInexistente() {
        when(persistence.buscarPorUsername("admin")).thenReturn(Optional.of(usuario(1L, "admin", Rol.ADMIN, true)));
        when(hasher.matches("mala", "hash-1")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("admin", "mala")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void login_usuarioInactivoConPasswordCorrecta_lanzaRNUSU03() {
        when(persistence.buscarPorUsername("enc")).thenReturn(Optional.of(usuario(3L, "enc", Rol.ENCARGADO, false)));
        when(hasher.matches("clave", "hash-3")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand("enc", "clave")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("RN-USU-03");

        verify(tokenIssuer, never()).issue(any());
    }
}
