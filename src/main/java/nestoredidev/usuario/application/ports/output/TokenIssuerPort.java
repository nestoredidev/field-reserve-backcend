package nestoredidev.usuario.application.ports.output;

import nestoredidev.usuario.domain.model.Usuario;

public interface TokenIssuerPort {
    String issue(Usuario usuario);

    long expiresInSeconds();
}
