package nestoredidev.usuario.infrastructure.security;

import nestoredidev.common.application.CurrentUser;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.exception.AuthenticationFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserAdapter implements CurrentUserPort {
    @Override
    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser u)) {
            throw new AuthenticationFailedException("No hay un usuario autenticado");
        }
        return new CurrentUser(u.id(), u.username(), u.rol());
    }
}
