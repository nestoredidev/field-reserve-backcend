package nestoredidev.common.application;

import nestoredidev.common.domain.Rol;

public record CurrentUser(Long id, String username, Rol rol) {
    public boolean isAdmin() {
        return rol == Rol.ADMIN;
    }
}
