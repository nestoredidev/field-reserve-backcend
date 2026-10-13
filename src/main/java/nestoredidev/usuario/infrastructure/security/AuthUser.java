package nestoredidev.usuario.infrastructure.security;

import nestoredidev.common.domain.Rol;

public record AuthUser(Long id, String username, Rol rol) {
}
