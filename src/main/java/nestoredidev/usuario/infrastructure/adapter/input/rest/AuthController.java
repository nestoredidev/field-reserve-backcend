package nestoredidev.usuario.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nestoredidev.common.application.CurrentUserPort;
import nestoredidev.common.infrastructure.web.ApiResponses;
import nestoredidev.common.utils.pagination.PageResponse;
import nestoredidev.usuario.application.ports.input.UsuarioServicePort;
import nestoredidev.usuario.application.ports.input.dto.LoginCommand;
import nestoredidev.usuario.application.ports.input.dto.LoginResponse;
import nestoredidev.usuario.application.ports.input.dto.UsuarioDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioServicePort usuarioService;
    private final CurrentUserPort currentUser;

    @PostMapping("/login")
    public ResponseEntity<PageResponse<LoginResponse>> login(@Valid @RequestBody LoginCommand command) {
        return ApiResponses.ok(usuarioService.login(command), "Login exitoso");
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<UsuarioDto>> me() {
        return ApiResponses.ok(usuarioService.obtener(currentUser.get().id()), "Usuario autenticado");
    }
}
