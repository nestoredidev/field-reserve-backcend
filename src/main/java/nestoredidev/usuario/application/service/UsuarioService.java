package nestoredidev.usuario.application.service;

import lombok.RequiredArgsConstructor;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.common.domain.Rol;
import nestoredidev.common.exception.AuthenticationFailedException;
import nestoredidev.common.exception.ConflictException;
import nestoredidev.common.exception.DomainException;
import nestoredidev.usuario.application.ports.input.UsuarioServicePort;
import nestoredidev.usuario.application.ports.input.dto.*;
import nestoredidev.usuario.application.ports.output.PasswordHasherPort;
import nestoredidev.usuario.application.ports.output.TokenIssuerPort;
import nestoredidev.usuario.application.ports.output.UsuarioPersistencePort;
import nestoredidev.usuario.domain.exception.UsuarioNotFoundException;
import nestoredidev.usuario.domain.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UsuarioServicePort {
    private final UsuarioPersistencePort persistence;
    private final PasswordHasherPort hasher;
    private final TokenIssuerPort tokenIssuer;
    private final CurrentUserPort currentUser;

    @Override
    @Transactional
    public UsuarioDto crear(CreateUsuarioCommand command) {
        Usuario.validarPassword(command.password());
        String username = command.username() == null ? null : command.username().trim();
        if (username != null && persistence.existePorUsername(username)) {
            throw new ConflictException("RN-USU-01: el username '" + username + "' ya existe");
        }
        Usuario nuevo = Usuario.crear(command.nombre(), username, hasher.hash(command.password()), command.rol());
        return UsuarioDto.fromDomain(persistence.guardar(nuevo));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDto obtener(Long id) {
        return UsuarioDto.fromDomain(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UsuarioDto> listar(PageQuery query) {
        return persistence.listar(query).map(UsuarioDto::fromDomain);
    }

    @Override
    @Transactional
    public UsuarioDto actualizar(Long id, UpdateUsuarioCommand command) {
        Usuario actual = buscar(id);
        boolean pierdeAdmin = actual.getRol() == Rol.ADMIN && command.rol() != Rol.ADMIN;
        if (pierdeAdmin) {
            if (currentUser.get().id().equals(id)) {
                throw new DomainException("RN-USU-04: no puedes quitarte el rol ADMIN a ti mismo");
            }
            exigirOtroAdminActivo(actual);
        }
        return UsuarioDto.fromDomain(persistence.guardar(actual.actualizar(command.nombre(), command.rol())));
    }

    @Override
    @Transactional
    public void cambiarPassword(Long id, ChangePasswordCommand command) {
        Usuario.validarPassword(command.password());
        Usuario actual = buscar(id);
        persistence.guardar(actual.conPassword(hasher.hash(command.password())));
    }

    @Override
    @Transactional
    public UsuarioDto activar(Long id) {
        return UsuarioDto.fromDomain(persistence.guardar(buscar(id).conActivo(true)));
    }

    @Override
    @Transactional
    public UsuarioDto desactivar(Long id) {
        Usuario actual = buscar(id);
        if (currentUser.get().id().equals(id)) {
            throw new DomainException("RN-USU-04: no puedes desactivarte a ti mismo");
        }
        if (actual.getRol() == Rol.ADMIN && actual.isActivo()) {
            exigirOtroAdminActivo(actual);
        }
        return UsuarioDto.fromDomain(persistence.guardar(actual.conActivo(false)));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginCommand command) {
        Usuario usuario = persistence.buscarPorUsername(command.username().trim().toLowerCase())
                .filter(u -> hasher.matches(command.password(), u.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException("Usuario o contraseña incorrectos"));
        if (!usuario.isActivo()) {
            throw new AuthenticationFailedException("RN-USU-03: el usuario está inactivo");
        }
        return new LoginResponse(tokenIssuer.issue(usuario), "Bearer", tokenIssuer.expiresInSeconds(),
                UsuarioDto.fromDomain(usuario));
    }

    private Usuario buscar(Long id) {
        return persistence.buscarPorId(id).orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    private void exigirOtroAdminActivo(Usuario actual) {
        long admins = persistence.contarAdminsActivos();
        long restantes = actual.isActivo() ? admins - 1 : admins;
        if (restantes < 1) {
            throw new DomainException("RN-USU-04: debe existir al menos un ADMIN activo");
        }
    }
}
