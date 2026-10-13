package nestoredidev.usuario.application.ports.input;

import nestoredidev.common.application.PageQuery;
import nestoredidev.common.application.PageResult;
import nestoredidev.usuario.application.ports.input.dto.*;

public interface UsuarioServicePort {

    UsuarioDto crear(CreateUsuarioCommand command);

    UsuarioDto obtener(Long id);

    PageResult<UsuarioDto> listar(PageQuery query);

    UsuarioDto actualizar(Long id, UpdateUsuarioCommand command);

    void cambiarPassword(Long id, ChangePasswordCommand command);

    UsuarioDto activar(Long id);

    UsuarioDto desactivar(Long id);

    LoginResponse login(LoginCommand command);
}
